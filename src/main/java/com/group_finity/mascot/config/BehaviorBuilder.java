package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class BehaviorBuilder {

	private static final Logger log = Logger.getLogger(BehaviorBuilder.class.getName());

	private final Configuration configuration;

	private final String name;

	private final String actionName;

	private final int frequency;

	private final List<String> conditions;

	private final boolean hidden;

	private final boolean nextAdditive;

	private final List<BehaviorBuilder> nextBehaviorBuilders = new ArrayList<BehaviorBuilder>();

	private final Map<String, String> params = new LinkedHashMap<String, String>();

	public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) {
		this.configuration = configuration;
		this.name = behaviorNode.getAttribute("Name");
		this.actionName = behaviorNode.getAttribute("Action") == null ? getName() : behaviorNode.getAttribute("Action");
		this.frequency = Integer.parseInt(behaviorNode.getAttribute("Frequency"));
        this.hidden = Boolean.parseBoolean(behaviorNode.getAttribute("Hidden"));
		this.conditions = new ArrayList<String>(conditions);
		this.getConditions().add(behaviorNode.getAttribute("Condition"));

		log.log(Level.INFO, "Start Reading({0})", this);

		this.getParams().putAll(behaviorNode.getAttributes());
		this.getParams().remove("Name");
		this.getParams().remove("Action");
		this.getParams().remove("Frequency");
		this.getParams().remove("Hidden");
		this.getParams().remove("Condition");

		boolean nextAdditive = true;

		for (final Entry nextList : behaviorNode.selectChildren("NextBehaviorList")) {

			log.log(Level.INFO, "Lists the Following Behaviors...");

			nextAdditive = Boolean.parseBoolean(nextList.getAttribute("Add"));

			loadBehaviors(nextList, new ArrayList<String>());
		}
		
		this.nextAdditive = nextAdditive;

		log.log(Level.INFO, "Behaviors have finished loading({0})", this);

	}

	@Override
	public String toString() {
		return "Behavior(" + getName() + "," + getFrequency() + "," + getActionName() + ")";
	}

	private void loadBehaviors(final Entry list, final List<String> conditions) {
		
		for (final Entry node : list.getChildren()) {

			if (node.getName().equals("Condition")) {

				final List<String> newConditions = new ArrayList<String>(conditions);
				newConditions.add(node.getAttribute("Condition"));

				loadBehaviors(node, newConditions);

			} else if (node.getName().equals("BehaviorReference")) {
				final BehaviorBuilder behavior = new BehaviorBuilder(getConfiguration(), node, conditions);
				getNextBehaviorBuilders().add(behavior);
			}
		}
	}

	public void validate() throws ConfigurationException {
		
		if ( !getConfiguration().getActionBuilders().containsKey(getActionName()) ) {
			log.log(Level.SEVERE, "There is no corresponding action(" + this + ")");			
			throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoActionFoundErrorMessage" ) + "("+this+")");
		}
	}

	public Behavior buildBehavior() throws BehaviorInstantiationException {

		try {
			return new UserBehavior(getName(),
						getConfiguration().buildAction(getActionName(), 
								getParams()), getConfiguration(), isHidden( ) );
		} catch (final ActionInstantiationException e) {
			log.log(Level.SEVERE, "Failed to initialize the corresponding action("+this+")");				
			throw new BehaviorInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedInitialiseCorrespondingActionErrorMessage" ) + "("+this+")", e);
		}
	}

	
	public boolean isEffective(final VariableMap context) throws VariableException {

		for (final String condition : getConditions()) {
			if (condition != null) {
				if (!(Boolean) Variable.parse(condition).get(context)) {
					return false;
				}
			}
		}

		return true;
	}
	
	public String getName() {
		return this.name;
	}

	public int getFrequency() {
		return this.frequency;
	}

	public boolean isHidden( ) {
		return this.hidden;
	}

	private String getActionName() {
		return this.actionName;
	}
	
	private Map<String, String> getParams() {
		return this.params;
	}
	
	private List<String> getConditions() {
		return this.conditions;
	}
	
	private Configuration getConfiguration() {
		return this.configuration;
	}

	public boolean isNextAdditive() {
		return this.nextAdditive;
	}

	public List<BehaviorBuilder> getNextBehaviorBuilders() {
		return this.nextBehaviorBuilders;
	}
}
