package com.group_finity.mascot.action;

import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Move extends BorderedAction {

	private static final Logger log = Logger.getLogger(Move.class.getName());

	private static final String PARAMETER_TARGETX = "TargetX";

	private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

	private static final String PARAMETER_TARGETY = "TargetY";

	private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;
        
	public Move(final List<Animation> animations, final VariableMap params) {
		super(animations, params);
	}

	@Override
	public boolean hasNext() throws VariableException {

		final int targetX = getTargetX();
		final int targetY = getTargetY();

		boolean noMoveX = false;
		boolean noMoveY = false;

		if (targetX != Integer.MIN_VALUE) {
			if (getMascot().getAnchor().x == targetX) {
				noMoveX = true;
			}
		}

		if (targetY != Integer.MIN_VALUE) {
			if (getMascot().getAnchor().y == targetY) {
				noMoveY = true;
			}
		}

		return super.hasNext() && !noMoveX && !noMoveY;
	}

	@Override
	protected void tick() throws LostGroundException, VariableException {

		super.tick();

		if ((getBorder() != null) && !getBorder().isOn(getMascot().getAnchor())) {
			log.log(Level.INFO, "Lost Ground ({0},{1})", new Object[] { getMascot(), this });
			throw new LostGroundException();
		}

		int targetX = getTargetX();
		int targetY = getTargetY();

		boolean down = false;

		if (targetX != DEFAULT_TARGETX) {
			if (getMascot().getAnchor().x != targetX) {
                            	getMascot().setLookRight(getMascot().getAnchor().x < targetX);
			}
		}
		if (targetY != DEFAULT_TARGETY) {
			down = getMascot().getAnchor().y < targetY;
		}

		getAnimation().next(getMascot(), getTime());

		if (targetX != DEFAULT_TARGETX) {
			if ((getMascot().isLookRight() && (getMascot().getAnchor().x >= targetX))
					|| (!getMascot().isLookRight() && (getMascot().getAnchor().x <= targetX))) {
				getMascot().setAnchor(new Point(targetX, getMascot().getAnchor().y));
			}
		}
		if (targetY != DEFAULT_TARGETY) {
			if ((down && (getMascot().getAnchor().y >= targetY)) ||
					(!down && (getMascot().getAnchor().y <= targetY))) {
				getMascot().setAnchor(new Point(getMascot().getAnchor().x, targetY));
			}
		}

	}

	private int getTargetY() throws VariableException {
		return eval(PARAMETER_TARGETY, Number.class, DEFAULT_TARGETY).intValue();
	}

	private int getTargetX() throws VariableException {
		return eval(PARAMETER_TARGETX, Number.class, DEFAULT_TARGETX).intValue();
	}

}
