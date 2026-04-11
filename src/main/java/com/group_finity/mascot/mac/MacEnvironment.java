package com.group_finity.mascot.mac;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.lang.management.ManagementFactory;

import com.sun.jna.Pointer;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.mac.jna.Carbon;
import com.group_finity.mascot.mac.jna.ProcessSerialNumber;
import com.group_finity.mascot.mac.jna.AXValueRef;
import com.group_finity.mascot.mac.jna.AXUIElementRef;
import com.group_finity.mascot.mac.jna.CFTypeRef;
import com.group_finity.mascot.mac.jna.CGPoint;
import com.group_finity.mascot.mac.jna.CGSize;
import com.group_finity.mascot.mac.jna.CFStringRef;
import com.group_finity.mascot.mac.jna.CFNumberRef;
import com.group_finity.mascot.mac.jna.CFArrayRef;
import com.group_finity.mascot.util.DPIScaler;

/**
 * Java では取得が難しい環境情報をAccessibility APIを使用して取得する.
 */
class MacEnvironment extends Environment {

  /**
		Mac では、アクティブなウィンドウを取れるので、
		それにしめじが反応するようにする。

		なので、このクラス内では、activeIE に frontmostWindow という別名をつける
   */
	private static Area activeIE = new Area();
  private static Area frontmostWindow = activeIE;

	// 使用 DPIScaler 获取正确的屏幕尺寸（逻辑像素）
	// Java 坐标系统使用逻辑像素，已经考虑了 Retina 显示器的缩放
	private static int getScreenWidth() {
		return (int) com.group_finity.mascot.util.DPIScaler.getLogicalScreenBounds().getWidth();
	}
	
	private static int getScreenHeight() {
		return (int) com.group_finity.mascot.util.DPIScaler.getLogicalScreenBounds().getHeight();
	}

	private static Carbon carbon = Carbon.INSTANCE;

	// Mac では、ManagementFactory.getRuntimeMXBean().getName()で
	// PID@マシン名 の文字列が返ってくる
	private static long myPID =
		Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

	private static long currentPID = myPID;

	private static HashSet<Long> touchedProcesses = new HashSet<Long>();

	static final CFStringRef
  	kAXPosition = createCFString("AXPosition"),
		kAXSize = createCFString("AXSize"),
		kAXFocusedWindow = createCFString("AXFocusedWindow"),
		kDock = createCFString("com.apple.Dock"),
		kTileSize = createCFString("tilesize"),
		kOrientation = createCFString("orientation"),
		kAXChildren = createCFString("AXChildren");

  private static Rectangle getFrontmostAppRect() {
		Rectangle ret;
		long pid = getCurrentPID();

		AXUIElementRef application =
			carbon.AXUIElementCreateApplication(pid);

		PointerByReference windowp = new PointerByReference();

		// XXX: ここ以外でもエラーチェックは必要?
		if (carbon.AXUIElementCopyAttributeValue(
					application, kAXFocusedWindow, windowp) == carbon.kAXErrorSuccess) {
			AXUIElementRef window = new AXUIElementRef();
			window.setPointer(windowp.getValue());
			ret = getRectOfWindow(window);
		} else {
			ret = null;
		}

		carbon.CFRelease(application);
		return ret;
  }

	private static long getFrontmostAppsPID() {
		ProcessSerialNumber frontProcessPsn = new ProcessSerialNumber();
		LongByReference frontProcessPidp = new LongByReference();

		carbon.GetFrontProcess(frontProcessPsn);
		carbon.GetProcessPID(frontProcessPsn, frontProcessPidp);

		return frontProcessPidp.getValue();
	}

	private static CGPoint getPositionOfWindow(AXUIElementRef window) {
		CGPoint position = new CGPoint();
		AXValueRef axvalue = new AXValueRef();
		PointerByReference valuep = new PointerByReference();

		carbon.AXUIElementCopyAttributeValue(window, kAXPosition, valuep);
		axvalue.setPointer(valuep.getValue());
		carbon.AXValueGetValue(axvalue, carbon.kAXValueCGPointType, position.getPointer());
		position.read();

		return position;
	}

	private static CGSize getSizeOfWindow(AXUIElementRef window) {
		CGSize size = new CGSize();
		AXValueRef axvalue = new AXValueRef();
		PointerByReference valuep = new PointerByReference();

		carbon.AXUIElementCopyAttributeValue(window, kAXSize, valuep);
		axvalue.setPointer(valuep.getValue());
		carbon.AXValueGetValue(axvalue, carbon.kAXValueCGSizeType, size.getPointer());
		size.read();

		return size;
	}

	private static void moveFrontmostWindow(final Point point) {
		AXUIElementRef application =
			carbon.AXUIElementCreateApplication(currentPID);

		PointerByReference windowp = new PointerByReference();

		if (carbon.AXUIElementCopyAttributeValue(
					application, kAXFocusedWindow, windowp) == carbon.kAXErrorSuccess) {
			AXUIElementRef window = new AXUIElementRef();
			window.setPointer(windowp.getValue());
			moveWindow(window, (int) point.x, (int) point.y);
		}

		carbon.CFRelease(application);
	}

	private static void restoreWindowsNotIn(final Rectangle rect) {
		Rectangle visibleArea = getWindowVisibleArea();
		for (long pid : getTouchedProcesses()) {
			AXUIElementRef application =
				carbon.AXUIElementCreateApplication(pid);

			for (AXUIElementRef window : getWindowsOf(application)) {
				carbon.CFRetain(window);
				Rectangle windowRect = getRectOfWindow(window);
				if (!visibleArea.intersects(windowRect)) {
					moveWindow(window, 0, 0);
				}
				carbon.CFRelease(window);
			}

			carbon.CFRelease(application);
		}
	}

	private static ArrayList<AXUIElementRef> getWindowsOf(AXUIElementRef application) {
		PointerByReference axWindowsp = new PointerByReference();
		ArrayList<AXUIElementRef> ret = new ArrayList<AXUIElementRef>();

		carbon.AXUIElementCopyAttributeValue(application, kAXChildren, axWindowsp);

		if (axWindowsp.getValue() == Pointer.NULL) {
			return ret;
		}

		CFArrayRef cfWindows = new CFArrayRef();
		cfWindows.setPointer(axWindowsp.getValue());

		for (long i = 0, l = carbon.CFArrayGetCount(cfWindows); i < l; ++i) {
			Pointer p = carbon.CFArrayGetValueAtIndex(cfWindows, i);
			AXUIElementRef el = new AXUIElementRef();
			el.setPointer(p);
			ret.add(el);
		}

		return ret;
	}

	private static Rectangle getRectOfWindow(AXUIElementRef window) {
		CGPoint pos = getPositionOfWindow(window);
		CGSize size = getSizeOfWindow(window);
		return new Rectangle(pos.getX(), pos.getY(), size.getWidth(), size.getHeight());
	}

	private static void moveWindow(AXUIElementRef window, int x, int y) {
		CGPoint position = new CGPoint((double) x, (double) y);
		position.write();
		AXValueRef axvalue = carbon.AXValueCreate(
			carbon.kAXValueCGPointType, position.getPointer());
		carbon.AXUIElementSetAttributeValue(window, kAXPosition, axvalue);
	}

	private static CFStringRef createCFString(String s) {
		return carbon.CFStringCreateWithCharacters(null, s.toCharArray(), s.length());
	}

	/**
		 min < max のとき、
		 min <= x <= max ならば x を返す
		 x < min ならば min を返す
		 x > max ならば max を返す
	 */
	private static double betweenOrLimit(double x, double min, double max) {
		return Math.min(Math.max(x, min), max);
	}

	/**
		画面内でウィンドウを移動しても押し返されない範囲を Rectangle で返す。
		Mac では、ウィンドウを完全に画面外に移動させようとすると、
		ウィンドウが画面内に押し返されてしまう。
	 */
	private static Rectangle getWindowVisibleArea() {
		final int menuBarHeight = 22;
		int x = 1, y = menuBarHeight,
			width = getScreenWidth() - 2, // 0-origin なので
			height = getScreenHeight() - menuBarHeight;

		refreshDockState();
		final String orientation = getDockOrientation();
		final int tilesize = getDockTileSize();

		if ("bottom".equals(orientation)) {
			height -= tilesize;
		} else if ("right".equals(orientation)) {
			width -= tilesize;
		}	else if ("left".equals(orientation)) {
			x += tilesize;
			width -= tilesize;
		}	else /* if ("null".equals(orientation)) */ {
			// Dock の方向がわからないので、どちらにあってもいいようにする
			x += tilesize;
			width -= 2 * tilesize;
		}

		Rectangle r = new Rectangle(x, y, width, height);
		return r;
	}

	private static String getDockOrientation() {
		CFTypeRef orientationRef =
			carbon.CFPreferencesCopyValue(
				kOrientation, kDock, carbon.kCurrentUser, carbon.kAnyHost);

		// CFPreferencesCopyValue が null を返す環境がある
		if (orientationRef == null) {
			return "null";
		}

		final int bufsize = 64;
		Memory buf = new Memory(64);
		carbon.CFStringGetCString(
			orientationRef, buf, bufsize, carbon.CFStringGetSystemEncoding());
		carbon.CFRelease(orientationRef);
		String ret = buf.getString(0);
		buf.clear();
		return ret;
	}

	private static int getDockTileSize() {
		/**
			 Dock の高さを監視する効率的な方法が見当たらないため、
			 ひとまず Dock の最大サイズより大きい定数を返しておく。

			 CFPreferencesCopyValue で得られる値は、
			 AppleScript で得られる値とは異なっていて、
			 AppleScript のほうが正しい値。

			 pid 取得して Accessibility API を使うと正しい値は取れるが、
			 killall Dock されると SEGV してしまう。
			 SEGV しないためには毎回 pid を取り直す必要があるが、
			 プロセスのリストをたぐってさがす以外の方法が見当たらない。
			 呼ばれる頻度を考えると AppleScript は使いたくない。
			 このトレードオフはあとで考えることにする。
		 */
		return 100;
	}

	private static void refreshDockState() {
		carbon.CFPreferencesAppSynchronize(kDock);
	}

	private static long getCurrentPID() {
		return currentPID;
	}

	private static void setCurrentPID(long newPID) {
		if (newPID != myPID) {
			currentPID = newPID;
			getTouchedProcesses().add(newPID);
		}
	}

	private static HashSet<Long> getTouchedProcesses() {
		return touchedProcesses;
	}

  private void updateFrontmostWindow() {
    final Rectangle
			frontmostWindowRect = getFrontmostAppRect(),
			windowVisibleArea = getWindowVisibleArea();

    frontmostWindow.setVisible(
      (frontmostWindowRect != null)
      && frontmostWindowRect.intersects(windowVisibleArea)
			&& !frontmostWindowRect.contains(windowVisibleArea) // デスクトップを除外
			);
    frontmostWindow.set(
      frontmostWindowRect == null ? new Rectangle(-1, -1, 0, 0) : frontmostWindowRect);
  }

	private static void updateFrontmostApp() {
		long newPID = getFrontmostAppsPID();
		setCurrentPID(newPID);
	}

	@Override
	public void tick() {
		super.tick();
		this.updateFrontmostApp();
    this.updateFrontmostWindow();
	}

	@Override
	public void moveActiveIE(final Point point) {
		/**
			前述のとおり、完全に画面外へ移動しようとすると押し返されるため、
			そのような位置の指定に対しては可能なかぎりの移動に切り替える。
		 */
		final Rectangle
			visibleRect = getWindowVisibleArea(),
			windowRect  = getFrontmostAppRect();

		final double
			minX = visibleRect.getMinX() - windowRect.getWidth(), // 左方向の折り返し座標
			maxX = visibleRect.getMaxX(),													// 右方向の折り返し座標
			minY = visibleRect.getMinY(),													// 上方向の折り返し座標
																														// (メニューバーより
																														//  上へは移動できない)
			maxY = visibleRect.getMaxY();													// 下方向の折り返し座標

		double
			pX   = point.getX(),
			pY   = point.getY();

		// X方向の折り返し
		pX = betweenOrLimit(pX, minX, maxX);

		// Y方向の折り返し
		pY = betweenOrLimit(pY, minY, maxY);

		point.setLocation(pX, pY);
		moveFrontmostWindow(point);
	}

	@Override
	public void restoreIE() {
		final Rectangle visibleRect = getWindowVisibleArea();
		restoreWindowsNotIn(visibleRect);
		getTouchedProcesses().clear();
	}

	@Override
	public Area getWorkArea() {
		return getScreen();
	}

	@Override
	public Area getActiveIE() {
		return this.activeIE;
	}
	
	@Override
	public String getActiveIETitle() {
		//  Mac 上不实现获取IE窗口标题的方法
		return "";
	}

    @Override
    public void refreshCache()
    {
    }
    
    @Override
    public void dispose()
    {
        // No resources to dispose for Mac environment
    }
}
