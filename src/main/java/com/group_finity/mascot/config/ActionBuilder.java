package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.Animate;
import com.group_finity.mascot.action.Move;
import com.group_finity.mascot.action.Select;
import com.group_finity.mascot.action.Sequence;
import com.group_finity.mascot.action.Stay;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class ActionBuilder implements IActionBuilder {

	private static final Logger log = LoggerFactory.getLogger( ActionBuilder.class.getName( ) );
	private final String type;
	private final String name;
	private final String className;
	private final Map<String, String> params = new LinkedHashMap<String, String>( );
	private final List<AnimationBuilder> animationBuilders = new ArrayList<AnimationBuilder>( );
	private final List<IActionBuilder> actionRefs = new ArrayList<IActionBuilder>( );
        private final ResourceBundle schema;

	public ActionBuilder( final Configuration configuration, final Entry actionNode, final String imageSet ) throws ConfigurationException
        {
            schema = configuration.getSchema( );
            name = actionNode.getAttribute( schema.getString( "Name" ) );
            type = actionNode.getAttribute( schema.getString( "Type" ) );
            className = actionNode.getAttribute( schema.getString( "Class" ) );
            
            
            try
            {
                getParams( ).putAll( actionNode.getAttributes( ) );
                for( final Entry node : actionNode.selectChildren( schema.getString( "Animation" ) ) )
                {
                    getAnimationBuilders( ).add( new AnimationBuilder( schema, node, imageSet ) );
                }

                for( final Entry node : actionNode.getChildren( ) )
                {
                    if( node.getName( ).equals( schema.getString( "ActionReference" ) ) )
                    {
                        getActionRefs( ).add( new ActionRef( configuration, node ) );
                    }
                    else if( node.getName( ).equals( schema.getString( "Action" ) ) )
                    {
                        getActionRefs( ).add( new ActionBuilder( configuration, node, imageSet ) );
                    }
                }
            }
            catch( ConfigurationException e )
            {
                throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadActionErrorMessage" ) + " \"" + name + "\" " + Main.getInstance( ).getLanguageBundle( ).getProperty( "ForShimeji" ) + " \"" + imageSet + "\".", e );
            }

	}

	@Override
	public String toString( )
        {
	    return "Action(" + getName( ) + "," + getType( ) + "," + getClassName( ) + ")";
	}

	@SuppressWarnings("unchecked")
	public Action buildAction( final Map<String, String> params) throws ActionInstantiationException {

		try {
			// Create Variable Map
			final VariableMap variables = createVariables(params);

			// Create Animations
			final List<Animation> animations = createAnimations();

			// Create Child Actions
			final List<Action> actions = createActions( );

			if( this.type.equals( schema.getString( "Embedded" ) ) )
                        {
				try {
					final Class<? extends Action> cls = (Class<? extends Action>) Class.forName(this.getClassName());
					try {

						try {
							return cls.getConstructor( ResourceBundle.class, List.class, VariableMap.class ).newInstance( schema, animations, variables);
						} catch (final Exception e) {
							// NOTE There's no constructor
						}

						return cls.getConstructor( ResourceBundle.class, VariableMap.class ).newInstance( schema, variables );
					} catch (final Exception e) {
						// NOTE There's no constructor
					}

					return cls.newInstance();
				} catch (final InstantiationException e) {
					throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedClassActionInitialiseErrorMessage" ) + "(" + this + ")", e);
				} catch (final IllegalAccessException e) {
					throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "CannotAccessClassActionErrorMessage" ) + "(" + this + ")", e);
				} catch (final ClassNotFoundException e) {
					throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "ClassNotFoundErrorMessage" ) + "(" + this + ")", e);
				}

			} else if( this.type.equals( schema.getString( "Move" ) ) ) {
                            return new Move( schema, animations, variables );
			} else if( this.type.equals( schema.getString( "Stay" ) ) ) {
                            return new Stay( schema, animations, variables);
			} else if( this.type.equals( schema.getString( "Animate" ) ) ) {
                            return new Animate( schema, animations, variables);
			} else if( this.type.equals( schema.getString( "Sequence" ) ) ) {
                            return new Sequence( schema, variables, actions.toArray(new Action[0]));
			} else if( this.type.equals( schema.getString( "Select" ) ) ) {
                            return new Select( schema, variables, actions.toArray(new Action[0]));
			} else {
                            throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "UnknownActionTypeErrorMessage" ) + "(" + this + ")");
			}

		} catch (final AnimationInstantiationException e) {
			throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedCreateAnimationErrorMessage" ) + "(" + this + ")", e);
		} catch (final VariableException e) {
			throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedParameterEvaluationErrorMessage" ) + "(" + this + ")", e);
		}
	}

	public void validate() throws ConfigurationException {

		for (final IActionBuilder ref : this.getActionRefs()) {
			ref.validate();
		}
	}
	
	private List<Action> createActions( ) throws ActionInstantiationException {
		final List<Action> actions = new ArrayList<Action>();
		for (final IActionBuilder ref : this.getActionRefs()) {
			actions.add( ref.buildAction( new HashMap<String, String>( ) ) );
		}
		return actions;
	}

	private List<Animation> createAnimations() throws AnimationInstantiationException {
		final List<Animation> animations = new ArrayList<Animation>();
		for (final AnimationBuilder animationFactory : this.getAnimationBuilders()) {
			animations.add(animationFactory.buildAnimation());
		}
		return animations;
	}

	private VariableMap createVariables(final Map<String, String> params) throws VariableException {
		final VariableMap variables = new VariableMap();
		for (final Map.Entry<String, String> param : this.getParams().entrySet()) {
			variables.put(param.getKey(), Variable.parse(param.getValue()));
		}
		for (final Map.Entry<String, String> param : params.entrySet()) {
			variables.put(param.getKey(), Variable.parse(param.getValue()));
		}
		return variables;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	private String getClassName() {
		return this.className;
	}

	private Map<String, String> getParams() {
		return this.params;
	}

	private List<AnimationBuilder> getAnimationBuilders() {
		return this.animationBuilders;
	}

	private List<IActionBuilder> getActionRefs() {
		return this.actionRefs;
	}


}
