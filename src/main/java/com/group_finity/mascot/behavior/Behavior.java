package com.group_finity.mascot.behavior;

import java.awt.event.MouseEvent;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.VariableException;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public interface Behavior {

    /**
     */
    public void init(Mascot mascot) throws CantBeAliveException;

    /**
     * 播放下一帧动画
     */
    public void next() throws CantBeAliveException;

    /**
     * 判断动画是否还有下一帧，若无则证明已播放完毕
     *
     * @return
     * @throws VariableException
     */
    boolean hasNext() throws VariableException;

    /**
     * 获得当前正在播放的动画名
     *
     * @return
     */
    String getName();

    /**
     * 结束当前正在播放的动画
     */
    void endCurrentAnimation();

    /**
     */
    public void mousePressed(MouseEvent e) throws CantBeAliveException;

    /**
     */
    public void mouseReleased(MouseEvent e) throws CantBeAliveException;

    public boolean isHidden();
}
