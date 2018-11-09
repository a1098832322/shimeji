package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static final Logger log = Logger.getLogger(ActionBuilder.class.getName());

	private final String type;

	private final String name;

	private final String className;

	private final Map<String, String> params = new LinkedHashMap<String, String>();

	private final List<AnimationBuilder> animationBuilders = new ArrayList<AnimationBuilder>();

	private final List<IActionBuilder> actionRefs = new ArrayList<IActionBuilder>();

	public ActionBuilder(final Configuration configuration, final Entry actionNode, final String imageSet) throws IOException {
		this.name = actionNode.getAttribute("Name");
		this.type = actionNode.getAttribute("Type");
		this.className = actionNode.getAttribute("Class");

		log.log(Level.INFO, "Read Start Operation({0})", this);

		this.getParams().putAll(actionNode.getAttributes());
		for (final Entry node : actionNode.selectChildren("Animation")) {
			this.getAnimationBuilders().add(new AnimationBuilder(node,imageSet));
		}

		for (final Entry node : actionNode.getChildren()) {
			if (node.getName().equals("ActionReference")) {
				this.getActionRefs().add(new ActionRef(configuration, node));
			} else if (node.getName().equals("Action")) {
				this.getActionRefs().add(new ActionBuilder(configuration, node, imageSet));
			}
		}

		log.log(Level.INFO, "Actions Finished Loading");
	}

	@Override
	public String toString() {
		return "Action(" + getName() + "," + getType() + "," + getClassName() + ")";
	}

	@SuppressWarnings("unchecked")
	public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {

		try {
			// Create Variable Map
			final VariableMap variables = createVariables(params);

			// Create Animations
			final List<Animation> animations = createAnimations();

			// Create Child Actions
			final List<Action> actions = createActions();

			if (this.type.equals("Embedded")) {
				try {
					final Class<? extends Action> cls = (Class<? extends Action>) Class.forName(this.getClassName());
					try {

						try {
							return cls.getConstructor(List.class, VariableMap.class).newInstance(animations, variables);
						} catch (final Exception e) {
							// NOTE There's no constructor
						}

						return cls.getConstructor(VariableMap.class).newInstance(variables);
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

			} else if (this.type.equals("Move")) {
				return new Move(animations, variables);
			} else if (this.type.equals("Stay")) {
				return new Stay(animations, variables);
			} else if (this.type.equals("Animate")) {
				return new Animate(animations, variables);
			} else if (this.type.equals("Sequence")) {
				return new Sequence(variables, actions.toArray(new Action[0]));
			} else if (this.type.equals("Select")) {
				return new Select(variables, actions.toArray(new Action[0]));
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
	
	private List<Action> createActions() throws ActionInstantiationException {
		final List<Action> actions = new ArrayList<Action>();
		for (final IActionBuilder ref : this.getActionRefs()) {
			actions.add(ref.buildAction(new HashMap<String, String>()));
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
