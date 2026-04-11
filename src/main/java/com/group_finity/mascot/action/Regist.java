package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
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
public class Regist extends ActionBase
{
    public static final String PARAMETER_OFFSETX = "OffsetX";

    private static final int DEFAULT_OFFSETX = 0;

    public static final String PARAMETER_OFFSETTYPE = "OffsetType";

    private static final String DEFAULT_OFFSETTYPE = "ImageAnchor";

    private double scaling;

    private static final Logger log = LoggerFactory.getLogger( Regist.class.getName( ) );

    public Regist( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap context )
    {
        super( schema, animations, context );
    }

    @Override
    public void init( final Mascot mascot ) throws VariableException
    {
        super.init( mascot );

        scaling = Double.parseDouble( Main.getInstance( ).getProperties( ).getProperty( "Scaling", "1.0" ) );
    }

    @Override
    public boolean hasNext( ) throws VariableException
    {
        int offsetX = (int)Math.round( getOffsetX( ) * scaling );
        if( getOffsetType( ).equals( getSchema( ).getString( "Origin" ) ) )
        {
            offsetX = 0 - offsetX + getMascot( ).getImage( ).getCenter( ).x;
        }
        
        final boolean notMoved = Math.abs( getEnvironment( ).getCursor( ).getX( ) - getMascot( ).getAnchor( ).x + offsetX ) < 5;

        return super.hasNext( ) && notMoved;
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        getMascot( ).setDragging( true );

        final Animation animation = getAnimation( );
        animation.next( getMascot( ), getTime( ) );

        if( getTime( ) + 1 >= getAnimation( ).getDuration( ) )
        {
            getMascot( ).setLookRight( Math.random( ) < 0.5 );
            
            throw new LostGroundException( );
        }
    }

    @Override
    protected void refreshHotspots( )
    {
        // action does not support hotspots
        getMascot( ).getHotspots( ).clear( );
    }

    private int getOffsetX( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_OFFSETX ), Number.class, DEFAULT_OFFSETX ).intValue( );
    }

    private String getOffsetType( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_OFFSETTYPE ), String.class, DEFAULT_OFFSETTYPE );
    }
}
