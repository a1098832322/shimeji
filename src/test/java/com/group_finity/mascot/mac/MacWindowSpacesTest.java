package com.group_finity.mascot.mac;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import org.junit.Assume;
import org.junit.Test;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class MacWindowSpacesTest {
    private interface Dispatch extends Library {
        void dispatch_async_f(Pointer queue, Pointer context, Callback callback);
    }
    private interface MainCallback extends Callback { void invoke(Pointer context); }

    @Test
    public void eachMascotWindowJoinsAllSpacesAndFullScreen() throws Exception {
        Assume.assumeTrue(Platform.isMac() && !GraphicsEnvironment.isHeadless());
        JWindow[] windows = new JWindow[2];
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < windows.length; i++) {
                    com.group_finity.mascot.generic.NativeFactoryImpl factory =
                            new com.group_finity.mascot.generic.NativeFactoryImpl();
                    MacTranslucentWindow mascot = new MacTranslucentWindow(factory);
                    mascot.setImage(factory.newNativeImage(new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)));
                    windows[i] = (JWindow) mascot.asComponent();
                    windows[i].setSize(40, 40);
                    windows[i].setLocation(-1000, -1000);
                    windows[i].setAlwaysOnTop(true);
                    windows[i].setVisible(true);
                }
            });
            SwingUtilities.invokeAndWait(() -> { }); // process the displayability callbacks
            for (JWindow window : windows) {
                String marker = ((File) window.getRootPane().getClientProperty("Window.documentFile")).getAbsolutePath();
                long behavior = readBehavior(marker).get(5, TimeUnit.SECONDS);
                assertTrue("can join all spaces", (behavior & 1L) != 0);
                assertTrue("full-screen auxiliary", (behavior & (1L << 8)) != 0);
                assertEquals(0, behavior & ((1L << 1) | (1L << 7) | (1L << 9)));
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (JWindow window : windows) if (window != null) window.dispose();
            });
        }
    }

    private static CompletableFuture<Long> readBehavior(String marker) {
        CompletableFuture<Long> result = new CompletableFuture<>();
        MainCallback callback = context -> {
            try {
                NativeLibrary objc = NativeLibrary.getInstance("objc");
                Function send = objc.getFunction("objc_msgSend");
                Function sel = objc.getFunction("sel_registerName");
                Pointer app = msg(send, sel, objc.getFunction("objc_getClass").invokePointer(new Object[]{"NSApplication"}), "sharedApplication");
                Pointer list = msg(send, sel, app, "windows");
                long count = send.invokeLong(new Object[]{list, sel.invokePointer(new Object[]{"count"})});
                for (long i = 0; i < count; i++) {
                    Pointer window = send.invokePointer(new Object[]{list, sel.invokePointer(new Object[]{"objectAtIndex:"}), i});
                    Pointer filename = msg(send, sel, window, "representedFilename");
                    Pointer utf8 = msg(send, sel, filename, "UTF8String");
                    if (utf8 != null && marker.equals(utf8.getString(0))) {
                        result.complete(send.invokeLong(new Object[]{window, sel.invokePointer(new Object[]{"collectionBehavior"})}));
                        return;
                    }
                }
                result.completeExceptionally(new AssertionError("Window not found: " + marker));
            } catch (Throwable e) {
                result.completeExceptionally(e);
            }
        };
        Native.load("System", Dispatch.class).dispatch_async_f(
                NativeLibrary.getInstance("System").getGlobalVariableAddress("_dispatch_main_q"), null, callback);
        // Keep the JNA callback strongly reachable until it runs.
        result.whenComplete((value, error) -> callback.hashCode());
        return result;
    }

    private static Pointer msg(Function send, Function sel, Pointer receiver, String name) {
        return send.invokePointer(new Object[]{receiver, sel.invokePointer(new Object[]{name})});
    }
}
