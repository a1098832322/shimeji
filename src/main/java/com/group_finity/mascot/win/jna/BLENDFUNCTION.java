package com.group_finity.mascot.win.jna;

import com.sun.jna.Structure;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class BLENDFUNCTION extends Structure {
	public static final byte AC_SRC_OVER = 0;
	public static final byte AC_SRC_ALPHA = 1;

	public byte BlendOp;
	public byte BlendFlags;
	public byte SourceConstantAlpha;
	public byte AlphaFormat;
}
