package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class ActionRef implements IActionBuilder {

	private static final Logger log = LoggerFactory.getLogger(ActionRef.class);

	private final Configuration configuration;

	private final String name;

	private final Map<String, String> params = new LinkedHashMap<String, String>();

	public ActionRef(final Configuration configuration, final Entry refNode) {
		this.configuration = configuration;

		this.name = refNode.getAttribute( configuration.getSchema( ).getString( "Name" ) );
		this.getParams().putAll(refNode.getAttributes());

		log.debug("Read Action Reference({})", this);
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
			log.error("There is no corresponding behavior({})", this);		
			throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoBehaviourFoundErrorMessage" ) + "(" + this + ")");
		}
	}

	public Action buildAction( final Map<String, String> params) throws ActionInstantiationException {
		final Map<String, String> newParams = new LinkedHashMap<String, String>(params);
		newParams.putAll(getParams());
		return this.getConfiguration().buildAction(getName(), newParams);
	}
}
