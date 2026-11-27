package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import com.group_finity.mascot.win.jna.Dwmapi;
import java.time.Instant;

/** Uses a window handle plus process birth time and class to avoid restoring a reused handle. */
final class WindowsWindowMoveBackend implements RecoverableWindowMoves.Backend {
    private static final int NO_SIZE = 0x0001, NO_ZORDER = 0x0004, NO_ACTIVATE = 0x0010;

    @Override public JSONObject capture(long handle) {
        HWND window = new HWND(Pointer.createConstant(handle));
        if (!canMove(window)) return null;
        IntByReference cloaked = new IntByReference();
        int result = Dwmapi.INSTANCE.DwmGetWindowAttribute(window.getPointer(), Dwmapi.DWMWA_CLOAKED, cloaked, Integer.BYTES);
        if (result != 0x80070057 && (result != 0 || cloaked.getValue() != 0)) return null;
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, pid);
        if (pid.getValue() == 0 || pid.getValue() == Kernel32.INSTANCE.GetCurrentProcessId()) return null;
        long processId = Integer.toUnsignedLong(pid.getValue());
        Instant started = processStart(processId);
        if (started == null) return null;
        char[] name = new char[256];
        if (User32.INSTANCE.GetClassName(window, name, name.length) == 0) return null;
        String windowClass = Native.toString(name);
        if (windowClass.equals("Progman") || windowClass.equals("WorkerW") ||
            windowClass.equals("Shell_TrayWnd") || windowClass.equals("Shell_SecondaryTrayWnd")) return null;
        RECT rect = new RECT();
        if (!User32.INSTANCE.GetWindowRect(window, rect) || rect.right <= rect.left || rect.bottom <= rect.top) return null;
        JSONObject identity = new JSONObject();
        identity.put("handle", Pointer.nativeValue(window.getPointer()));
        identity.put("pid", processId);
        identity.put("started", started.toEpochMilli());
        identity.put("class", windowClass);
        identity.put("x", rect.left); identity.put("y", rect.top);
        identity.put("width", rect.right - rect.left); identity.put("height", rect.bottom - rect.top);
        return identity;
    }

    @Override public boolean move(JSONObject identity, int x, int y) {
        HWND window = matchingWindow(identity);
        RECT current = new RECT();
        return window != null && canMove(window) && User32.INSTANCE.GetWindowRect(window, current) &&
            samePosition(current, identity.getIntValue("x"), identity.getIntValue("y")) &&
            current.right - current.left == identity.getIntValue("width") &&
            current.bottom - current.top == identity.getIntValue("height") &&
            position(window, x, y);
    }

    @Override public boolean restore(JSONObject record) {
        if (!"window".equals(record.getString("kind"))) return false;
        JSONObject identity = record.getJSONObject("identity");
        if (identity == null || !identity.containsKey("handle") || !identity.containsKey("pid") ||
            !identity.containsKey("started") || identity.getString("class") == null ||
            !identity.containsKey("movedX") || !identity.containsKey("movedY")) return false;
        HWND window = matchingWindow(identity);
        if (window == null) return true;
        if (!canMove(window)) return false;
        RECT current = new RECT();
        if (!User32.INSTANCE.GetWindowRect(window, current)) return false;
        int originalX = record.getIntValue("x"), originalY = record.getIntValue("y");
        // If the window has since been moved, keep the user's placement.
        if (!samePosition(current, identity.getIntValue("movedX"), identity.getIntValue("movedY"))) return true;
        return position(window, originalX, originalY);
    }

    private static boolean canMove(HWND window) {
        return User32.INSTANCE.IsWindowVisible(window) &&
            com.group_finity.mascot.win.jna.User32.INSTANCE.IsIconic(window.getPointer()) == 0 &&
            com.group_finity.mascot.win.jna.User32.INSTANCE.IsZoomed(window.getPointer()) == 0;
    }

    private static boolean samePosition(RECT rect, int x, int y) {
        return rect.left == x && rect.top == y;
    }

    private static boolean position(HWND window, int x, int y) {
        RECT current = new RECT();
        return User32.INSTANCE.SetWindowPos(window, null, x, y, 0, 0, NO_SIZE | NO_ZORDER | NO_ACTIVATE) &&
            User32.INSTANCE.GetWindowRect(window, current) && samePosition(current, x, y);
    }

    private static Instant processStart(long pid) {
        return ProcessHandle.of(pid).flatMap(process -> process.info().startInstant()).orElse(null);
    }

    private static HWND matchingWindow(JSONObject identity) {
        HWND window = new HWND(Pointer.createConstant(identity.getLongValue("handle")));
        if (!User32.INSTANCE.IsWindow(window)) return null;
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, pid);
        if (Integer.toUnsignedLong(pid.getValue()) != identity.getLongValue("pid")) return null;
        Instant started = processStart(identity.getLongValue("pid"));
        if (started == null || started.toEpochMilli() != identity.getLongValue("started")) return null;
        char[] name = new char[256];
        if (User32.INSTANCE.GetClassName(window, name, name.length) == 0 ||
                !Native.toString(name).equals(identity.getString("class"))) return null;
        return window;
    }
}
