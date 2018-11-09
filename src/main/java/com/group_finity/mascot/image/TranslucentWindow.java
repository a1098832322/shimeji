package com.group_finity.mascot.image;

import javax.swing.JWindow;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public interface TranslucentWindow {

	public JWindow asJWindow();

	public void setImage(NativeImage image);

	public void updateImage();
    
    public void setStayOnTop( boolean alwaysOnTop );
}
