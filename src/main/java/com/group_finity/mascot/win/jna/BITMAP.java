package com.group_finity.mascot.win.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class BITMAP extends Structure {
	  public int   bmType; 
	  public int   bmWidth; 
	  public int   bmHeight; 
	  public int   bmWidthBytes; 
	  public short   bmPlanes; 
	  public short   bmBitsPixel; 
	  public Pointer bmBits;
}
