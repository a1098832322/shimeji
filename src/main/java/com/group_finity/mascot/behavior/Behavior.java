package com.group_finity.mascot.behavior;

import java.awt.event.MouseEvent;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public interface Behavior
{
    public void init( Mascot mascot ) throws CantBeAliveException;

    public void next( ) throws CantBeAliveException;

    public void mousePressed( MouseEvent e ) throws CantBeAliveException;

    public void mouseReleased( MouseEvent e ) throws CantBeAliveException;
}
