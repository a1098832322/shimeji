package com.group_finity.mascot.mischief;

import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;

/** Pointer movement is opt-in and performed off the engine tick thread. */
public final class CursorPush {
    interface Pointer {
        Point location() throws Exception;
        void move(Point position) throws Exception;
    }
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mischief-cursor"); t.setDaemon(true); return t;
    });
    private static volatile String capability = "supported";
    private static long lastAttempt;
    private CursorPush() {}
    static synchronized boolean reserve(long now, long interval) {
        if (now - lastAttempt < interval) return false;
        lastAttempt = now;
        return true;
    }
    static void resetForTest() { lastAttempt = 0; capability = "supported"; }
    public static String capability() { return capability; }
    public static void tick(Mascot mascot) {
        if (!MischiefMode.isCursorEnabled() || "unsupported".equals(capability)) return;
        try {
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            if (!nearMascot(mascot.getBounds(), pointer)) return;
            long interval = Math.max(15000, Long.parseLong(Main.getInstance().getProperties().getProperty("Mischief.PushCursorInterval", "15000")));
            if (!reserve(System.currentTimeMillis(), interval)) return;
            WORKER.execute(() -> push(() -> new Pointer() {
                final Robot robot = new Robot();
                public Point location() { return MouseInfo.getPointerInfo().getLocation(); }
                public void move(Point position) { robot.mouseMove(position.x, position.y); }
            }));
        } catch (Exception | LinkageError e) { capability = "unsupported"; }
    }
    static boolean nearMascot(Rectangle bounds, Point pointer) {
        Rectangle area = new Rectangle(bounds);
        area.grow(40, 40);
        return area.contains(pointer);
    }
    private static void push(PointerFactory factory) {
        try { push(factory.create()); } catch (Exception | LinkageError e) { capability = "unsupported"; }
    }
    interface PointerFactory { Pointer create() throws Exception; }
    static void push(Pointer pointer) {
        try {
            if (!MischiefMode.isCursorEnabled()) return;
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            push(pointer, bounds);
        } catch (Exception | LinkageError e) { capability = "unsupported"; }
    }
    static void push(Pointer pointer, Rectangle bounds) {
        try {
            if (!MischiefMode.isCursorEnabled()) return;
            Point before = pointer.location();
            int direction = before.x + 180 < bounds.x + bounds.width ? 1 : -1;
            Point target = new Point(Math.max(bounds.x, Math.min(bounds.x + bounds.width - 1, before.x + direction * 180)), before.y);
            if (target.equals(before)) { capability = "unsupported"; return; }
            if (!MischiefMode.moveCursor(() -> {
                try { pointer.move(target); }
                catch (Exception e) { throw new IllegalStateException(e); }
            })) return;
            Thread.sleep(100);
            if (pointer.location().equals(before)) capability = "unsupported";
        } catch (Exception | LinkageError e) { capability = "unsupported"; }
    }
}
