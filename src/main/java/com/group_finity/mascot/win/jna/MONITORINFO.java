package com.group_finity.mascot.win.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

/**
 *
 * @author Kilkakon
 */
public class MONITORINFO extends Structure
{
    public NativeLong cbSize;
    public RECT rcMonitor;
    public RECT rcWork;
    public NativeLong dwFlags;
}
