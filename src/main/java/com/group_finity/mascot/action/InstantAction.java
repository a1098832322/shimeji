package com.group_finity.mascot.action;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public abstract class InstantAction extends ActionBase {

	private static final Logger log = LoggerFactory.getLogger(InstantAction.class);

	public InstantAction( java.util.ResourceBundle schema, final VariableMap params) {
		super( schema, new ArrayList<Animation>(), params);

	}

	@Override
	public final void init(final Mascot mascot) throws VariableException {
		super.init(mascot);

		if (super.hasNext()) {
			apply();
		}
	}

	protected abstract void apply() throws VariableException;

	@Override
	public final boolean hasNext() throws VariableException {
		return super.hasNext() && false;
	}

	@Override
	protected final void tick() {
	}
}
