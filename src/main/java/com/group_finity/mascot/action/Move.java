package com.group_finity.mascot.action;

import java.awt.Point;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Move extends BorderedAction
{
    private static final Logger log = LoggerFactory.getLogger( Move.class.getName( ) );

    private static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

    private static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;
    
    protected boolean turning = false;
    
    private Boolean hasTurning = null;

    public Move( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap context )
    {
        super( schema, animations, context );
    }

    @Override
    public boolean hasNext( ) throws VariableException
    {
        final int targetX = getTargetX( );
        final int targetY = getTargetY( );

        boolean hasNotReached = ( targetX != Integer.MIN_VALUE && getMascot( ).getAnchor( ).x == targetX ) ||
                                ( targetY != Integer.MIN_VALUE && getMascot( ).getAnchor( ).y == targetY );

        return super.hasNext( ) && ( !hasNotReached || turning );
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        super.tick( );

        if( ( getBorder( ) != null ) && !getBorder( ).isOn( getMascot( ).getAnchor( ) ) )
        {
            throw new LostGroundException( );
        }

        int targetX = getTargetX( );
        int targetY = getTargetY( );

        boolean down = false;

        if( targetX != DEFAULT_TARGETX )
        {
            if( getMascot( ).getAnchor( ).x != targetX )
            {
                // activate turn animation if we change directions
                turning = hasTurningAnimation( ) && ( turning || getMascot( ).getAnchor( ).x < targetX != getMascot( ).isLookRight( ) );
                getMascot( ).setLookRight( getMascot( ).getAnchor( ).x < targetX );
            }
        }
        if( targetY != DEFAULT_TARGETY )
        {
            down = getMascot( ).getAnchor( ).y < targetY;
        }
        
        // check if turning animation has finished
        if( turning && getTime( ) >= getAnimation( ).getDuration( ) )
        {
            turning = false;
        }

        getAnimation( ).next( getMascot( ), getTime( ) );

        if( targetX != DEFAULT_TARGETX )
        {
            if( ( getMascot( ).isLookRight( ) && ( getMascot( ).getAnchor( ).x >= targetX ) ) || 
                ( !getMascot( ).isLookRight( ) && ( getMascot( ).getAnchor( ).x <= targetX ) ) )
            {
                getMascot( ).setAnchor( new Point( targetX, getMascot( ).getAnchor( ).y ) );
            }
        }
        if( targetY != DEFAULT_TARGETY )
        {
            if( ( down && ( getMascot( ).getAnchor( ).y >= targetY ) ) ||
                ( !down && ( getMascot( ).getAnchor( ).y <= targetY ) ) )
            {
                getMascot( ).setAnchor( new Point( getMascot( ).getAnchor( ).x, targetY ) );
            }
        }
    }
    
    @Override
    protected Animation getAnimation( ) throws VariableException
    {
        // had to expose both animations and varibles for this
        // is there a better way?
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
                    break;
                }
            }
        }
        return hasTurning;
    }
    
    protected boolean isTurning( )
    {
        return turning;
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
