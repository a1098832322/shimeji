package com.group_finity.mascot.mac.jna;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class CGSize extends Structure {
	public double width, height;

	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("width", "height");
	}

	public int getWidth() {
		return (int) Math.round(this.width);
	}

	public int getHeight() {
		return (int) Math.round(this.height);
	}
}

