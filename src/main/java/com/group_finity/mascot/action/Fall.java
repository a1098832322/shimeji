package com.group_finity.mascot.action;

import java.awt.Point;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Fall extends ActionBase {

	private static final Logger log = LoggerFactory.getLogger(Fall.class);

	public static final String PARAMETER_INITIALVX = "InitialVX";

	private static final int DEFAULT_INITIALVX = 0;

	private static final String PARAMETER_INITIALVY = "InitialVY";

	private static final int DEFAULT_INITIALVY = 0;

	public static final String PARAMETER_RESISTANCEX = "ResistanceX";

	private static final double DEFAULT_RESISTANCEX = 0.05;

	public static final String PARAMETER_RESISTANCEY = "ResistanceY";

	private static final double DEFAULT_RESISTANCEY = 0.1;

	public static final String PARAMETER_GRAVITY = "Gravity";

	private static final double DEFAULT_GRAVITY = 2;

	public static final String VARIABLE_VELOCITYX = "VelocityX";

	public static final String VARIABLE_VELOCITYY = "VelocityY";

	private double velocityX;

	private double velocityY;

	private double modX;

	private double modY;

	public Fall( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap context )
        {
            super( schema, animations, context );
	}

	@Override
	public void init(final Mascot mascot) throws VariableException {
		super.init(mascot);

		this.setVelocityX(getInitialVx());
		this.setVelocityY(getInitialVy());
	}

	@Override
	public boolean hasNext() throws VariableException {

		Point pos = getMascot().getAnchor();
		boolean onBorder = false;
		if ( getEnvironment().getFloor().isOn(pos) ) {
			onBorder = true;
		}
		if ( getEnvironment().getWall().isOn(pos) ) {
			onBorder = true;
		}
		return super.hasNext() && !onBorder;
	}

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        if( this.getVelocityX( ) != 0 )
        {
            getMascot( ).setLookRight( this.getVelocityX( ) > 0 );
        }

        this.setVelocityX( this.getVelocityX( ) - ( this.getVelocityX( ) * getResistanceX( ) ) );
        this.setVelocityY( this.getVelocityY( ) - ( this.getVelocityY( ) * getResistanceY( ) ) + getGravity( ) );

        putVariable( getSchema( ).getString( VARIABLE_VELOCITYX ), getVelocityX( ) );
        putVariable( getSchema( ).getString( VARIABLE_VELOCITYY ), getVelocityY( ) );

        this.setModX( this.getModX( ) + ( this.getVelocityX( ) % 1 ) );
        this.setModY( this.getModY( ) + ( this.getVelocityY( ) % 1 ) );

        int dx = (int) this.getVelocityX( ) + (int) this.getModX( );
        int dy = (int) this.getVelocityY( ) + (int) this.getModY( );

        this.setModX( this.getModX( ) % 1 );
        this.setModY( this.getModY( ) % 1 );

        int dev = Math.max( 1, Math.max( Math.abs( dx ), Math.abs( dy ) ) );

        Point start = getMascot( ).getAnchor( );

        OUTER: for( int i = 0; i <= dev; ++i )
        {
            int x = start.x + dx * i / dev;
            int y = start.y + dy * i / dev;

            getMascot( ).setAnchor( new Point( x, y ) );
            if( dy > 0 )
            {
                // HACK IE
                for( int j = -80; j<=0; ++j )
                {
                    getMascot( ).setAnchor( new Point( x, y + j ) );
                    if( getEnvironment( ).getFloor( true ).isOn( getMascot( ).getAnchor( ) ) )
                    {
                        break OUTER;
                    }
                }
            }
            if( getEnvironment( ).getWall( true ).isOn( getMascot( ).getAnchor( ) ) )
            {
                break;
            }
        }

        getAnimation( ).next( getMascot( ), getTime( ) );
    }

    private int getInitialVx( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_INITIALVX ), Number.class, DEFAULT_INITIALVX ).intValue( );
    }

    private int getInitialVy( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_INITIALVY ), Number.class, DEFAULT_INITIALVY ).intValue( );
    }

    private double getGravity( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_GRAVITY ), Number.class, DEFAULT_GRAVITY ).doubleValue( );
    }

    private double getResistanceX( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_RESISTANCEX ), Number.class, DEFAULT_RESISTANCEX ).doubleValue( );
    }

    private double getResistanceY( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_RESISTANCEY ), Number.class, DEFAULT_RESISTANCEY ).doubleValue( );
    }

	private void setVelocityY(final double velocityY) {
		this.velocityY = velocityY;
	}

	private double getVelocityY() {
		return this.velocityY;
	}

	private void setVelocityX(final double velocityX) {
		this.velocityX = velocityX;
	}

	private double getVelocityX() {
		return this.velocityX;
	}

	private void setModX(final double modX) {
		this.modX = modX;
	}

	private double getModX() {
		return this.modX;
	}

	private void setModY(final double modY) {
		this.modY = modY;
	}

	private double getModY() {
		return this.modY;
	}

}
