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
import java.awt.RenderingHints;
import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.glados.GladosEventReporter;
import com.group_finity.mascot.glados.GladosOptions;

/** A pretend icon snack: never changes a file or an operating system icon. */
public final class VisualIcon {
    private static long lastCheck;
    private static long lastSnack;
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
        if (Math.random() >= 0.2 || !mascot.getEnvironment().getFloor().isOn(mascot.getAnchor())) return;
        MischiefMode.runIfEnabled(() -> {
          try {
            mascot.setBehavior(Main.getInstance().getConfiguration(mascot.getImageSet()).buildBehavior("SitAndSpinHead", mascot));
            lastSnack = now;
            if (!isCurrent(request)) return;
            final java.awt.Rectangle bounds = mascot.getBounds();
            SwingUtilities.invokeLater(() -> showSnack(bounds, request));
            if (GladosOptions.isManagedProcess()) {
                GladosEventReporter.report("mischief", "{\"kind\":\"eatIcon\",\"id\":\"" + mascot.getInstanceId() + "\"}");
            } else {
                SwingUtilities.invokeLater(() -> show(bounds, request));
            }
          } catch (Exception ignored) { /* The skin may not provide this optional animation. */ }
        });
    }
    private static void show(java.awt.Rectangle bounds, long request) {
        if (!isCurrent(request)) return;
        JWindow window = new JWindow();
        window.add(new JLabel("  我假装吃掉了一个图标～  "));
        window.pack();
        window.setAlwaysOnTop(true);
        window.setLocation(bounds.x, Math.max(0, bounds.y - window.getHeight()));
        bubble = window;
        window.setVisible(true);
        Timer timer = new Timer(3000, e -> { window.dispose(); if (bubble == window) bubble = null; });
        timer.setRepeats(false); timer.start();
    }

    /** A synthetic icon shrinks into the character; it never represents a real file. */
    static void showSnack(java.awt.Rectangle bounds, long request) {
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
        window.setLocation(bounds.x + bounds.width / 2 - 32, bounds.y + bounds.height / 2 - 32);
        animation = window;
        window.setVisible(true);
        Timer timer = new Timer(80, null);
        timer.addActionListener(e -> {
            if (!isCurrent(request) || ++frame[0] >= 17) {
                timer.stop();
                window.dispose();
                if (animation == window) animation = null;
            } else drawing.repaint();
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
