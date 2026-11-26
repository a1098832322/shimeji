package com.group_finity.mascot.generic;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.junit.Assume;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.Assert.*;

public class LinuxWindowSpacesTest {
    @Test
    public void newWindowRequestsAllWorkspaces() throws Exception {
        Assume.assumeTrue(Platform.isLinux() && !GraphicsEnvironment.isHeadless()
                && System.getenv("DISPLAY") != null);
        GenericTranslucentWindow[] window = new GenericTranslucentWindow[1];
        X11.Display display = X11.INSTANCE.XOpenDisplay(null);
        Assume.assumeNotNull(display);
        try {
            SwingUtilities.invokeAndWait(() -> {
                window[0] = new GenericTranslucentWindow();
                window[0].addNotify();
            });
            X11 x11 = X11.INSTANCE;
            PointerByReference value = new PointerByReference();
            IntByReference format = new IntByReference();
            NativeLongByReference count = new NativeLongByReference();
            int result = x11.XGetWindowProperty(display, new X11.Window(Native.getWindowID(window[0])),
                    x11.XInternAtom(display, "_NET_WM_DESKTOP", false), new NativeLong(0), new NativeLong(1),
                    false, X11.XA_CARDINAL, new X11.AtomByReference(), format, count,
                    new NativeLongByReference(), value);
            assertEquals(X11.Success, result);
            assertEquals(32, format.getValue());
            assertEquals(1, count.getValue().longValue());
            Pointer data = value.getValue();
            assertNotNull(data);
            try {
                assertEquals(0xffffffffL, data.getNativeLong(0).longValue() & 0xffffffffL);
            } finally {
                x11.XFree(data);
            }
        } finally {
            if (window[0] != null) SwingUtilities.invokeAndWait(window[0]::dispose);
            X11.INSTANCE.XCloseDisplay(display);
        }
    }
}
