package com.group_finity.mascot.animation;

import java.awt.Point;
import java.nio.file.Path;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairs;

/**
 * Original Author: Yuki Yamada of Group Finity
 * (http://www.group-finity.com/Shimeji/) Currently developed by Shimeji-ee
 * Group.
 */
public class Pose
{
    private final Path image;
    private final Path rightImage;
    private final int dx;
    private final int dy;
    private final int duration;
    private final String sound;

    public Pose( final Path image )
    {
        this( image, null, 0, 0, 1 );
    }

    public Pose( final Path image, final int duration )
    {
        this( image, null, 0, 0, duration );
    }

    public Pose( final Path image, final int dx, final int dy, final int duration )
    {
        this( image, null, dx, dy, duration );
    }

    public Pose( final Path image, final Path rightImage )
    {
        this( image, rightImage, 0, 0, 1 );
    }

    public Pose( final Path image, final Path rightImage, final int duration )
    {
        this( image, rightImage, 0, 0, duration );
    }

    public Pose( final Path image, final Path rightImage, final int dx, final int dy, final int duration )
    {
        this( image, rightImage, dx, dy, duration, null );
    }

    public Pose( final Path image, final Path rightImage, final int dx, final int dy, final int duration, final String sound )
    {
        this.image = image;
        this.rightImage = rightImage;
        this.dx = dx;
        this.dy = dy;
        this.duration = duration;
        this.sound = sound;
    }

    @Override
    public String toString( )
    {
        return "Pose (" + ( getImage( ) == null ? "" : getImage( ) ) + "," + getDx( ) + "," + getDy( ) + "," + getDuration( ) + ", " + sound + ")";
    }

    public void next( final Mascot mascot )
    {
        mascot.setAnchor( new Point( mascot.getAnchor( ).x + ( mascot.isLookRight( ) ? -getDx( ) : getDx( ) ),
                          mascot.getAnchor( ).y + getDy( ) ) );
        mascot.setImage( ImagePairs.getImage( getImageName( ), mascot.isLookRight( ) ) );
        mascot.setSound( getSoundName( ) );
    }

    public int getDuration( )
    {
        return duration;
    }

    public String getImageName( )
    {
        return ( image == null ? "" : image.toString( ) ) + ( rightImage == null ? "" : rightImage.toString( ) );
    }

    public ImagePair getImage( )
    {
        return ImagePairs.getImagePair( this.getImageName( ) );
    }

    public int getDx( )
    {
        return dx;
    }

    public int getDy( )
    {
        return dy;
    }

    public String getSoundName( )
    {
        return sound;
    }
}