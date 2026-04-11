package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(BehaviorBuilder.class);

	private final Configuration configuration;

	private final String name;

	private final String actionName;

	private final int frequency;

	private final List<String> conditions;

	private final boolean hidden;
        
        private final boolean toggleable;

	private final boolean nextAdditive;

	private final List<BehaviorBuilder> nextBehaviorBuilders = new ArrayList<BehaviorBuilder>();

	private final Map<String, String> params = new LinkedHashMap<String, String>();

	public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) {
		this.configuration = configuration;
		this.name = behaviorNode.getAttribute( configuration.getSchema( ).getString( "Name" ) );
		this.actionName = behaviorNode.getAttribute( configuration.getSchema( ).getString( "Action" ) ) == null ? getName( ) : behaviorNode.getAttribute( configuration.getSchema( ).getString( "Action" ) );
		this.frequency = Integer.parseInt( behaviorNode.getAttribute( configuration.getSchema( ).getString( "Frequency" ) ) );
                this.hidden = Boolean.parseBoolean( behaviorNode.getAttribute( configuration.getSchema( ).getString( "Hidden" ) ) );
		this.conditions = new ArrayList<String>(conditions);
		this.getConditions().add(behaviorNode.getAttribute( configuration.getSchema( ).getString( "Condition" ) ) );
                
                // override of toggleable state for required fields
                if( name.equals( UserBehavior.BEHAVIOURNAME_FALL ) ||
                    name.equals( UserBehavior.BEHAVIOURNAME_THROWN ) ||
                    name.equals( UserBehavior.BEHAVIOURNAME_DRAGGED ) )
                {
                    toggleable = false;
                }
                else
                {
                    toggleable = Boolean.parseBoolean( behaviorNode.getAttribute( configuration.getSchema( ).getString( "Toggleable" ) ) );
                }
                
		log.debug("Start Reading({})", this);

		this.getParams( ).putAll( behaviorNode.getAttributes( ) );
		this.getParams( ).remove( configuration.getSchema( ).getString( "Name" ) );
		this.getParams( ).remove( configuration.getSchema( ).getString( "Action" ) );
		this.getParams( ).remove( configuration.getSchema( ).getString( "Frequency" ) );
		this.getParams( ).remove( configuration.getSchema( ).getString( "Hidden" ) );
		this.getParams( ).remove( configuration.getSchema( ).getString( "Condition" ) );
                this.getParams( ).remove( configuration.getSchema( ).getString( "Toggleable" ) );

		boolean nextAdditive = true;

		for( final Entry nextList : behaviorNode.selectChildren( configuration.getSchema( ).getString( "NextBehaviourList" ) ) )
                {
			log.info("Lists the Following Behaviors...");

			nextAdditive = Boolean.parseBoolean( nextList.getAttribute( configuration.getSchema( ).getString( "Add"  ) ) );

			loadBehaviors(nextList, new ArrayList<String>());
		}
		
		this.nextAdditive = nextAdditive;

		log.debug("Behaviors have finished loading({})", this);

	}

	@Override
	public String toString() {
		return "Behavior(" + getName() + "," + getFrequency() + "," + getActionName() + ")";
	}

	private void loadBehaviors(final Entry list, final List<String> conditions) {
		
		for (final Entry node : list.getChildren()) {

			if( node.getName( ).equals( configuration.getSchema( ).getString( "Condition" ) ) )
                        {

				final List<String> newConditions = new ArrayList<String>(conditions);
				newConditions.add( node.getAttribute( configuration.getSchema( ).getString( "Condition" ) ) );

				loadBehaviors(node, newConditions);

			}
                        else if( node.getName( ).equals( configuration.getSchema( ).getString( "BehaviourReference" ) ) )
                        {
				final BehaviorBuilder behavior = new BehaviorBuilder(getConfiguration(), node, conditions);
				getNextBehaviorBuilders().add(behavior);
			}
		}
	}

	public void validate() throws ConfigurationException {
		
		if ( !getConfiguration().getActionBuilders().containsKey(getActionName()) ) {
			log.error("There is no corresponding action({})", this);			
			throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoActionFoundErrorMessage" ) + "("+this+")");
		}
	}

	public Behavior buildBehavior() throws BehaviorInstantiationException {

		try {
			return new UserBehavior(getName(),
						getConfiguration().buildAction(getActionName(), 
								getParams()), getConfiguration() );
		} catch (final ActionInstantiationException e) {
			log.error("Failed to initialize the corresponding action({})", this);				
			throw new BehaviorInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedInitialiseCorrespondingActionErrorMessage" ) + "("+this+")", e);
		}
	}

	
    public boolean isEffective(final VariableMap context) throws VariableException
    {
        if( frequency == 0 )
            return false;

        for( final String condition : getConditions( ) )
        {
            if( condition != null )
            {
                if( !(Boolean)Variable.parse( condition ).get( context ) )
                {
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

    public boolean isHidden( )
    {
        return hidden;
    }

    public boolean isToggleable( )
    {
        return toggleable;
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
