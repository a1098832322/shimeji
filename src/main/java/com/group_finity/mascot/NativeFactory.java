package com.group_finity.mascot;

import java.awt.image.BufferedImage;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Platform;

public abstract class NativeFactory
{
    private static NativeFactory instance;

    static
    {
        resetInstance( );
    }

    public static NativeFactory getInstance( )
    {
        return instance;
    }
    
    public static void resetInstance( )
    {
        String subpkg = Main.getInstance( ).getProperties( ).getProperty( "Environment", "generic" );

        if( subpkg.equals( "generic" ) )
        {
            if( Platform.isWindows( ) )
            {
                subpkg = "win";
            }
            else if( Platform.isMac( ) )
            {
                subpkg = "mac";
            }
        }

        String basepkg = NativeFactory.class.getName( );
        // Remove a class name
        basepkg = basepkg.substring( 0, basepkg.lastIndexOf( '.' ) );

        try
        {
            @SuppressWarnings( "unchecked" )
            final Class<? extends NativeFactory> impl = (Class<? extends NativeFactory>)Class.forName( basepkg + "." + subpkg + ".NativeFactoryImpl" );

            instance = impl.newInstance( );
        }
        catch( final ClassNotFoundException e )
        {
            throw new RuntimeException( e );
        }
        catch( final InstantiationException e )
        {
            throw new RuntimeException( e );
        }
        catch( final IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }

    public abstract Environment getEnvironment( );

    public abstract NativeImage newNativeImage( BufferedImage src );

    public abstract TranslucentWindow newTransparentWindow( );
}
