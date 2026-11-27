package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import java.awt.Point;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serializes recoverable desktop moves and keeps a per-home exclusive process lock. */
public final class RecoverableWindowMoves {
    interface Backend {
        JSONObject capture(long handle) throws Exception;
        boolean move(JSONObject identity, int x, int y) throws Exception;
        boolean restore(JSONObject record) throws Exception;
    }
    private static final Logger LOG = LoggerFactory.getLogger(RecoverableWindowMoves.class);
    private static final long THROW_INTERVAL_MS = 30_000;
    private static RecoverableWindowMoves instance;
    private final RestoreJournal journal;
    private final Backend backend;
    private final DesktopIconSession icons;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mischief-desktop");
        thread.setDaemon(true);
        return thread;
    });
    private boolean enabled;
    private long generation;
    private long lastThrow;
    private boolean throwQueued;
    private boolean iconQueued;

    private RecoverableWindowMoves(Path path, Backend backend, DesktopIconSession icons, FileChannel channel, FileLock lock) {
        this.journal = new RestoreJournal(path);
        this.backend = backend;
        this.icons = icons;
        this.lockChannel = channel;
        this.lock = lock;
    }

    static RecoverableWindowMoves open(Path path, Backend backend) throws IOException {
        return open(path, backend, null);
    }

    static RecoverableWindowMoves open(Path path, Backend backend, DesktopIconSession icons) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        FileChannel channel = FileChannel.open(path.resolveSibling(path.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new IOException("restore journal is in use");
            RecoverableWindowMoves service = new RecoverableWindowMoves(path, backend, icons, channel, lock);
            try { service.restore(); return service; }
            catch (IOException e) { service.close(); throw e; }
        } catch (RuntimeException | IOException e) { channel.close(); throw e; }
    }

    public static synchronized void initialize(Path path) {
        if (instance != null || !com.sun.jna.Platform.isWindows()) return;
        Path desktopRecovery = Paths.get(System.getProperty("user.home"), ".shimeji", "mischief-desktop-restore.json");
        try { instance = open(path, new WindowsWindowMoveBackend(), new DesktopIconSession(desktopRecovery)); }
        catch (Exception | LinkageError e) { LOG.warn("可恢复桌面移动不可用", e); }
    }
    public static synchronized boolean supported() { return instance != null; }
    public static synchronized void setEnabled(boolean enabled) {
        if (instance == null) return;
        instance.setActive(enabled);
    }
    public static synchronized void request(boolean right, long handle) {
        if (instance != null) instance.enqueue(right, handle);
    }
    static synchronized boolean requestIcon(Point anchor, Consumer<Point> completed) {
        return instance != null && instance.enqueueIcon(anchor, completed);
    }
    public static synchronized void shutdown() {
        if (instance != null) { instance.close(); instance = null; }
    }

    synchronized void setActive(boolean value) {
        enabled = value;
        if (!value) {
            generation++;
            throwQueued = false;
            iconQueued = false;
            try { restore(); } catch (IOException e) { LOG.warn("恢复桌面位置失败，记录已保留", e); }
        }
    }
    synchronized void enqueue(boolean right, long handle) {
        if (!enabled || throwQueued || System.currentTimeMillis() - lastThrow < THROW_INTERVAL_MS) return;
        throwQueued = true;
        long requestedGeneration = generation;
        worker.execute(() -> {
            synchronized (this) {
                if (requestedGeneration != generation) return;
                throwQueued = false;
                move(right, handle);
            }
        });
    }
    synchronized void move(boolean right, long handle) {
        if (!enabled || System.currentTimeMillis() - lastThrow < THROW_INTERVAL_MS) return;
        try {
            JSONObject target = backend.capture(handle);
            if (target == null) return;
            for (JSONObject record : journal.pending()) {
                if ("window".equals(record.getString("kind")) &&
                    handle == record.getJSONObject("identity").getLongValue("handle")) return;
            }
            int x = target.getIntValue("x"), y = target.getIntValue("y");
            target.put("movedX", x + (right ? 300 : -300));
            target.put("movedY", y);
            String id = journal.record("window", target, x, y);
            if (backend.move(target, target.getIntValue("movedX"), y)) {
                lastThrow = System.currentTimeMillis();
            } else {
                journal.restorePending(record -> id.equals(record.getString("id")) && backend.restore(record));
            }
        } catch (Exception | LinkageError e) { LOG.warn("窗口移动失败，恢复记录保留", e); }
    }
    private synchronized boolean enqueueIcon(Point anchor, Consumer<Point> completed) {
        if (!enabled || icons == null || iconQueued) return false;
        iconQueued = true;
        long requestedGeneration = generation;
        worker.execute(() -> moveIcon(anchor, completed, requestedGeneration));
        return true;
    }

    private synchronized void moveIcon(Point anchor, Consumer<Point> completed, long requestedGeneration) {
        if (requestedGeneration != generation) return;
        iconQueued = false;
        Point original = null;
        try {
            original = icons.move(anchor);
        } catch (Exception | LinkageError e) {
            LOG.warn("Desktop icon movement failed", e);
        }
        completed.accept(original);
    }

    synchronized void restore() throws IOException {
        try {
            journal.restorePending(record -> "window".equals(record.getString("kind")) && backend.restore(record));
        } finally {
            if (icons != null) {
                try {
                    icons.restore(journal);
                } catch (IOException | LinkageError e) {
                    LOG.warn("恢复桌面图标和排列设置失败，记录已保留", e);
                }
            }
        }
    }
    synchronized void close() {
        setActive(false);
        worker.shutdownNow();
        try { lock.release(); lockChannel.close(); } catch (IOException e) { LOG.warn("关闭恢复日志失败", e); }
    }
}
