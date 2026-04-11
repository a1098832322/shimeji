package com.group_finity.mascot.action;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity
 * (http://www.group-finity.com/Shimeji/) Currently developed by Shimeji-ee
 * Group.
 */
public class Transform extends Animate
{
    private static final Logger log = LoggerFactory.getLogger( Transform.class.getName( ) );
    
    public static final String PARAMETER_TRANSFORMBEHAVIOUR = "TransformBehaviour";

    private static final String DEFAULT_TRANSFORMBEHAVIOUR = "";

    public static final String PARAMETER_TRANSFORMMASCOT = "TransformMascot";

    private static final String DEFAULT_TRANSFORMMASCOT = "";

    public Transform( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap params )
    {
        super( schema, animations, params );
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        super.tick( );

        if( ( getTime( ) == getAnimation( ).getDuration( ) - 1 || getAnimation( ).getDuration( ) == 1 ) && Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Transformation", "true" ) ) )
        {
            transform( );
        }
    }

    private void transform( ) throws VariableException
    {
        String childType = Main.getInstance( ).getConfiguration( getTransformMascot( ) ) != null ? getTransformMascot( ) : getMascot( ).getImageSet( );
        
        try
        {
            getMascot( ).setImageSet( childType );
            getMascot( ).setBehavior( Main.getInstance( ).getConfiguration( childType ).buildBehavior( getTransformBehavior( ), getMascot( ) ) );
        }
        catch( final BehaviorInstantiationException e )
        {
            Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedCreateNewShimejiErrorMessage" ) );
        }
        catch( final CantBeAliveException e )
        {
            Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedCreateNewShimejiErrorMessage" ) );
        }
    }

    private String getTransformBehavior( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_TRANSFORMBEHAVIOUR ), String.class, DEFAULT_TRANSFORMBEHAVIOUR );
    }

    private String getTransformMascot( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_TRANSFORMMASCOT ), String.class, DEFAULT_TRANSFORMMASCOT );
    }
}
