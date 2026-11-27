package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Guid.REFIID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Reads and moves desktop view items through Explorer's IFolderView. */
final class WindowsDesktopIcons {
    static final String AUTO_ARRANGE = "desktopAutoArrange";
    private static final CLSID SHELL_WINDOWS = new CLSID("{9BA05972-F6A8-11CF-A442-00A0C90A8F39}");
    private static final IID IID_SHELL_WINDOWS = new IID("{85CB6900-4D95-11CF-960C-0080C7F4EE85}");
    private static final IID IID_SERVICE_PROVIDER = new IID("{6D5140C1-7436-11CE-8034-00AA006009FA}");
    private static final IID TOP_LEVEL_BROWSER = new IID("{4C96BE40-915C-11CF-99D3-00AA004AE837}");
    private static final IID IID_SHELL_BROWSER = new IID("{000214E2-0000-0000-C000-000000000046}");
    private static final IID IID_FOLDER_VIEW = new IID("{CDE725B0-CCC9-4519-917E-325D72FAB4CE}");
    private static final IID IID_FOLDER_VIEW_2 = new IID("{1AF3A467-214F-4298-908E-06B03E0B39F9}");
    private static final int FWF_AUTOARRANGE = 1;
    private static final int SWC_DESKTOP = 8;
    private static final int SWFO_NEEDDISPATCH = 1;
    private static final int SVGIO_ALLVIEW = 2;
    private static final int SVSI_POSITIONITEM = 0x80;
    private static final int CLSCTX_ALL = 23;
    private static final int RPC_E_CHANGED_MODE = 0x80010106;
    private static final int MAX_ICON_DISTANCE = 160;

    interface ShellItems extends StdCallLibrary {
        ShellItems INSTANCE = Native.load("shell32", ShellItems.class);
        int ILGetSize(Pointer pidl);
    }

    interface DesktopCoordinates extends StdCallLibrary {
        DesktopCoordinates INSTANCE = Native.load("user32", DesktopCoordinates.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean ClientToScreen(HWND window, POINT point);
        HWND WindowFromPoint(POINT.ByValue point);
    }

    Point move(Point anchor, RestoreJournal journal) throws IOException {
        try (FolderView view = FolderView.open()) {
            if (view == null || !User32.INSTANCE.IsWindowVisible(view.list)) return null;
            POINT origin = new POINT();
            POINT spacing = new POINT();
            RECT client = new RECT();
            if (!DesktopCoordinates.INSTANCE.ClientToScreen(view.list, origin) ||
                !User32.INSTANCE.GetClientRect(view.list, client) || !view.spacing(spacing)) return null;

            List<Icon> icons = view.icons();
            Icon closest = null;
            double distance = MAX_ICON_DISTANCE;
            for (Icon icon : icons) {
                POINT.ByValue center = new POINT.ByValue();
                center.x = origin.x + icon.position.x + spacing.x / 2;
                center.y = origin.y + icon.position.y + spacing.y / 3;
                double candidate = anchor.distance(center.x, center.y);
                if (candidate < distance && view.list.equals(DesktopCoordinates.INSTANCE.WindowFromPoint(center))) {
                    closest = icon;
                    distance = candidate;
                }
            }
            if (closest == null) return null;
            int autoArrange = view.autoArrangeState();
            if (!view.enableManualPlacement(journal)) return null;
            if (autoArrange == 0) {
                // Explorer can normalize icon coordinates when switching layouts.
                String targetId = closest.id;
                icons = view.icons();
                closest = icons.stream().filter(icon -> targetId.equals(icon.id)).findFirst().orElse(null);
                if (closest == null) return null;
            }
            Point destination = vacantPosition(closest, icons, spacing, client);
            if (destination == null || !view.position(closest, destination, journal)) return null;
            return new Point(origin.x + closest.position.x + spacing.x / 2,
                origin.y + closest.position.y + spacing.y / 3);
        }
    }

    boolean restoreAutoArrange(JSONObject record) {
        if (!Boolean.TRUE.equals(record.getJSONObject("identity").get("enabled"))) return false;
        try (FolderView view = FolderView.open()) {
            return view != null && view.setAutoArrange(true);
        }
    }

    boolean restore(JSONObject record) {
        JSONObject identity = record.getJSONObject("identity");
        String pidl = identity.getString("pidl");
        if (pidl == null || !identity.containsKey("movedX") || !identity.containsKey("movedY")) return false;
        try (FolderView view = FolderView.open()) {
            if (view == null) return false;
            List<Icon> icons = view.icons();
            for (Icon icon : icons) {
                if (!pidl.equals(icon.id)) continue;
                Point moved = new Point(identity.getIntValue("movedX"), identity.getIntValue("movedY"));
                if (!moved.equals(icon.position)) return true;
                Point original = new Point(record.getIntValue("x"), record.getIntValue("y"));
                POINT spacing = new POINT();
                return view.canPosition() && view.spacing(spacing) &&
                    isVacant(original, icon, icons, spacing) && view.setPosition(icon, original) &&
                    original.equals(view.readPosition(icon));
            }
            // Explorer can populate icons after the desktop view becomes available.
            return false;
        }
    }

    private static Point vacantPosition(Icon icon, List<Icon> icons, POINT spacing, RECT client) {
        Rectangle bounds = new Rectangle(client.left, client.top,
            client.right - client.left - spacing.x + 1, client.bottom - client.top - spacing.y + 1);
        for (int offset = 2; offset <= 5; offset++) {
            Point[] candidates = {
                new Point(icon.position.x + offset * spacing.x, icon.position.y),
                new Point(icon.position.x, icon.position.y + offset * spacing.y),
                new Point(icon.position.x - offset * spacing.x, icon.position.y),
                new Point(icon.position.x, icon.position.y - offset * spacing.y)
            };
            for (Point candidate : candidates) {
                if (bounds.contains(candidate) && isVacant(candidate, icon, icons, spacing)) return candidate;
            }
        }
        return null;
    }

    private static boolean isVacant(Point position, Icon target, List<Icon> icons, POINT spacing) {
        for (Icon icon : icons) {
            if (icon != target && Math.abs(icon.position.x - position.x) < spacing.x &&
                Math.abs(icon.position.y - position.y) < spacing.y) return false;
        }
        return true;
    }

    private static final class Icon {
        final byte[] pidl;
        final String id;
        final Point position;

        Icon(byte[] pidl, Point position) {
            this.pidl = pidl;
            this.id = Base64.getEncoder().encodeToString(pidl);
            this.position = position;
        }
    }

    private static final class ComObject extends Unknown implements AutoCloseable {
        ComObject(Pointer pointer) {
            super(pointer);
        }

        int call(int slot, Object... parameters) {
            Object[] args = new Object[parameters.length + 1];
            args[0] = getPointer();
            System.arraycopy(parameters, 0, args, 1, parameters.length);
            return _invokeNativeInt(slot, args);
        }

        @Override public void close() {
            Release();
        }
    }

    private static final class FolderView implements AutoCloseable {
        private final ComObject folder;
        private final boolean uninitialize;
        private final HWND list;

        private FolderView(Pointer folder, HWND list, boolean uninitialize) {
            this.folder = new ComObject(folder);
            this.list = list;
            this.uninitialize = uninitialize;
        }

        static FolderView open() {
            int initialized = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED).intValue();
            if (initialized < 0 && initialized != RPC_E_CHANGED_MODE) return null;
            FolderView view = null;
            try {
                view = findDesktop(initialized >= 0);
                return view;
            } finally {
                if (view == null && initialized >= 0) Ole32.INSTANCE.CoUninitialize();
            }
        }

        private static FolderView findDesktop(boolean uninitialize) {
            PointerByReference result = new PointerByReference();
            if (Ole32.INSTANCE.CoCreateInstance(SHELL_WINDOWS, null, CLSCTX_ALL, IID_SHELL_WINDOWS, result).intValue() != 0)
                return null;
            try (ComObject windows = new ComObject(result.getValue())) {
                // IShellWindows::FindWindowSW
                if (windows.call(15, new VARIANT(0), new VARIANT(), SWC_DESKTOP,
                    new IntByReference(), SWFO_NEEDDISPATCH, result) != 0) return null;
                try (ComObject dispatch = new ComObject(result.getValue())) {
                    if (dispatch.QueryInterface(new REFIID(IID_SERVICE_PROVIDER), result).intValue() != 0) return null;
                    try (ComObject services = new ComObject(result.getValue())) {
                        // IServiceProvider::QueryService
                        if (services.call(3, TOP_LEVEL_BROWSER, IID_SHELL_BROWSER, result) != 0) return null;
                    }
                    try (ComObject browser = new ComObject(result.getValue())) {
                        // IShellBrowser::QueryActiveShellView
                        if (browser.call(15, result) != 0) return null;
                    }
                    try (ComObject shellView = new ComObject(result.getValue())) {
                        // IOleWindow::GetWindow
                        if (shellView.call(3, result) != 0) return null;
                        HWND list = User32.INSTANCE.FindWindowEx(new HWND(result.getValue()), null, "SysListView32", null);
                        if (list == null || shellView.QueryInterface(new REFIID(IID_FOLDER_VIEW), result).intValue() != 0)
                            return null;
                        return new FolderView(result.getValue(), list, uninitialize);
                    }
                }
            }
        }

        boolean canPosition() {
            // IFolderView::GetAutoArrange returns S_FALSE when auto-arrange is off.
            return autoArrangeState() == 1;
        }

        private int autoArrangeState() {
            return folder.call(14);
        }

        private boolean setAutoArrange(boolean enabled) {
            int current = autoArrangeState();
            int desired = enabled ? 0 : 1;
            if (current == desired) return true;
            if (current != 0 && current != 1) return false;

            PointerByReference result = new PointerByReference();
            if (folder.QueryInterface(new REFIID(IID_FOLDER_VIEW_2), result).intValue() != 0) return false;
            try (ComObject folderView2 = new ComObject(result.getValue())) {
                // IFolderView2::SetCurrentFolderFlags applies only the bits in the mask.
                folderView2.call(24, FWF_AUTOARRANGE, enabled ? FWF_AUTOARRANGE : 0);
                return autoArrangeState() == desired;
            }
        }

        private boolean enableManualPlacement(RestoreJournal journal) throws IOException {
            int current = autoArrangeState();
            if (current == 1) return true;
            if (current != 0) return false;
            // If the user has turned auto-arrange back on, respect that choice.
            for (JSONObject record : journal.pending()) {
                if (AUTO_ARRANGE.equals(record.getString("kind"))) return false;
            }
            JSONObject setting = new JSONObject();
            setting.put("enabled", true);
            String id = journal.record(AUTO_ARRANGE, setting, 0, 0);
            if (setAutoArrange(false)) return true;
            if (autoArrangeState() == 0) journal.discard(id);
            return false;
        }

        boolean spacing(POINT spacing) {
            return folder.call(12, spacing) == 0 && spacing.x > 0 && spacing.y > 0;
        }

        List<Icon> icons() {
            IntByReference count = new IntByReference();
            List<Icon> icons = new ArrayList<>();
            if (folder.call(7, SVGIO_ALLVIEW, count) != 0) return icons;
            PointerByReference item = new PointerByReference();
            for (int index = 0; index < count.getValue(); index++) {
                // IFolderView::Item allocates the relative PIDL with the COM allocator.
                if (folder.call(6, index, item) != 0) continue;
                Pointer pidl = item.getValue();
                try {
                    Point position = readPosition(pidl);
                    if (position != null) icons.add(new Icon(pidl.getByteArray(0, ShellItems.INSTANCE.ILGetSize(pidl)), position));
                } finally {
                    Ole32.INSTANCE.CoTaskMemFree(pidl);
                }
            }
            return icons;
        }

        boolean position(Icon icon, Point destination, RestoreJournal journal) throws IOException {
            if (!canPosition() || !icon.position.equals(readPosition(icon))) return false;
            for (JSONObject record : journal.pending()) {
                if ("icon".equals(record.getString("kind")) &&
                    icon.id.equals(record.getJSONObject("identity").getString("pidl"))) return false;
            }
            JSONObject identity = new JSONObject();
            identity.put("pidl", icon.id);
            identity.put("movedX", destination.x);
            identity.put("movedY", destination.y);
            String id = journal.record("icon", identity, icon.position.x, icon.position.y);
            setPosition(icon, destination);
            Point actual = readPosition(icon);
            if (actual == null) return false;
            if (actual.equals(icon.position)) {
                journal.discard(id);
                return false;
            }
            if (!actual.equals(destination)) journal.updateDestination(id, actual);
            return true;
        }

        private Point readPosition(Icon icon) {
            try (Memory pidl = new Memory(icon.pidl.length)) {
                pidl.write(0, icon.pidl, 0, icon.pidl.length);
                return readPosition(pidl);
            }
        }

        private Point readPosition(Pointer pidl) {
            POINT position = new POINT();
            // IFolderView::GetItemPosition
            return folder.call(11, pidl, position) == 0 ? new Point(position.x, position.y) : null;
        }

        private boolean setPosition(Icon icon, Point target) {
            try (Memory pidl = new Memory(icon.pidl.length)) {
                pidl.write(0, icon.pidl, 0, icon.pidl.length);
                POINT position = new POINT(target.x, target.y);
                // IFolderView::SelectAndPositionItems with positioning only.
                return folder.call(16, 1, new Pointer[]{pidl}, position, SVSI_POSITIONITEM) == 0;
            }
        }

        @Override public void close() {
            try {
                folder.close();
            } finally {
                if (uninitialize) Ole32.INSTANCE.CoUninitialize();
            }
        }
    }
}
