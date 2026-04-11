package com.group_finity.mascot.win;

import java.awt.Graphics;
import java.awt.Component;
import javax.swing.JWindow;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.win.jna.BLENDFUNCTION;
import com.group_finity.mascot.win.jna.Gdi32;
import com.group_finity.mascot.win.jna.POINT;
import com.group_finity.mascot.win.jna.RECT;
import com.group_finity.mascot.win.jna.SIZE;
import com.group_finity.mascot.win.jna.User32;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * The image window with alpha.
 * {@link #setImage(WindowsNativeImage)} set in {@link WindowsNativeImage} can be displayed on the desktop.
 * 
 * {@link #setAlpha(int)} may be specified when the concentration of view.
 *
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
class WindowsTranslucentWindow extends JWindow implements TranslucentWindow {

	private static final long serialVersionUID = 1L;
	
	@Override
	public Component asComponent() {
		return this;
	}

	/**
	 * Draw a picture with a value of alpha.
	 * @param imageHandle bitmap handle.
	 * @param alpha concentrations shown. 0 = not at all, 255 = full display.
	 */
	private void paint(final Pointer imageHandle, final int alpha) {
            
            //this.setSize( WIDTH, HEIGHT );
		
		final Pointer hWnd = Native.getComponentPointer(this);
		
		if ( User32.INSTANCE.IsWindow(hWnd)!=0 ) {
			
			final int exStyle = User32.INSTANCE.GetWindowLongW(hWnd, User32.GWL_EXSTYLE);
			if ( (exStyle&User32.WS_EX_LAYERED)==0 ) {
				User32.INSTANCE.SetWindowLongW(hWnd, User32.GWL_EXSTYLE, exStyle | User32.WS_EX_LAYERED);
			}

			// Create a DC source of the image
			final Pointer clientDC = User32.INSTANCE.GetDC(hWnd);
			final Pointer memDC = Gdi32.INSTANCE.CreateCompatibleDC(clientDC);
			final Pointer oldBmp = Gdi32.INSTANCE.SelectObject(memDC, imageHandle );
			
			User32.INSTANCE.ReleaseDC(hWnd, clientDC);

			// Destination Area
			final RECT windowRect = new RECT();
			User32.INSTANCE.GetWindowRect(hWnd, windowRect);

			// Forward
			final BLENDFUNCTION bf = new BLENDFUNCTION();
			bf.BlendOp = BLENDFUNCTION.AC_SRC_OVER;
			bf.BlendFlags = 0;
			bf.SourceConstantAlpha = (byte)alpha; // Level set
			bf.AlphaFormat = BLENDFUNCTION.AC_SRC_ALPHA;

			final POINT lt = new POINT();
			lt.x = windowRect.left;
			lt.y = windowRect.top;
			final SIZE size = new SIZE();
			size.cx = windowRect.Width();
			size.cy = windowRect.Height();
			final POINT zero = new POINT();
			User32.INSTANCE.UpdateLayeredWindow( 
					hWnd, Pointer.NULL, 
					lt, size,
					memDC, zero, 0, bf, User32.ULW_ALPHA );

			// Replace the bitmap you
			Gdi32.INSTANCE.SelectObject(memDC, oldBmp);
			Gdi32.INSTANCE.DeleteDC(memDC);

            // Bring to front
//            if( alwaysOnTop )
//            {
//                User32.INSTANCE.BringWindowToTop( hWnd );
//            }
            
		}
	}

	/**
	 * Image to display.
	 */
	private WindowsNativeImage image;

	/**
	 * The concentration shown. 0 = not at all, 255 = full display.
	 */
	private int alpha = 255;

	@Override
	public String toString() {
		return "LayeredWindow[hashCode="+hashCode()+",bounds="+getBounds()+"]";
	}
	
	@Override
	public void paint(final Graphics g) {
		if (getImage() != null) {
			// JNI with drawing images using the a value.
			paint(getImage().getHandle(), getAlpha());
		}
	}

	private WindowsNativeImage getImage() {
		return this.image;
	}

	public void setImage(final NativeImage image) {
		this.image = (WindowsNativeImage)image;
	}

	public int getAlpha() {
		return this.alpha;
	}

	public void setAlpha(final int alpha) {
		this.alpha = alpha;
	}

        @Override
	public void updateImage() {
		repaint();
	}
}