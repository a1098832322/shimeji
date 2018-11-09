package com.group_finity.mascot.action;

import java.util.List;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Animate extends BorderedAction {

	private static final Logger log = Logger.getLogger(Animate.class.getName());

	public Animate(final List<Animation> animations, final VariableMap params) {
		super(animations, params);

	}

	@Override
	protected void tick() throws LostGroundException, VariableException {

		super.tick();

		if ((getBorder() != null) && !getBorder().isOn(getMascot().getAnchor())) {
			throw new LostGroundException();
		}

		getAnimation().next(getMascot(), getTime());

	}

	@Override
	public boolean hasNext() throws VariableException {

		final boolean intime = getTime() < getAnimation().getDuration();

		return super.hasNext() && intime;
	}

}
