package com.group_finity.mascot.action;

import java.awt.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Offset extends InstantAction {

	private static final Logger log = LoggerFactory.getLogger(Offset.class);

	public static final String PARAMETER_OFFSETX = "X";

	private static final int DEFAULT_OFFSETX = 0;

	public static final String PARAMETER_OFFSETY = "Y";

	private static final int DEFAULT_OFFSETY = 0;

	public Offset( java.util.ResourceBundle schema, final VariableMap params )
        {
            super( schema, params );
	}

	@Override
	protected void apply() throws VariableException {
		getMascot().setAnchor(
				new Point(getMascot().getAnchor().x + getOffsetX(), getMascot().getAnchor().y + getOffsetY()));
	}

    private int getOffsetX( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_OFFSETX ), Number.class, DEFAULT_OFFSETX ).intValue( );
    }
    
    private int getOffsetY( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_OFFSETY ), Number.class, DEFAULT_OFFSETY ).intValue( );
    }
}
