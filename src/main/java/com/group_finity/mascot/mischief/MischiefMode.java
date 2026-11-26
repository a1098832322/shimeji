package com.group_finity.mascot.mischief;

import java.util.Map;
import java.util.Properties;
import com.alibaba.fastjson.JSONObject;

/** Runtime-only settings; unsupported disruptive actions remain disabled until recoverable. */
public final class MischiefMode {
    private static volatile boolean enabled;
    private static volatile boolean pushCursor;
    private static String previousBreeding;
    private static String previousThrowing;
    private static final Map<String, Integer> FREQUENCIES = Map.ofEntries(
        Map.entry("SitDown", 50), Map.entry("LieDown", 40), Map.entry("SitWhileDanglingLegs", 30),
        Map.entry("WalkAlongWorkAreaFloor", 300), Map.entry("RunAlongWorkAreaFloor", 200),
        Map.entry("JumpFromBottomOfIE", 150), Map.entry("ChaseMouse", 75), Map.entry("SplitIntoTwo", 200), Map.entry("PullUpShimeji", 200));
    private MischiefMode() {}
    public static boolean isEnabled() { return enabled; }
    public static boolean isCursorEnabled() { return enabled && pushCursor; }
    static synchronized boolean moveCursor(Runnable move) {
        if (!enabled || !pushCursor) return false;
        move.run();
        return true;
    }
    public static int frequency(String name, int normal) { return enabled ? FREQUENCIES.getOrDefault(name, normal) : normal; }
    public static synchronized void set(Properties properties, boolean value, boolean cursor) {
        if (value && !enabled) {
            previousBreeding = properties.getProperty("Breeding", "true");
            previousThrowing = properties.getProperty("Throwing", "true");
            properties.setProperty("Breeding", "true");
            // Never run the original unrestricted ThrowIE in this mode: it has no recoverable journal.
            properties.setProperty("Throwing", "false");
        } else if (!value && enabled) {
            properties.setProperty("Breeding", previousBreeding);
            properties.setProperty("Throwing", previousThrowing);
        }
        if (!value) VisualIcon.close();
        pushCursor = cursor;
        enabled = value;
    }
    public static JSONObject status() {
        JSONObject result = new JSONObject();
        result.put("enabled", enabled);
        result.put("throwWindow", "unsupported");
        result.put("eatIcon", "visual");
        result.put("pushCursor", CursorPush.capability());
        result.put("pushCursorEnabled", pushCursor);
        return result;
    }
}
