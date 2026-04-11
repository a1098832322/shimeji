package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
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
 * by Kilkakon
 * kilkakon.com
 */
public class ScanInteract extends BorderedAction
{
    private static final Logger log = LoggerFactory.getLogger( ScanInteract.class.getName( ) );
    
    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";
    
    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";
    
    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;
    
    private WeakReference<Mascot> target;
    
    private boolean turning = false;
    
    private Boolean hasTurning = null;

    public ScanInteract( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap context )
    {
        super( schema, animations, context );
    }

    @Override
    public void init( final Mascot mascot ) throws VariableException
    {
        super.init( mascot );
        
        // cannot broadcast while scanning for an affordance
        getMascot( ).getAffordances( ).clear( );
        
        putVariable( getSchema( ).getString( "TargetX" ), null );
        putVariable( getSchema( ).getString( "TargetY" ), null );
    }
    
    @Override
    public boolean hasNext( ) throws VariableException
    {
        final boolean intime = getTime( ) < getAnimation( ).getDuration( );

        return super.hasNext( ) && turning || intime;
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        super.tick( );
        
        // cannot broadcast while scanning for an affordance
        getMascot( ).getAffordances( ).clear( );

        if( ( getBorder( ) != null ) && !getBorder( ).isOn( getMascot( ).getAnchor( ) ) )
        {
            throw new LostGroundException( );
        }
        
        // refresh target
        if( getMascot( ).getManager( ) != null && ( target == null || target.get( ) == null || !target.get( ).getAffordances( ).contains( getAffordance( ) ) ) )
            target = getMascot( ).getManager( ).getMascotWithAffordance( getAffordance( ) );
        
        putVariable( getSchema( ).getString( "TargetX" ), target != null && target.get( ) != null ? target.get( ).getAnchor( ).x : null );
        putVariable( getSchema( ).getString( "TargetY" ), target != null && target.get( ) != null ? target.get( ).getAnchor( ).y : null );
        
        if( target != null && target.get( ) != null && target.get( ).getAffordances( ).contains( getAffordance( ) ) )
        {
            if( getMascot( ).getAnchor( ).x != target.get( ).getAnchor( ).x )
            {
                // activate turn animation if we change directions
                turning = hasTurningAnimation( ) && ( turning || getMascot( ).getAnchor( ).x < target.get( ).getAnchor( ).x != getMascot( ).isLookRight( ) );
                getMascot( ).setLookRight( getMascot( ).getAnchor( ).x < target.get( ).getAnchor( ).x );
            }
        
            // check if turning animation has finished
            if( turning && getTime( ) >= getAnimation( ).getDuration( ) )
            {
                setTime( getTime( ) - getAnimation( ).getDuration( ) );
                turning = false;
            }
        
            getAnimation( ).next( getMascot( ), getTime( ) );

            if( !turning && ( getTime( ) == getAnimation( ).getDuration( ) - 1 || getAnimation( ).getDuration( ) == 1 ) && !getBehavior( ).trim( ).isEmpty( ) )
            {
                try
                {
                    getMascot( ).setBehavior( Main.getInstance( ).getConfiguration( getMascot( ).getImageSet( ) ).buildBehavior( getBehavior( ), getMascot( ) ) );
                    if( !getTargetBehavior( ).trim( ).isEmpty( ) )
                    {
                        target.get( ).setBehavior( Main.getInstance( ).getConfiguration( target.get( ).getImageSet( ) ).buildBehavior( getTargetBehavior( ), target.get( ) ) );
                    }
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
    }
    
    @Override
    protected Animation getAnimation( ) throws VariableException
    {
        List<Animation> animations = super.getAnimations( );
        for( int index = 0; index < animations.size( ); index++ )
        {
            if( animations.get( index ).isEffective( getVariables( ) ) && 
                turning == animations.get( index ).isTurn( ) )
            {
                return animations.get( index );
            }
        }

        return null;
    }
    
    protected boolean hasTurningAnimation( )
    {
        if( hasTurning == null )
        {
            hasTurning = false;
            List<Animation> animations = super.getAnimations( );
            for( int index = 0; index < animations.size( ); index++ )
            {
                if( animations.get( index ).isTurn( ) )
                {
                    hasTurning = true;
                    index = animations.size( );
                }
            }
        }
        return hasTurning;
    }
    
    protected boolean isTurning( )
    {
        return turning;
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
}
