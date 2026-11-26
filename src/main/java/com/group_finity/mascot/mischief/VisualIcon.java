package com.group_finity.mascot.mischief;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.glados.GladosEventReporter;
import com.group_finity.mascot.glados.GladosOptions;

/** A pretend icon snack: never changes a file or an operating system icon. */
public final class VisualIcon {
    private static long lastCheck;
    private static long lastSnack;
    private static volatile JWindow bubble;
    private VisualIcon() {}
    public static void tick(Mascot mascot) {
        if (!MischiefMode.isEnabled() || !Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Mischief.EatIconEnabled", "true"))) return;
        if (GladosOptions.isManagedProcess() && mascot.getManager() != null && mascot != mascot.getManager().getPrimaryMascot()) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck < 5000 || now - lastSnack < 30000) return;
        lastCheck = now;
        if (Math.random() >= 0.2 || !mascot.getEnvironment().getFloor().isOn(mascot.getAnchor())) return;
        try {
            mascot.setBehavior(Main.getInstance().getConfiguration(mascot.getImageSet()).buildBehavior("SitAndSpinHead", mascot));
            lastSnack = now;
            if (GladosOptions.isManagedProcess()) {
                GladosEventReporter.report("mischief", "{\"kind\":\"eatIcon\",\"id\":\"" + mascot.getInstanceId() + "\"}");
            } else {
                final java.awt.Rectangle bounds = mascot.getBounds();
                SwingUtilities.invokeLater(() -> show(bounds));
            }
        } catch (Exception ignored) { /* The skin may not provide this optional animation. */ }
    }
    private static void show(java.awt.Rectangle bounds) {
        if (!MischiefMode.isEnabled()) return;
        close();
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
    public static void close() {
        JWindow current = bubble;
        bubble = null;
        if (current != null) SwingUtilities.invokeLater(current::dispose);
    }
}
