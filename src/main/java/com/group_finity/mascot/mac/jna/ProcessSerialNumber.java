package com.group_finity.mascot.mac.jna;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class ProcessSerialNumber extends Structure {
	public long highLongOfPSN, lowLongOfPSN;

	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("highLongOfPSN", "lowLongOfPSN");
	}
}
