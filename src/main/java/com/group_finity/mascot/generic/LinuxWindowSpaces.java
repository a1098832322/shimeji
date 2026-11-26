package com.group_finity.mascot.generic;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.X11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Window;

/** Requests all-workspace placement using the X11 EWMH window-manager protocol. */
final class LinuxWindowSpaces {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxWindowSpaces.class);

    private LinuxWindowSpaces() { }

    static void showOnAllWorkspaces(Window window) {
        X11.Display display = null;
        try {
            X11 x11 = X11.INSTANCE;
            display = x11.XOpenDisplay(null);
            if (display == null) {
                LOG.warn("无法连接 X11，桌宠跨工作区显示不可用");
                return;
            }
            long id = Native.getWindowID(window);
            if (id == 0) {
                LOG.warn("无法获取 X11 桌宠窗口，跨工作区显示不可用");
                return;
            }
            X11.Atom property = x11.XInternAtom(display, "_NET_WM_DESKTOP", false);
            Memory value = new Memory(NativeLong.SIZE);
            value.setNativeLong(0, new NativeLong(0xffffffffL));
            x11.XChangeProperty(display, new X11.Window(id), property, X11.XA_CARDINAL,
                    32, X11.PropModeReplace, value, 1);
            x11.XFlush(display);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("设置 Linux 桌宠跨工作区显示失败", e);
        } finally {
            if (display != null) X11.INSTANCE.XCloseDisplay(display);
        }
    }
}
