package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class ActionRef implements IActionBuilder {

	private static final Logger log = Logger.getLogger(ActionRef.class.getName());

	private final Configuration configuration;

	private final String name;

	private final Map<String, String> params = new LinkedHashMap<String, String>();

	public ActionRef(final Configuration configuration, final Entry refNode) {
		this.configuration = configuration;

		this.name = refNode.getAttribute("Name");
		this.getParams().putAll(refNode.getAttributes());

		log.log(Level.INFO, "Read Action Reference({0})", this);
	}

	@Override
	public String toString() {
		return "Action(" + getName() + ")";
	}

	private String getName() {
		return this.name;
	}

	private Map<String, String> getParams() {
		return this.params;
	}

	private Configuration getConfiguration() {
		return this.configuration;
	}

	@Override
	public void validate() throws ConfigurationException {
		if (!getConfiguration().getActionBuilders().containsKey(getName())) {
			log.log(Level.SEVERE, "There is no corresponding behavior(" + this + ")");		
			throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoBehaviourFoundErrorMessage" ) + "(" + this + ")");
		}
	}

	public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
		final Map<String, String> newParams = new LinkedHashMap<String, String>(params);
		newParams.putAll(getParams());
		return this.getConfiguration().buildAction(getName(), newParams);
	}
}
