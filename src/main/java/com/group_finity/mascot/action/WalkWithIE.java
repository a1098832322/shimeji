package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import java.awt.Point;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class WalkWithIE extends Move
{
    private static final Logger log = LoggerFactory.getLogger( WalkWithIE.class.getName( ) );

    public static final String PARAMETER_IEOFFSETX = "IeOffsetX";

    private static final int DEFAULT_IEOFFSETX = 0;

    public static final String PARAMETER_IEOFFSETY = "IeOffsetY";

    private static final int DEFAULT_IEOFFSETY = 0;

    public WalkWithIE( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap params )
    {
        super( schema, animations, params );
    }

    @Override
    public boolean hasNext( ) throws VariableException
    {
        if( !Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Throwing", "true" ) ) )
            return false;

        return super.hasNext( );
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        final Area activeIE = getEnvironment( ).getActiveIE( );
        if( !activeIE.isVisible( ) )
        {
            throw new LostGroundException( );
        }

        final int offsetX = getIEOffsetX( );
        final int offsetY = getIEOffsetY( );

        if( getMascot( ).isLookRight( ) )
        {
            if( ( getMascot( ).getAnchor( ).x - offsetX != activeIE.getLeft( ) ) || 
                ( getMascot( ).getAnchor( ).y + offsetY != activeIE.getBottom( ) ) )
            {
                throw new LostGroundException( );
            }
        }
        else
        {
            if( ( getMascot( ).getAnchor( ).x + offsetX != activeIE.getRight( ) ) || 
                ( getMascot( ).getAnchor( ).y + offsetY != activeIE.getBottom( ) ) )
            {
                throw new LostGroundException( );
            }
        }

        super.tick( );

        if( activeIE.isVisible( ) )
        {
            if( getMascot( ).isLookRight( ) )
            {
                getEnvironment( ).moveActiveIE( new Point( getMascot( ).getAnchor( ).x - offsetX, 
                                                           getMascot( ).getAnchor( ).y + offsetY - activeIE.getHeight( ) ) );
            }
            else
            {
                getEnvironment( ).moveActiveIE( new Point( getMascot( ).getAnchor( ).x + offsetX - activeIE.getWidth( ),
                                                           getMascot( ).getAnchor( ).y + offsetY - activeIE.getHeight( ) ) );
            }
        }
    }

    private int getIEOffsetX( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_IEOFFSETX ), Number.class, DEFAULT_IEOFFSETX ).intValue( );
    }

    private int getIEOffsetY( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_IEOFFSETY ), Number.class, DEFAULT_IEOFFSETY ).intValue( );
    }
}
