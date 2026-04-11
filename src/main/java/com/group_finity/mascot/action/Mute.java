package com.group_finity.mascot.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import com.group_finity.mascot.sound.Sounds;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.sound.sampled.Clip;

/**
 * By Kilkakon
 * Go to kilkakon.com for all the best milkshakes and chocolate sundaes
 * Now I'm hungry
 */
public class Mute extends InstantAction
{
    private static final Logger log = LoggerFactory.getLogger( Offset.class.getName( ) );

    public static final String PARAMETER_SOUND = "Sound";

    private static final String DEFAULT_SOUND = null;

    public Mute( java.util.ResourceBundle schema, final VariableMap params )
    {
        super( schema, params );
    }

    @Override
    protected void apply( ) throws VariableException
    {
        String soundName = getSound( );
        if( soundName != null )
        {
            ArrayList<Clip> clips = Sounds.getSoundsIgnoringVolume( Paths.get( ".", "sound", soundName ).toString( ) );
            if( clips.size( ) > 0 )
            {
                for( Clip clip : clips )
                { 
                    if( clip != null && clip.isRunning( ) )
                        clip.stop( );
                }
            }
            else
            {
                clips = Sounds.getSoundsIgnoringVolume( Paths.get( ".", "sound", getMascot( ).getImageSet( ), soundName ).toString( ) );
                if( clips.size( ) > 0 )
                {
                    for( Clip clip : clips )
                    { 
                        if( clip != null && clip.isRunning( ) )
                            clip.stop( );
                    }
                }
                else
                {
                    clips = Sounds.getSoundsIgnoringVolume( Paths.get( ".", "img", getMascot( ).getImageSet( ), "sound", soundName ).toString( ) );
                    for( Clip clip : clips )
                    { 
                        if( clip != null && clip.isRunning( ) )
                            clip.stop( );
                    }
                }
            }
        }
        else
        {
            if( !Sounds.isMuted( ) )
            {
                Sounds.setMuted( true );
                Sounds.setMuted( false );
            }
        }
    }

    private String getSound( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_SOUND ), String.class, DEFAULT_SOUND );
    }
}