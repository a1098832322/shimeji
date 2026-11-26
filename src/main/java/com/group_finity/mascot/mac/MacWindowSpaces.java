package com.group_finity.mascot.mac;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** Applies AppKit window collection behavior on the macOS main thread. */
final class MacWindowSpaces {
    private static final Logger LOG = LoggerFactory.getLogger(MacWindowSpaces.class);
    private static final long CAN_JOIN_ALL_SPACES = 1L;
    private static final long MOVE_TO_ACTIVE_SPACE = 1L << 1;
    private static final long FULL_SCREEN_PRIMARY = 1L << 7;
    private static final long FULL_SCREEN_AUXILIARY = 1L << 8;
    private static final long FULL_SCREEN_NONE = 1L << 9;
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final ConcurrentMap<Long, String> PENDING = new ConcurrentHashMap<>();

    private interface Dispatch extends Library {
        void dispatch_async_f(Pointer queue, Pointer context, MainThreadCallback callback);
    }
    private interface MainThreadCallback extends Callback {
        void invoke(Pointer context);
    }

    // Keep the callback reachable until all asynchronous dispatches have completed.
    private static final MainThreadCallback CALLBACK = context -> {
        String marker = PENDING.remove(Pointer.nativeValue(context));
        if (marker == null) return;
        try {
            if (!apply(marker)) LOG.warn("无法找到 macOS 桌宠窗口，跨桌面显示未启用");
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("设置 macOS 桌宠跨桌面显示失败", e);
        }
    };

    private MacWindowSpaces() { }

    static void showOnAllSpaces(String marker) {
        long id = NEXT_ID.incrementAndGet();
        PENDING.put(id, marker);
        try {
            NativeLibrary system = NativeLibrary.getInstance("System");
            Pointer mainQueue = system.getGlobalVariableAddress("_dispatch_main_q");
            Native.load("System", Dispatch.class).dispatch_async_f(mainQueue, Pointer.createConstant(id), CALLBACK);
        } catch (RuntimeException | LinkageError e) {
            PENDING.remove(id);
            LOG.warn("无法调度 macOS 桌宠窗口设置", e);
        }
    }

    private static boolean apply(String marker) {
        NativeLibrary objc = NativeLibrary.getInstance("objc");
        Function getClass = objc.getFunction("objc_getClass");
        Function selector = objc.getFunction("sel_registerName");
        Function send = objc.getFunction("objc_msgSend");
        Pointer application = message(send, selector, getClass.invokePointer(new Object[]{"NSApplication"}), "sharedApplication");
        Pointer windows = message(send, selector, application, "windows");
        long count = send.invokeLong(new Object[]{windows, selector.invokePointer(new Object[]{"count"})});
        for (long i = 0; i < count; i++) {
            Pointer window = send.invokePointer(new Object[]{windows, selector.invokePointer(new Object[]{"objectAtIndex:"}), i});
            Pointer filename = message(send, selector, window, "representedFilename");
            Pointer utf8 = message(send, selector, filename, "UTF8String");
            if (utf8 == null || !marker.equals(utf8.getString(0))) continue;
            long behavior = send.invokeLong(new Object[]{window, selector.invokePointer(new Object[]{"collectionBehavior"})});
            behavior = (behavior & ~(MOVE_TO_ACTIVE_SPACE | FULL_SCREEN_PRIMARY | FULL_SCREEN_NONE))
                    | CAN_JOIN_ALL_SPACES | FULL_SCREEN_AUXILIARY;
            send.invokeVoid(new Object[]{window, selector.invokePointer(new Object[]{"setCollectionBehavior:"}), behavior});
            return true;
        }
        return false;
    }

    private static Pointer message(Function send, Function selector, Pointer receiver, String name) {
        return send.invokePointer(new Object[]{receiver, selector.invokePointer(new Object[]{name})});
    }
}
