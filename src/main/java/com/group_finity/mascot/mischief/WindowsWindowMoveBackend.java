package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import java.util.Optional;

/** Uses a window handle plus process birth time and class to avoid restoring a reused handle. */
final class WindowsWindowMoveBackend implements RecoverableWindowMoves.Backend {
    private static final int NO_SIZE = 0x0001, NO_ZORDER = 0x0004, NO_ACTIVATE = 0x0010;

    @Override public JSONObject capture() {
        HWND window = User32.INSTANCE.GetForegroundWindow();
        if (window == null || !User32.INSTANCE.IsWindowVisible(window)) return null;
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, pid);
        if (pid.getValue() == 0 || pid.getValue() == Kernel32.INSTANCE.GetCurrentProcessId()) return null;
        Optional<ProcessHandle> process = ProcessHandle.of(Integer.toUnsignedLong(pid.getValue()));
        if (!process.isPresent() || !process.get().info().startInstant().isPresent()) return null;
        char[] name = new char[256];
        if (User32.INSTANCE.GetClassName(window, name, name.length) == 0) return null;
        String windowClass = Native.toString(name);
        if (windowClass.equals("Progman") || windowClass.equals("WorkerW") || windowClass.equals("Shell_TrayWnd")) return null;
        RECT rect = new RECT();
        if (!User32.INSTANCE.GetWindowRect(window, rect) || rect.right <= rect.left || rect.bottom <= rect.top) return null;
        JSONObject identity = new JSONObject();
        identity.put("handle", Pointer.nativeValue(window.getPointer()));
        identity.put("pid", Integer.toUnsignedLong(pid.getValue()));
        identity.put("started", process.get().info().startInstant().get().toEpochMilli());
        identity.put("class", windowClass);
        identity.put("x", rect.left); identity.put("y", rect.top);
        identity.put("width", rect.right - rect.left); identity.put("height", rect.bottom - rect.top);
        return identity;
    }

    @Override public boolean move(JSONObject identity, int x, int y) {
        HWND window = matchingWindow(identity);
        return window != null && User32.INSTANCE.SetWindowPos(window, null, x, y, 0, 0, NO_SIZE | NO_ZORDER | NO_ACTIVATE);
    }

    @Override public boolean restore(JSONObject record) {
        if (!"window".equals(record.getString("kind"))) return false;
        JSONObject identity = record.getJSONObject("identity");
        HWND window = matchingWindow(identity);
        if (window == null) return false;
        RECT current = new RECT();
        if (!User32.INSTANCE.GetWindowRect(window, current)) return false;
        int originalX = record.getIntValue("x"), originalY = record.getIntValue("y");
        if (current.left == originalX && current.top == originalY) return true;
        // Do not undo a user's subsequent manual repositioning.
        if (!identity.containsKey("movedX") || !identity.containsKey("movedY") ||
                Math.abs(current.left - identity.getIntValue("movedX")) > 2 ||
                Math.abs(current.top - identity.getIntValue("movedY")) > 2) return false;
        return User32.INSTANCE.SetWindowPos(window, null, originalX, originalY, 0, 0,
                NO_SIZE | NO_ZORDER | NO_ACTIVATE);
    }

    private HWND matchingWindow(JSONObject identity) {
        if (identity == null || !identity.containsKey("handle") || !identity.containsKey("pid") ||
                !identity.containsKey("started") || identity.getString("class") == null) return null;
        HWND window = new HWND(Pointer.createConstant(identity.getLongValue("handle")));
        if (!User32.INSTANCE.IsWindow(window)) return null;
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, pid);
        if (Integer.toUnsignedLong(pid.getValue()) != identity.getLongValue("pid")) return null;
        Optional<ProcessHandle> process = ProcessHandle.of(identity.getLongValue("pid"));
        if (!process.isPresent() || !process.get().info().startInstant().isPresent() ||
                process.get().info().startInstant().get().toEpochMilli() != identity.getLongValue("started")) return null;
        char[] name = new char[256];
        if (User32.INSTANCE.GetClassName(window, name, name.length) == 0 ||
                !Native.toString(name).equals(identity.getString("class"))) return null;
        return window;
    }
}
