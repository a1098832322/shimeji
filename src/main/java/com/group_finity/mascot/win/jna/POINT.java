package com.group_finity.mascot.win.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class POINT extends Structure{

	public int x;
	public int y;

	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("x", "y");
	}
}
