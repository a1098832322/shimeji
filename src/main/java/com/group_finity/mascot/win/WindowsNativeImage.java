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
                Gdi32.INSTANCE.GetObjectW( nativeHandle, Main.getInstance( ).getPlatform( ).getBitmapSize( ) + Native.POINTER_SIZE, bmp );

		// Copy at the pixel level. These dimensions are already scaled
		int width = bmp.bmWidth;
		int height = bmp.bmHeight;
		final int destPitch = ((bmp.bmWidth*bmp.bmBitsPixel)+31)/32*4;
		int destIndex = destPitch*(height-1);
		int srcColIndex = 0;
                
		for( int y = 0; y < height; ++y )
		{
			for( int x = 0; x<width; ++x )
			{
                    
				// UpdateLayeredWindow and Photoshop are incompatible ?Irashii
				// UpdateLayeredWindow FFFFFF RGB value has the bug that it ignores the value of a,
				// Photoshop is where a is an RGB value of 0 have the property value to 0.

				bmp.bmBits.setInt(destIndex + x*4L,
					(rgb[srcColIndex]&0xFF000000)==0 ? 0 : rgb[srcColIndex] );

				++srcColIndex;
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

	public WindowsNativeImage(final BufferedImage image) {

	    this.managedImage = image;
            this.nativeHandle = createNative(image.getWidth(), image.getHeight());
            
            int[] rbgValues = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            
            flushNative(this.getNativeHandle(), rbgValues);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		freeNative(this.getNativeHandle());
	}

    /**
     * Changes to be reflected in the Windows bitmap image.
     */
    public void update( )
    {
        // this isn't used
    }

    public void flush( )
    {
        managedImage.flush( );
    }

    public Graphics getGraphics( )
    {
        return managedImage.createGraphics( );
    }

    public Pointer getHandle( )
    {
        return nativeHandle;
    }

    public int getHeight( )
    {
        return managedImage.getHeight( );
    }

    public int getWidth( )
    {
        return managedImage.getWidth( );
    }

    public int getHeight( final ImageObserver observer )
    {
        return managedImage.getHeight( observer );
    }

    public Object getProperty( final String name, final ImageObserver observer )
    {
        return managedImage.getProperty( name, observer );
    }

    public ImageProducer getSource( )
    {
        return managedImage.getSource( );
    }

    public int getWidth( final ImageObserver observer )
    {
        return managedImage.getWidth( observer );
    }

    private BufferedImage getManagedImage( )
    {
        return managedImage;
    }

    private Pointer getNativeHandle( )
    {
        return nativeHandle;
    }
}
