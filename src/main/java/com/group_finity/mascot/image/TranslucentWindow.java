package com.group_finity.mascot.image;

import java.awt.Component;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public interface TranslucentWindow
{
    public Component asComponent( );

    public void setImage( NativeImage image );

    public void updateImage( );
    
    public void dispose( );
    
    public void setAlwaysOnTop( boolean onTop );
}
