package com.group_finity.mascot.win;

import com.group_finity.mascot.Main;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.win.jna.BITMAP;
import com.group_finity.mascot.win.jna.BITMAPINFOHEADER;
import com.group_finity.mascot.win.jna.Gdi32;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * (@link WindowsTranslucentWindow) a value that can be used with images.
 * 
 * {@link WindowsTranslucentWindow} is available because only Windows bitmap
 * {@link BufferedImage} existing copy pixels from a Windows bitmap.
 * 
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
class WindowsNativeImage implements NativeImage {

	/**
	* Windows to create a bitmap.
	* @ Param width width of the bitmap.
	* @ Param height the height of the bitmap.
	* @ Return the handle of a bitmap that you create.
	 */
	private static Pointer createNative(final int width, final int height) {

		final BITMAPINFOHEADER bmi = new BITMAPINFOHEADER();
		bmi.biSize = 40;
		bmi.biWidth = width;
		bmi.biHeight = height;
		bmi.biPlanes = 1;
		bmi.biBitCount = 32;

		final Pointer hBitmap = Gdi32.INSTANCE.CreateDIBSection(
				Pointer.NULL, bmi, Gdi32.DIB_RGB_COLORS, Pointer.NULL, Pointer.NULL, 0 );

		return hBitmap;
	}

	/**
	 * {@link BufferedImage} to reflect the contents of the bitmap.
	 * @param nativeHandle bitmap handle.
	 * @param rgb ARGB of the picture.
	 */
	private static void flushNative(final Pointer nativeHandle, final int[] rgb) {

		final BITMAP bmp = new BITMAP();
		Gdi32.INSTANCE.GetObjectW(nativeHandle, Main.getInstance( ).getPlatform( ).getBitmapSize( ) + Native.POINTER_SIZE, bmp);

		// Copy at the pixel level.
		final int width = bmp.bmWidth;
		final int height = bmp.bmHeight;
		final int destPitch = ((bmp.bmWidth*bmp.bmBitsPixel)+31)/32*4;
		int destIndex = destPitch*(height-1);
		int srcIndex = 0;
        int premultipliedR, premultipliedG, premultipliedB, alpha;
		for( int y = 0; y < height; ++y )
		{
			for( int x = 0; x<width; ++x )
			{
				// UpdateLayeredWindow and Photoshop are incompatible ?Irashii
				// UpdateLayeredWindow FFFFFF RGB value has the bug that it ignores the value of a,
				// Photoshop is where a is an RGB value of 0 have the property value to 0.

				bmp.bmBits.setInt(destIndex + x*4,
					(rgb[srcIndex]&0xFF000000)==0 ? 0 : rgb[srcIndex] );

				++srcIndex;
			}

			destIndex -= destPitch;
		}

	}

	/**
	* Windows to open a bitmap.
	* @ Param nativeHandle bitmap handle.
	 */
	private static void freeNative(final Pointer nativeHandle) {
		Gdi32.INSTANCE.DeleteObject(nativeHandle);
	}

	/**
	 * Java Image object.
	 */
	private final BufferedImage managedImage;

	/**
	 * Windows Bittomappuhandoru.
	 */
	private final Pointer nativeHandle;

	/**
	 * ARGB buffer used to transfer value.
	 */
	private final int[] rgb;

	public WindowsNativeImage(final BufferedImage image) {
		this.managedImage = image;
		this.nativeHandle = createNative(this.getManagedImage().getWidth(), this.getManagedImage().getHeight());
		this.rgb = new int[this.getManagedImage().getWidth() * this.getManagedImage().getHeight()];
		update();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		freeNative(this.getNativeHandle());
	}

	/**
	 * Changes to be reflected in the Windows bitmap image.
	 */
	public void update() {

		this.getManagedImage().getRGB(0, 0, this.getManagedImage().getWidth(), this.getManagedImage().getHeight(), this.getRgb(), 0,
				this.getManagedImage().getWidth());

		flushNative(this.getNativeHandle(), this.getRgb());

	}

	public void flush() {
		this.getManagedImage().flush();
	}

	public Pointer getHandle() {
		return this.getNativeHandle();
	}

	public Graphics getGraphics() {
		return this.getManagedImage().createGraphics();
	}

	public int getHeight() {
		return this.getManagedImage().getHeight();
	}

	public int getWidth() {
		return this.getManagedImage().getWidth();
	}

	public int getHeight(final ImageObserver observer) {
		return this.getManagedImage().getHeight(observer);
	}

	public Object getProperty(final String name, final ImageObserver observer) {
		return this.getManagedImage().getProperty(name, observer);
	}

	public ImageProducer getSource() {
		return this.getManagedImage().getSource();
	}

	public int getWidth(final ImageObserver observer) {
		return this.getManagedImage().getWidth(observer);
	}

	private BufferedImage getManagedImage() {
		return this.managedImage;
	}

	private Pointer getNativeHandle() {
		return this.nativeHandle;
	}

	private int[] getRgb() {
		return this.rgb;
	}

}
