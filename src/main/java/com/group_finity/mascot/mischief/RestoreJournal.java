package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.awt.Point;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable write-ahead records for future recoverable desktop operations. */
public final class RestoreJournal {
    public interface Restorer { boolean restore(JSONObject record) throws Exception; }
    private final Path file;

    public RestoreJournal(Path file) { this.file = file.toAbsolutePath(); }

    /** Persist before moving anything; a failure must prevent the operation. */
    public synchronized String record(String kind, JSONObject identity, int x, int y) throws IOException {
        if (kind == null || kind.trim().isEmpty() || identity == null || identity.isEmpty())
            throw new IllegalArgumentException("kind and identity are required");
        List<JSONObject> entries = read();
        JSONObject entry = new JSONObject(true);
        String id = UUID.randomUUID().toString();
        entry.put("id", id);
        entry.put("kind", kind);
        entry.put("identity", JSON.parseObject(identity.toJSONString()));
        entry.put("x", x);
        entry.put("y", y);
        entries.add(entry);
        write(entries);
        return id;
    }

    /** Remove only successfully restored records; exceptions and failures stay pending. */
    public synchronized int restorePending(Restorer restorer) throws IOException {
        List<JSONObject> entries = read();
        int restored = 0;
        for (JSONObject entry : new ArrayList<>(entries)) {
            boolean success;
            try { success = restorer.restore(JSON.parseObject(entry.toJSONString())); }
            catch (Exception e) { success = false; }
            if (success) {
                entries.remove(entry);
                write(entries);
                restored++;
            }
        }
        return restored;
    }

    public synchronized List<JSONObject> pending() throws IOException { return read(); }

    synchronized void discard(String id) throws IOException {
        List<JSONObject> entries = read();
        if (entries.removeIf(entry -> id.equals(entry.getString("id")))) write(entries);
    }

    synchronized void updateDestination(String id, Point destination) throws IOException {
        List<JSONObject> entries = read();
        for (JSONObject entry : entries) {
            if (!id.equals(entry.getString("id"))) continue;
            JSONObject identity = entry.getJSONObject("identity");
            identity.put("movedX", destination.x);
            identity.put("movedY", destination.y);
            write(entries);
            return;
        }
    }

    private List<JSONObject> read() throws IOException {
        List<JSONObject> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        try {
            JSONObject root = JSON.parseObject(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            if (root == null || root.getIntValue("version") != 1 || !(root.get("entries") instanceof JSONArray))
                throw new IOException("Invalid restore journal format");
            JSONArray entries = root.getJSONArray("entries");
            for (Object value : entries) {
                if (!(value instanceof JSONObject)) throw new IOException("Invalid restore journal entry");
                JSONObject entry = (JSONObject) value;
                if (entry.getString("id") == null || entry.getString("kind") == null ||
                    !(entry.get("identity") instanceof JSONObject) || !entry.containsKey("x") || !entry.containsKey("y"))
                    throw new IOException("Invalid restore journal entry");
                result.add(entry);
            }
            return result;
        } catch (RuntimeException e) {
            throw new IOException("Invalid restore journal", e);
        }
    }

    private void write(List<JSONObject> entries) throws IOException {
        Path directory = file.getParent();
        Files.createDirectories(directory);
        JSONObject root = new JSONObject(true);
        root.put("version", 1);
        root.put("entries", entries);
        byte[] data = root.toJSONString().getBytes(StandardCharsets.UTF_8);
        Path temporary = Files.createTempFile(directory, ".mischief-restore-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(data);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("Atomic journal replacement is not supported", e);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
