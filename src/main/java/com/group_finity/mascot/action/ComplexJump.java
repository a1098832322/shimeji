package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * @author Kilkakon
 */
public class ComplexJump extends ActionBase
{
    private static final Logger log = LoggerFactory.getLogger( ComplexJump.class.getName( ) );
    
    private final Breed.Delegate delegate = new Breed.Delegate( this );
    
    public static final String PARAMETER_CHARACTERISTICS = "Characteristics";

    private static final String DEFAULT_CHARACTERISTICS = "";
    
    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";
    
    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";
    
    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    //A Pose Attribute is already named Velocity
    public static final String PARAMETER_VELOCITY = "VelocityParam";

    private static final double DEFAULT_VELOCITY = 20.0;

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = 0;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = 0;

    public static final String VARIABLE_VELOCITYX = "VelocityX";

    public static final String VARIABLE_VELOCITYY = "VelocityY";
    
    private WeakReference<Mascot> target;
    
    private boolean breedEnabled = false;
    
    private boolean scanEnabled = false;
    
    public ComplexJump( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap params )
    {
	super( schema, animations, params );
    }

    @Override
    public void init( final Mascot mascot ) throws VariableException
    {
        super.init( mascot );
        
        for( String characteristic : getCharacteristics( ).split( "," ) )
        {
            if( characteristic.equals( getSchema( ).getString( "Breed" ) ) )
                breedEnabled = true;
            if( characteristic.equals( getSchema( ).getString( "Scan" ) ) )
                scanEnabled = true;
        }
        
        if( breedEnabled )
        {
            delegate.validateBornCount( );
            delegate.validateBornInterval( );
        }
        if( scanEnabled )
        {
            // cannot broadcast while scanning for an affordance
            getMascot( ).getAffordances( ).clear( );

            if( getMascot( ).getManager( ) != null )
                target = getMascot( ).getManager( ).getMascotWithAffordance( getAffordance( ) );
            putVariable( getSchema( ).getString( "TargetX" ), target != null && target.get( ) != null ? target.get( ).getAnchor( ).x : null );
            putVariable( getSchema( ).getString( "TargetY" ), target != null && target.get( ) != null ? target.get( ).getAnchor( ).y : null );
        }
    }

    @Override
    public boolean hasNext( ) throws VariableException
    {
        if( scanEnabled )
        {
            if( getMascot( ).getManager( ) == null )
                return super.hasNext( );

            return super.hasNext( ) && target != null && target.get( ) != null && target.get( ).getAffordances( ).contains( getAffordance( ) );
        }
        else
        {
            final int targetX = getTargetX( );
            final int targetY = getTargetY( );

            final double distanceX = targetX - getMascot( ).getAnchor( ).x;
            final double distanceY = targetY - getMascot( ).getAnchor( ).y - Math.abs( distanceX ) / 2;

            final double distance = Math.sqrt( distanceX * distanceX + distanceY * distanceY );

            return super.hasNext( ) && ( distance != 0 );
        }
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        int targetX = 0;
        int targetY = 0;
        
        if( scanEnabled )
        {
            // cannot broadcast while scanning for an affordance
            getMascot( ).getAffordances( ).clear( );

            targetX = target.get( ).getAnchor( ).x;
            targetY = target.get( ).getAnchor( ).y;
        
            putVariable( getSchema( ).getString( "TargetX" ), targetX );
            putVariable( getSchema( ).getString( "TargetY" ), targetY );

            if( getMascot( ).getAnchor( ).x != targetX )
            {
                getMascot( ).setLookRight( getMascot( ).getAnchor( ).x < targetX );
            }
        }
        else
        {
            targetX = getTargetX( );
            targetY = getTargetY( );

            getMascot( ).setLookRight( getMascot( ).getAnchor( ).x < targetX );
        }

        double distanceX = targetX - getMascot( ).getAnchor( ).x;
        double distanceY = targetY - getMascot( ).getAnchor( ).y - Math.abs( distanceX )/2;

        double distance = Math.sqrt( distanceX * distanceX + distanceY * distanceY );

        double velocity = getVelocity( );
		
        if( distance != 0 )
        {
            int velocityX = (int)( velocity * distanceX / distance );
            int velocityY = (int)( velocity * distanceY / distance );
            
            putVariable( getSchema( ).getString( VARIABLE_VELOCITYX ), velocity * distanceX / distance );
            putVariable( getSchema( ).getString( VARIABLE_VELOCITYY ), velocity * distanceY / distance );

            getMascot( ).setAnchor( new Point( getMascot( ).getAnchor( ).x + velocityX, 
                                               getMascot( ).getAnchor( ).y + velocityY ) );
            getAnimation( ).next( getMascot( ), getTime( ) );
        }

        if( distance <= velocity )
        {
            getMascot( ).setAnchor( new Point( targetX, targetY ) );
            
            if( scanEnabled )
            {
                try
                {
                    getMascot( ).setBehavior( Main.getInstance( ).getConfiguration( getMascot( ).getImageSet( ) ).buildBehavior( getBehavior( ), getMascot( ) ) );
                    target.get( ).setBehavior( Main.getInstance( ).getConfiguration( target.get( ).getImageSet( ) ).buildBehavior( getTargetBehavior( ), target.get( ) ) );
                    if( getTargetLook( ) && target.get( ).isLookRight( ) == getMascot( ).isLookRight( ) )
                    {
                        target.get( ).setLookRight( !getMascot( ).isLookRight( ) );
                    }
                }
                catch( final NullPointerException e )
                {
                    Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
                }
                catch( final CantBeAliveException e )
                {
                    Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
                }
            }
        }
        
        if( breedEnabled && delegate.isIntervalFrame( ) && delegate.isEnabled( ) )
            delegate.breed( );
    }

    private String getCharacteristics( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_CHARACTERISTICS ), String.class, DEFAULT_CHARACTERISTICS );
    }

    private String getBehavior( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BEHAVIOUR ), String.class, DEFAULT_BEHAVIOUR );
    }

    private String getTargetBehavior( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_TARGETBEHAVIOUR ), String.class, DEFAULT_TARGETBEHAVIOUR );
    }

    private boolean getTargetLook( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_TARGETLOOK ), Boolean.class, DEFAULT_TARGETLOOK );
    }
    
    private double getVelocity( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_VELOCITY ), Number.class, DEFAULT_VELOCITY ).doubleValue( );
    }

    private int getTargetX( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_TARGETX ), Number.class, DEFAULT_TARGETX ).intValue( );
    }

    private int getTargetY( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_TARGETY ), Number.class, DEFAULT_TARGETY ).intValue( );
    }
}
