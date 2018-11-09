package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Main;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

/**
 * Simple Sample Behavior.
 * <p>
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class UserBehavior implements Behavior {
    private static final Logger log = Logger.getLogger(UserBehavior.class.getName());

    public static final String BEHAVIORNAME_FALL = "Fall";

    public static final String BEHAVIORNAME_DRAGGED = "Dragged";

    public static final String BEHAVIORNAME_THROWN = "Thrown";

    /**
     * 角色执行的命令们
     */
    private static final String[] commands = {"ThrowIEFromRight", "ThrowIEFromLeft"
            , "WalkAndThrowIEFromLeft", "WalkAndThrowIEFromRight"};

    private final String name;

    private final Configuration configuration;

    private final Action action;

    private Mascot mascot;

    private boolean hidden;

    public UserBehavior(final String name, final Action action, final Configuration configuration, boolean hidden) {
        this.name = name;
        this.configuration = configuration;
        this.action = action;
        this.hidden = hidden;
    }

    @Override
    public String toString() {
        return "Behavior(" + getName() + ")";
    }

    @Override
    public synchronized void init(final Mascot mascot) throws CantBeAliveException {

        this.setMascot(mascot);

        log.log(Level.INFO, "Default Behavior({0},{1})", new Object[]{this.getMascot(), this});

        try {
            getAction().init(mascot);
            if (!getAction().hasNext()) {
                try {
                    mascot.setBehavior(this.getConfiguration().buildBehavior(getName(), mascot));
                } catch (final BehaviorInstantiationException e) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedInitialiseFollowingBehaviourErrorMessage"), e);
                }
            }
        } catch (final VariableException e) {
            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("VariableEvaluationErrorMessage"), e);
        }

    }

    private Configuration getConfiguration() {
        return this.configuration;
    }

    public Action getAction() {
        return this.action;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * On Mouse Pressed.
     * Start dragging.
     *
     * @ Throws CantBeAliveException
     */
    public synchronized void mousePressed(final MouseEvent event) throws CantBeAliveException {

        if (SwingUtilities.isLeftMouseButton(event)) {
            // Begin dragging
            try {
                event.getPoint();
                getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_DRAGGED));
            } catch (final BehaviorInstantiationException e) {
                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedDragActionInitialiseErrorMessage"), e);
            }
        }

    }

    /**
     * On Mouse Release.
     * End dragging.
     *
     * @ Throws CantBeAliveException
     */
    public synchronized void mouseReleased(final MouseEvent event) throws CantBeAliveException {

        if (SwingUtilities.isLeftMouseButton(event)) {
            // Termination of drag
            try {
                getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_THROWN));
            } catch (final BehaviorInstantiationException e) {
                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedDropActionInitialiseErrorMessage"), e);
            }
        }

    }

    @Override
    public synchronized boolean hasNext() throws VariableException {
        return getAction().hasNext();
    }

    @Override
    public void endCurrentAnimation() {
        getAction().endCurrentAnimation();

    }

    @Override
    public synchronized void next() throws CantBeAliveException {
        //System.out.println("Behavior Current Action:" + getName());
        try {
            if (getAction().hasNext()) {
                getAction().next();
            }

            if (getAction().hasNext()) {
                //若有下一帧
                if ((getMascot().getBounds().getX() + getMascot().getBounds().getWidth() <=
                        getEnvironment().getScreen().getLeft())
                        || (getEnvironment().getScreen().getRight() <= getMascot().getBounds().getX())
                        || (getEnvironment().getScreen().getBottom() <= getMascot().getBounds().getY())) {
                    //若角色超出屏幕界限
                    log.log(Level.INFO, "Out of the screen bounds({0},{1})", new Object[]{getMascot(), this});

                    getMascot().setAnchor(
                            new Point((int) (Math.random() * (getEnvironment().getScreen().getRight() - getEnvironment()
                                    .getScreen().getLeft()))
                                    + getEnvironment().getScreen().getLeft(), getEnvironment().getScreen().getTop() - 256));

                    try {
                        getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_FALL));
                    } catch (final BehaviorInstantiationException e) {
                        throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedFallingActionInitialiseErrorMessage"), e);
                    }
                }

            } else {
                //若播放完毕
                log.log(Level.INFO, "Completed Behavior ({0},{1})", new Object[]{getMascot(), this});

                //如果屏幕区域出现了目标窗口
                if (!getEnvironment().getActiveIE().isScreen()) {
                    //置空动画
                    getMascot().setBehavior(null);

                    //加载投掷动画
                    String command = commands[(int) (Math.random() * 2) + 2];//使用第3,4个动画
                    System.out.println("Active: " + command);
                    try {
                        getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(command));
                    } catch (BehaviorInstantiationException e1) {
                        e1.printStackTrace();
                        throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedInitialiseFollowingActionsErrorMessage"), e1);
                    }

                } else {
                    //否则随机加载动画
                    try {
                        getMascot().setBehavior(this.getConfiguration().buildBehavior(getName(), getMascot()));
                    } catch (final BehaviorInstantiationException e) {
                        throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedInitialiseFollowingActionsErrorMessage"), e);
                    }
                }
            }
        } catch (final LostGroundException e) {
            log.log(Level.INFO, "Lost Ground ({0},{1})", new Object[]{getMascot(), this});


            try {
                getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_FALL));
            } catch (final BehaviorInstantiationException ex) {
                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("FailedFallingActionInitialiseErrorMessage"), e);
            }
        } catch (final VariableException e) {
            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getProperty("VariableEvaluationErrorMessage"), e);
        }

    }

    private void setMascot(final Mascot mascot) {
        this.mascot = mascot;
    }

    private Mascot getMascot() {
        return this.mascot;
    }

    protected MascotEnvironment getEnvironment() {
        return getMascot().getEnvironment();
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }
}
