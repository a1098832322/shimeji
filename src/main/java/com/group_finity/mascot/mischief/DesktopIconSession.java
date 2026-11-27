package com.group_finity.mascot.mischief;

import java.awt.Point;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** All Shimeji instances for a Windows user share the Explorer layout and this journal. */
final class DesktopIconSession {
    private final WindowsDesktopIcons icons = new WindowsDesktopIcons();
    private final RestoreJournal journal;
    private final Path lockPath;
    private FileLock ownership;

    DesktopIconSession(Path path) {
        journal = new RestoreJournal(path);
        lockPath = path.toAbsolutePath().resolveSibling(path.getFileName() + ".lock");
    }

    Point move(Point anchor) throws IOException {
        boolean starting = ownership == null;
        if (!acquire()) return null;
        try {
            if (starting) restoreDesktop();
            Point moved = icons.move(anchor, journal);
            if (moved == null && starting) restoreDesktop();
            return moved;
        } finally {
            if (journal.pending().isEmpty()) release();
        }
    }

    void restore(RestoreJournal legacy) throws IOException {
        if (!acquire()) return;
        try {
            try {
                legacy.restorePending(record -> "icon".equals(record.getString("kind")) && icons.restore(record));
            } finally {
                restoreDesktop();
            }
        } finally {
            release();
        }
    }

    private void restoreDesktop() throws IOException {
        journal.restorePending(record -> "icon".equals(record.getString("kind")) && icons.restore(record));
        // Explorer needs manual placement while the original icon positions are restored.
        journal.restorePending(record -> WindowsDesktopIcons.AUTO_ARRANGE.equals(record.getString("kind"))
            && icons.restoreAutoArrange(record));
    }

    private boolean acquire() throws IOException {
        if (ownership != null) return true;
        Files.createDirectories(lockPath.getParent());
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            ownership = channel.tryLock();
            return ownership != null;
        } catch (OverlappingFileLockException busy) {
            return false;
        } finally {
            if (ownership == null) channel.close();
        }
    }

    private void release() throws IOException {
        if (ownership == null) return;
        FileChannel channel = ownership.channel();
        ownership = null;
        channel.close();
    }
}
