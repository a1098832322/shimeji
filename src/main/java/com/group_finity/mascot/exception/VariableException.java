package com.group_finity.mascot.exception;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class VariableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VariableException(final String message) {
		super(message);
	}

	public VariableException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
