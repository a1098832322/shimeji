package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serializes recoverable window moves and keeps a per-home exclusive process lock. */
public final class RecoverableWindowMoves {
    interface Backend {
        JSONObject capture() throws Exception;
        boolean move(JSONObject identity, int x, int y) throws Exception;
        boolean restore(JSONObject record) throws Exception;
    }
    private static final Logger LOG = LoggerFactory.getLogger(RecoverableWindowMoves.class);
    private static RecoverableWindowMoves instance;
    private final RestoreJournal journal;
    private final Backend backend;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mischief-window"); thread.setDaemon(true); return thread;
    });
    private boolean enabled;
    private long lastAttempt;

    private RecoverableWindowMoves(Path path, Backend backend, FileChannel channel, FileLock lock) {
        this.journal = new RestoreJournal(path); this.backend = backend; this.lockChannel = channel; this.lock = lock;
    }

    static RecoverableWindowMoves open(Path path, Backend backend) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        FileChannel channel = FileChannel.open(path.resolveSibling(path.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new IOException("restore journal is in use");
            RecoverableWindowMoves service = new RecoverableWindowMoves(path, backend, channel, lock);
            try { service.restore(); return service; }
            catch (IOException e) { service.close(); throw e; }
        } catch (RuntimeException | IOException e) { channel.close(); throw e; }
    }

    public static synchronized void initialize(Path path) {
        if (instance != null || !com.sun.jna.Platform.isWindows()) return;
        try { instance = open(path, new WindowsWindowMoveBackend()); }
        catch (Exception | LinkageError e) { LOG.warn("可恢复窗口移动不可用", e); }
    }
    public static synchronized boolean supported() { return instance != null; }
    public static synchronized void setEnabled(boolean enabled) {
        if (instance == null) return;
        instance.setActive(enabled);
    }
    public static synchronized void request(boolean right, int x, int y, int width, int height) {
        if (instance != null) instance.enqueue(right, x, y, width, height);
    }
    public static synchronized void shutdown() {
        if (instance != null) { instance.close(); instance = null; }
    }

    synchronized void setActive(boolean value) {
        enabled = value;
        if (!value) {
            try { restore(); } catch (IOException e) { LOG.warn("恢复窗口位置失败，记录已保留", e); }
        }
    }
    synchronized void enqueue(boolean right, int x, int y, int width, int height) {
        long now = System.currentTimeMillis();
        if (!enabled || now - lastAttempt < 30000) return;
        lastAttempt = now;
        worker.execute(() -> move(right, x, y, width, height));
    }
    synchronized void move(boolean right, int expectedX, int expectedY, int expectedWidth, int expectedHeight) {
        if (!enabled) return;
        try {
            // A pending record prevents repeated moves until it can be restored.
            if (!journal.pending().isEmpty()) return;
            JSONObject target = backend.capture();
            if (target == null) return;
            // The active window may have changed while the request was queued.
            if (Math.abs(target.getIntValue("x") - expectedX) > 8 ||
                Math.abs(target.getIntValue("y") - expectedY) > 8 ||
                Math.abs(target.getIntValue("width") - expectedWidth) > 8 ||
                Math.abs(target.getIntValue("height") - expectedHeight) > 8) return;
            int x = target.getIntValue("x"), y = target.getIntValue("y");
            target.put("movedX", x + (right ? 300 : -300));
            target.put("movedY", y);
            journal.record("window", target, x, y);
            backend.move(target, target.getIntValue("movedX"), y);
        } catch (Exception | LinkageError e) { LOG.warn("窗口移动失败，恢复记录保留", e); }
    }
    synchronized void restore() throws IOException { journal.restorePending(backend::restore); }
    synchronized void close() {
        setActive(false);
        worker.shutdownNow();
        try { lock.release(); lockChannel.close(); } catch (IOException e) { LOG.warn("关闭恢复日志失败", e); }
    }
}
