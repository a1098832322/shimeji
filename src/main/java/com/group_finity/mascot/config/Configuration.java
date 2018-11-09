package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class Configuration {

    private static final Logger log = Logger.getLogger(Configuration.class.getName());

    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<String, ActionBuilder>();

    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<String, BehaviorBuilder>();

    public void load(final Entry configurationNode, final String imageSet) throws Exception {

        log.log(Level.INFO, "Start Reading Configuration File...");

        for (final Entry list : configurationNode.selectChildren("ActionList")) {

            log.log(Level.INFO, "Action List...");

            for (final Entry node : list.selectChildren("Action")) {

                final ActionBuilder action = new ActionBuilder(this, node, imageSet);

                if (this.getActionBuilders().containsKey(action.getName())) {
                    throw new ConfigurationException(Main.getInstance().getLanguageBundle().getProperty("DuplicateActionErrorMessage") + ": " + action.getName());
                }

                this.getActionBuilders().put(action.getName(), action);
            }
        }

        for (final Entry list : configurationNode.selectChildren("BehaviorList")) {

            log.log(Level.INFO, "Behavior List...");

            loadBehaviors(list, new ArrayList<String>());
        }

        log.log(Level.INFO, "Behavior List");
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) {
        for (final Entry node : list.getChildren()) {

            if (node.getName().equals("Condition")) {

                final List<String> newConditions = new ArrayList<String>(conditions);
                newConditions.add(node.getAttribute("Condition"));

                loadBehaviors(node, newConditions);

            } else if (node.getName().equals("Behavior")) {

                final BehaviorBuilder behavior = new BehaviorBuilder(this, node, conditions);
                this.getBehaviorBuilders().put(behavior.getName(), behavior);

            }
        }
    }

    public Action buildAction(final String name, final Map<String, String> params) throws ActionInstantiationException {

        final ActionBuilder factory = this.actionBuilders.get(name);
        if (factory == null) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getProperty("NoCorrespondingActionFoundErrorMessage") + ": " + name);
        }

        return factory.buildAction(params);
    }

    public void validate() throws ConfigurationException {

        for (final ActionBuilder builder : getActionBuilders().values()) {
            builder.validate();
        }
        for (final BehaviorBuilder builder : getBehaviorBuilders().values()) {
            builder.validate();
        }
    }

    public Behavior buildBehavior(final String previousName, final Mascot mascot) throws BehaviorInstantiationException {

        final VariableMap context = new VariableMap();
        context.put("mascot", mascot);

        final List<BehaviorBuilder> candidates = new ArrayList<BehaviorBuilder>();
        long totalFrequency = 0;
        for (final BehaviorBuilder behaviorFactory : this.getBehaviorBuilders().values()) {
            try {
                if (behaviorFactory.isEffective(context)) {
                    candidates.add(behaviorFactory);
                    totalFrequency += behaviorFactory.getFrequency();
                }
            } catch (final VariableException e) {
                log.log(Level.WARNING, "An error occurred calculating the frequency of the action", e);
            }
        }

        if (previousName != null) {
            final BehaviorBuilder previousBehaviorFactory = this.getBehaviorBuilders().get(previousName);
            if (!previousBehaviorFactory.isNextAdditive()) {
                totalFrequency = 0;
                candidates.clear();
            }
            for (final BehaviorBuilder behaviorFactory : previousBehaviorFactory.getNextBehaviorBuilders()) {
                try {
                    if (behaviorFactory.isEffective(context)) {
                        candidates.add(behaviorFactory);
                        totalFrequency += behaviorFactory.getFrequency();
                    }
                } catch (final VariableException e) {
                    log.log(Level.WARNING, "An error occurred calculating the frequency of the behavior", e);
                }
            }
        }

        if (totalFrequency == 0) {
            mascot.setAnchor(new Point(
                    (int) (Math.random() * (mascot.getEnvironment().getScreen().getRight()
                            - mascot.getEnvironment()
                            .getScreen().getLeft()))
                            + mascot.getEnvironment().getScreen().getLeft(), mascot.getEnvironment().getScreen().getTop() - 256));
            return buildBehavior(UserBehavior.BEHAVIORNAME_FALL);
        }

        double random = Math.random() * totalFrequency;

        for (final BehaviorBuilder behaviorFactory : candidates) {
            random -= behaviorFactory.getFrequency();
            if (random < 0) {
                return behaviorFactory.buildBehavior();
            }
        }

        return null;
    }

    public Behavior buildBehavior(final String name) throws BehaviorInstantiationException {
        return this.getBehaviorBuilders().get(name).buildBehavior();
    }

    Map<String, ActionBuilder> getActionBuilders() {
        return this.actionBuilders;
    }

    private Map<String, BehaviorBuilder> getBehaviorBuilders() {
        return this.behaviorBuilders;
    }

    public java.util.Set<String> getBehaviorNames() {
        return behaviorBuilders.keySet();
    }

}
