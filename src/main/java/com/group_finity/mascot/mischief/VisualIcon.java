package com.group_finity.mascot.mischief;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.glados.GladosEventReporter;
import com.group_finity.mascot.glados.GladosOptions;
import com.group_finity.mascot.util.DPIScaler;

/** Plays the icon snack animation, using an Explorer icon when available. */
public final class VisualIcon {
    private static long lastCheck;
    private static volatile long lastSnack;
    private static volatile JWindow bubble;
    private static volatile JWindow animation;
    private static long generation;
    private VisualIcon() {}
    public static void tick(Mascot mascot) {
        if (!MischiefMode.isEnabled() || !Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Mischief.EatIconEnabled", "true"))) return;
        final long request = currentGeneration();
        if (GladosOptions.isManagedProcess() && mascot.getManager() != null && mascot != mascot.getManager().getPrimaryMascot()) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck < 5000 || now - lastSnack < 30000) return;
        lastCheck = now;
        if (Math.random() >= 0.2) return;
        MischiefMode.runIfEnabled(() -> {
            Rectangle bounds = mascot.getBounds();
            boolean onFloor = mascot.getEnvironment().getFloor().isOn(mascot.getAnchor());
            if (onFloor) {
                try {
                    mascot.setBehavior(Main.getInstance().getConfiguration(mascot.getImageSet())
                        .buildBehavior("SitAndSpinHead", mascot));
                } catch (Exception ignored) { /* Some skins omit the optional character animation. */ }
            }
            Point anchor = mascot.getAnchor();
            Point physical = new Point(DPIScaler.logicalToPhysical(anchor.x), DPIScaler.logicalToPhysical(anchor.y));
            String mascotId = mascot.getInstanceId();
            if (!RecoverableWindowMoves.requestIcon(physical, moved -> {
                if (moved != null || onFloor) present(bounds, request, mascotId, moved);
            }) && onFloor) {
                present(bounds, request, mascotId, null);
            }
        });
    }

    private static void present(Rectangle bounds, long request, String mascotId, Point moved) {
        if (!isCurrent(request)) return;
        lastSnack = System.currentTimeMillis();
        Point origin = moved == null ? null :
            new Point(DPIScaler.physicalToLogical(moved.x), DPIScaler.physicalToLogical(moved.y));
        SwingUtilities.invokeLater(() -> {
            if (!isCurrent(request)) return;
            showSnack(bounds, request, origin);
            if (GladosOptions.isManagedProcess()) {
                GladosEventReporter.report("mischief", "{\"kind\":\"eatIcon\",\"id\":\"" + mascotId + "\"}");
            } else {
                show(bounds, request, origin != null);
            }
        });
    }

    private static void show(Rectangle bounds, long request, boolean moved) {
        if (!isCurrent(request)) return;
        JWindow window = new JWindow();
        window.add(new JLabel(moved ? "  我吃掉了一个图标～  " : "  我假装吃掉了一个图标～  "));
        window.pack();
        window.setAlwaysOnTop(true);
        window.setLocation(bounds.x, Math.max(0, bounds.y - window.getHeight()));
        bubble = window;
        window.setVisible(true);
        Timer timer = new Timer(3000, e -> { window.dispose(); if (bubble == window) bubble = null; });
        timer.setRepeats(false); timer.start();
    }

    static void showSnack(Rectangle bounds, long request) {
        showSnack(bounds, request, null);
    }

    private static void showSnack(Rectangle bounds, long request, Point origin) {
        if (!isCurrent(request)) return;
        JWindow window = new JWindow();
        window.setBackground(new Color(0, 0, 0, 0));
        final int[] frame = {0};
        JPanel drawing = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.max(1, 34 - frame[0] * 2);
                int x = 8 + frame[0] * 2;
                int y = 24 + frame[0];
                g.setColor(new Color(91, 160, 228, 235));
                g.fillRoundRect(x, y, size, size, 7, 7);
                g.setColor(Color.WHITE);
                g.drawRoundRect(x, y, size, size, 7, 7);
                g.dispose();
            }
        };
        drawing.setOpaque(false);
        drawing.setPreferredSize(new Dimension(64, 64));
        window.setContentPane(drawing);
        window.pack();
        window.setAlwaysOnTop(true);
        window.setFocusableWindowState(false);
        Point destination = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        Point start = origin == null ? destination : origin;
        window.setLocation(start.x - 32, start.y - 32);
        animation = window;
        window.setVisible(true);
        Timer timer = new Timer(80, null);
        timer.addActionListener(e -> {
            if (!isCurrent(request) || ++frame[0] >= 17) {
                timer.stop();
                window.dispose();
                if (animation == window) animation = null;
            } else {
                window.setLocation(start.x + (destination.x - start.x) * frame[0] / 17 - 32,
                    start.y + (destination.y - start.y) * frame[0] / 17 - 32);
                drawing.repaint();
            }
        });
        timer.start();
    }

    static synchronized long currentGeneration() { return generation; }
    private static synchronized boolean isCurrent(long request) {
        return MischiefMode.isEnabled() && request == generation;
    }
    public static synchronized void close() {
        generation++;
        SwingUtilities.invokeLater(() -> {
            JWindow current = bubble;
            bubble = null;
            if (current != null) current.dispose();
            current = animation;
            animation = null;
            if (current != null) current.dispose();
        });
    }
}
