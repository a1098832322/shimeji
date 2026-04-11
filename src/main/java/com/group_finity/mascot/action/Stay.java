package com.group_finity.mascot.action;

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
public class Stay extends BorderedAction {

	private static final Logger log = LoggerFactory.getLogger(Stay.class);

	public Stay( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap params )
        {
            super( schema, animations, params );
	}

	@Override
	protected void tick() throws LostGroundException, VariableException {

		super.tick();

		if ((getBorder() != null) && !getBorder().isOn(getMascot().getAnchor())) {
			log.info("Lost Ground ({0},{1})", new Object[] { getMascot(), this });
			throw new LostGroundException();
		}

		getAnimation().next(getMascot(), getTime());
	}

}
