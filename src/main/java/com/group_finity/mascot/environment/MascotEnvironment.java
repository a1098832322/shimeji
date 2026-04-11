package com.group_finity.mascot.environment;

import com.group_finity.mascot.Main;
import java.awt.Point;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.NativeFactory;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class MascotEnvironment
{
    private final Environment impl;

    private final Mascot mascot;

    private Area currentWorkArea;

    public MascotEnvironment( Mascot mascot )
    {
        this.mascot = mascot;
        impl = NativeFactory.getInstance( ).getEnvironment( );
        impl.init( );
    }

    public Area getWorkArea( )
    {
        return getWorkArea( false );
    }

    public Area getWorkArea( Boolean ignoreSettings )
    {
        if ( currentWorkArea != null )
        {
            if( ignoreSettings || ( Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Multiscreen", "true" ) ) ) )
            {
                // NOTE Windows
                if( currentWorkArea != impl.getWorkArea( ) && currentWorkArea.toRectangle( ).contains( impl.getWorkArea( ).toRectangle( ) ) )
                {
                    if( impl.getWorkArea( ).contains( mascot.getAnchor( ).x, mascot.getAnchor( ).y ) )
                    {
                        currentWorkArea = impl.getWorkArea( );
                        return currentWorkArea;
                    }
                }

                // NOTE
                if ( currentWorkArea.contains( mascot.getAnchor( ).x, mascot.getAnchor( ).y ) ) 
                {
                    return currentWorkArea;
                }
            }
            else return currentWorkArea;
        }

        if( impl.getWorkArea( ).contains( mascot.getAnchor( ).x, mascot.getAnchor( ).y ) )
        {
            currentWorkArea = impl.getWorkArea( );
            return currentWorkArea;
        }

        for( Area area: impl.getScreens( ) )
        {
            if ( area.contains( mascot.getAnchor( ).x, mascot.getAnchor( ).y ) )
            {
                currentWorkArea = area;
                return currentWorkArea;
            }
        }

        currentWorkArea = impl.getWorkArea( );
        return currentWorkArea;
    }

    public Area getActiveIE( )
    {
        Area activeIE = impl.getActiveIE( );
        
        if( currentWorkArea != null && !Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Multiscreen", "true" ) ) && !currentWorkArea.toRectangle( ).intersects( activeIE.toRectangle( ) ) )
            return new Area( );
        
        return activeIE;
    }
    
    public String getActiveIETitle( )
    {
        return impl.getActiveIETitle( );
    }

    public Border getCeiling( )
    {
        return getCeiling( false );
    }

    public Border getCeiling( boolean ignoreSeparator )
    {
        if( getActiveIE( ).getBottomBorder( ).isOn( mascot.getAnchor( ) ) )
        {
            return getActiveIE( ).getBottomBorder( );
        }
        if( getWorkArea( ).getTopBorder( ).isOn( mascot.getAnchor( ) ) )
        {
            if ( !ignoreSeparator || isScreenTopBottom( ) )
            {
                    return getWorkArea( ).getTopBorder( );
            }
        }
        return NotOnBorder.INSTANCE;
    }

    public ComplexArea getComplexScreen( )
    {
        return impl.getComplexScreen( );
    }

    public Location getCursor( )
    {
        return impl.getCursor( );
    }

    public Border getFloor( )
    {
        return getFloor( false );
    }

    public Border getFloor( boolean ignoreSeparator )
    {
        if( getActiveIE( ).getTopBorder( ).isOn( mascot.getAnchor( ) ) )
        {
            return getActiveIE( ).getTopBorder( );
        }
        if( getWorkArea( ).getBottomBorder( ).isOn( mascot.getAnchor( ) ) )
        {
            if( !ignoreSeparator || isScreenTopBottom( ) )
            {
                return getWorkArea( ).getBottomBorder( );
            }
        }
        return NotOnBorder.INSTANCE;
    }

    public Area getScreen( )
    {
        return impl.getScreen( );
    }

    public Border getWall( )
    {
        return getWall( false );
    }

    public Border getWall( boolean ignoreSeparator )
    {
        if( mascot.isLookRight( ) )
        {
            if( getActiveIE( ).getLeftBorder( ).isOn( mascot.getAnchor( ) ) )
            {
                return getActiveIE( ).getLeftBorder( );
            }

            if( getWorkArea( ).getRightBorder( ).isOn( mascot.getAnchor( ) ) )
            {
                if( !ignoreSeparator || isScreenLeftRight( ) )
                {
                    return getWorkArea( ).getRightBorder( );
                }
            }
        }
        else
        {
            if( getActiveIE( ).getRightBorder( ).isOn( mascot.getAnchor( ) ) )
            {
                return getActiveIE( ).getRightBorder( );
            }

            if( getWorkArea( ).getLeftBorder( ).isOn( mascot.getAnchor( ) ) )
            {
                if( !ignoreSeparator || isScreenLeftRight( ) )
                {
                    return getWorkArea( ).getLeftBorder( );
                }
            }
        }

        return NotOnBorder.INSTANCE;
    }

    public void moveActiveIE( Point point )
    {
        impl.moveActiveIE( point );
    }

    public void restoreIE( )
    {
        impl.restoreIE( );
    }
    
    public void refreshWorkArea( )
    {
        getWorkArea( true );
    }

    private boolean isScreenTopBottom( )
    {
        return impl.isScreenTopBottom( mascot.getAnchor( ) );
    }

    private boolean isScreenLeftRight( )
    {
        return impl.isScreenLeftRight( mascot.getAnchor( ) );
    }
}
