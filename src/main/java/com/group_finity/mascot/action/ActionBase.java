package com.group_finity.mascot.action;

import java.util.List;
import java.util.logging.Logger;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public abstract class ActionBase implements Action {

    private static final Logger log = Logger.getLogger(ActionBase.class.getName());

    public static final String PARAMETER_DURATION = "Duration";

    private static final boolean DEFAULT_CONDITION = true;

    public static final String PARAMETER_CONDITION = "Condition";

    private static final int DEFAULT_DURATION = Integer.MAX_VALUE;

    private Mascot mascot;

    private int startTime;

    private List<Animation> animations;

    private VariableMap variables;

    private boolean isStopAnimation = false;

    public ActionBase(final List<Animation> animations, final VariableMap context) {
        this.animations = animations;
        this.variables = context;
    }

    @Override
    public String toString() {
        try {
            return "Action (" + getClass().getSimpleName() + "," + getName() + ")";
        } catch (final VariableException e) {
            return "Action (" + getClass().getSimpleName() + "," + null + ")";
        }
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        this.setMascot(mascot);
        this.setTime(0);
        this.isStopAnimation = false;
        this.getVariables().put("mascot", mascot);
        this.getVariables().put("action", this);

        getVariables().init();

        for (final Animation animation : this.animations) {
            animation.init();
        }
    }

    @Override
    public void next() throws LostGroundException, VariableException {
        initFrame();
        tick();
    }

    /**
     * 初始化帧动画
     */
    private void initFrame() {

        getVariables().initFrame();

        for (final Animation animation : getAnimations()) {
            animation.initFrame();
        }
    }

    protected List<Animation> getAnimations() {
        return this.animations;
    }

    protected abstract void tick() throws LostGroundException, VariableException;

    @Override
    public boolean hasNext() throws VariableException {
        if (isStopAnimation) {
            //isStopAnimation = false;
            return false;
        } else {
            final boolean effective = isEffective();
            final boolean intime = getTime() < getDuration();
            return effective && intime;
        }
    }

    /**
     * 强制将当前播放结束
     */
    @Override
    public synchronized void endCurrentAnimation() {
        this.isStopAnimation = true;
    }

    private Boolean isEffective() throws VariableException {
        return eval(PARAMETER_CONDITION, Boolean.class, DEFAULT_CONDITION);
    }

    private int getDuration() throws VariableException {
        return eval(PARAMETER_DURATION, Number.class, DEFAULT_DURATION).intValue();
    }

    private void setMascot(final Mascot mascot) {
        this.mascot = mascot;
    }

    protected Mascot getMascot() {
        return this.mascot;
    }

    protected int getTime() {
        return getMascot().getTime() - this.startTime;
    }

    protected void setTime(final int time) {
        this.startTime = getMascot().getTime() - time;
    }

    private String getName() throws VariableException {
        return this.eval("Name", String.class, null);
    }

    protected Animation getAnimation() throws VariableException {
        for (final Animation animation : getAnimations()) {
            if (animation.isEffective(getVariables())) {
                return animation;
            }
        }

        return null;
    }

    protected VariableMap getVariables() {
        return this.variables;
    }

    protected void putVariable(final String key, final Object value) {
        synchronized (getVariables()) {
            getVariables().put(key, value);
        }
    }

    protected <T> T eval(final String name, final Class<T> type, final T defaultValue) throws VariableException {

        synchronized (getVariables()) {
            final Variable variable = getVariables().getRawMap().get(name);
            if (variable != null) {
                return type.cast(variable.get(getVariables()));
            }
        }

        return defaultValue;
    }

    protected MascotEnvironment getEnvironment() {
        return getMascot().getEnvironment();
    }
}
