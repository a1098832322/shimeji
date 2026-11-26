package com.group_finity.mascot.win;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.BOOLByReference;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Window;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Uses the documented virtual desktop API to follow an active foreground window. */
final class WindowsWindowSpaces {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsWindowSpaces.class);
    private static final GUID CLSID = new GUID("{AA509086-5CA9-4C25-8F95-589D3C07B48A}");
    private static final GUID IID = new GUID("{A5CD92FF-29BE-454C-8D04-D82879FB3F1B}");
    private static final Map<Window, HWND> WINDOWS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "shimeji-virtual-desktop");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean warned;

    static {
        WORKER.scheduleWithFixedDelay(WindowsWindowSpaces::followForeground, 500, 500, TimeUnit.MILLISECONDS);
    }

    private WindowsWindowSpaces() { }

    static void register(Window window) {
        try {
            Pointer pointer = Native.getWindowPointer(window);
            synchronized (WINDOWS) {
                if (pointer != null && Pointer.nativeValue(pointer) != 0) WINDOWS.put(window, new HWND(pointer));
            }
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        }
    }

    static void unregister(Window window) {
        synchronized (WINDOWS) { WINDOWS.remove(window); }
    }

    private static void followForeground() {
        if (WINDOWS.isEmpty()) return;
        boolean initialized = false;
        DesktopManager manager = null;
        try {
            int init = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED).intValue();
            if (init < 0) throw new IllegalStateException("CoInitializeEx: " + init);
            initialized = true;
            PointerByReference result = new PointerByReference();
            int created = Ole32.INSTANCE.CoCreateInstance(CLSID, null, 1, IID, result).intValue();
            if (created < 0) throw new IllegalStateException("IVirtualDesktopManager: " + created);
            manager = new DesktopManager(result.getValue());
            HWND foreground = User32.INSTANCE.GetForegroundWindow();
            if (foreground == null) return;
            IntByReference process = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(foreground, process);
            if (process.getValue() == Kernel32.INSTANCE.GetCurrentProcessId()) return;
            BOOLByReference foregroundCurrent = new BOOLByReference();
            if (manager.isCurrent(foreground, foregroundCurrent) < 0 || !foregroundCurrent.getValue().booleanValue()) return;
            GUID desktop = new GUID();
            if (manager.getDesktop(foreground, desktop) < 0) return;
            synchronized (WINDOWS) {
                for (Map.Entry<Window, HWND> entry : WINDOWS.entrySet()) {
                    HWND handle = entry.getValue();
                    if (!User32.INSTANCE.IsWindow(handle)) continue;
                    BOOLByReference current = new BOOLByReference();
                    if (manager.isCurrent(handle, current) == 0 && !current.getValue().booleanValue()) {
                        int moved = manager.move(handle, desktop);
                        if (moved < 0) warn(new IllegalStateException("MoveWindowToDesktop: " + moved));
                    }
                }
            }
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        } finally {
            if (manager != null) manager.Release();
            if (initialized) Ole32.INSTANCE.CoUninitialize();
        }
    }

    private static void warn(Throwable error) {
        if (!warned) {
            warned = true;
            LOG.warn("Windows 桌宠跟随虚拟桌面不可用", error);
        }
    }

    private static final class DesktopManager extends Unknown {
        DesktopManager(Pointer pointer) { super(pointer); }
        int isCurrent(HWND window, BOOLByReference current) {
            return _invokeNativeInt(3, new Object[]{getPointer(), window, current});
        }
        int getDesktop(HWND window, GUID desktop) {
            return _invokeNativeInt(4, new Object[]{getPointer(), window, desktop});
        }
        int move(HWND window, GUID desktop) {
            return _invokeNativeInt(5, new Object[]{getPointer(), window, desktop});
        }
    }
}
