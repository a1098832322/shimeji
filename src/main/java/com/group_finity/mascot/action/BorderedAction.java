package com.group_finity.mascot.action;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Border;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public abstract class BorderedAction extends ActionBase {

	private static final Logger log = LoggerFactory.getLogger(BorderedAction.class);

	private static final String PARAMETER_BORDERTYPE = "BorderType";

	public static final String DEFAULT_BORDERTYPE = null;

	public static final String BORDERTYPE_CEILING = "Ceiling";

	public static final String BORDERTYPE_WALL = "Wall";

	public static final String BORDERTYPE_FLOOR = "Floor";

	private Border border;

	public BorderedAction( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap context )
        {
            super( schema, animations, context );
	}

	@Override
	public void init(final Mascot mascot) throws VariableException {
		super.init(mascot);

		final String borderType = getBorderType();

		if( getSchema( ).getString( BORDERTYPE_CEILING ).equals( borderType ) ) {
			this.setBorder(getEnvironment().getCeiling());
		} else if( getSchema( ).getString( BORDERTYPE_WALL ).equals( borderType ) ) {
			this.setBorder(getEnvironment().getWall());
		} else if( getSchema( ).getString( BORDERTYPE_FLOOR ).equals( borderType ) ) {
			this.setBorder(getEnvironment().getFloor());
		}
	}

	@Override
	protected void tick() throws LostGroundException, VariableException {
		if (getBorder() != null) {
			getMascot().setAnchor(getBorder().move(getMascot().getAnchor()));
		}
	}

	private String getBorderType() throws VariableException {
		return eval( getSchema( ).getString( PARAMETER_BORDERTYPE ), String.class, DEFAULT_BORDERTYPE);
	}

	private void setBorder(final Border border) {
		this.border = border;
	}
	
	protected Border getBorder() {
		return this.border;
	}

}
