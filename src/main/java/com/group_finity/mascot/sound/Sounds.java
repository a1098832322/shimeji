package com.group_finity.mascot.sound;

import com.group_finity.mascot.Main;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.Clip;

/**
 * This static class contains all the sounds loaded by Shimeji-ee.
 * 
 * Visit kilkakon.com/shimeji for updates
 * @author Kilkakon
 */
public class Sounds
{
    private final static ConcurrentHashMap<String,Clip> SOUNDS = new ConcurrentHashMap<String,Clip>( );

    public static void load( final String filename, final Clip clip )
    {
        if( !SOUNDS.containsKey( filename ) )
            SOUNDS.put( filename, clip );
    }

    public static boolean contains( String filename )
    {
        return SOUNDS.containsKey( filename );
    }

    public static Clip getSound( String filename )
    {
        if( !SOUNDS.containsKey( filename ) )
            return null;
        return SOUNDS.get( filename );
    }

    public static ArrayList<Clip> getSoundsIgnoringVolume( String filename )
    {
        ArrayList<Clip> sounds = new ArrayList( 5 );
        Enumeration<String> keys = SOUNDS.keys( );
        while( keys.hasMoreElements( ) )
        {
            String soundName = keys.nextElement( );
            if( soundName.startsWith( filename ) )
            {
                sounds.add( SOUNDS.get( soundName ) );
            }
        }
        return sounds;
    }
    
    public static boolean isMuted( )
    {
        return ! Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Sounds", "true" ) );
    }
    
    public static void setMuted( boolean mutedFlag )
    {
        if( mutedFlag )
        {
            // mute everything
            Enumeration<String> keys = SOUNDS.keys( );
            while( keys.hasMoreElements( ) )
            {
                SOUNDS.get( keys.nextElement( ) ).stop( );
            }
        }
    }
}
