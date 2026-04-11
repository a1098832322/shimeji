package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public interface Action {

	/**
	 * @param mascot 
	 */
	public void init(Mascot mascot) throws VariableException;

	/**
	 * @return 
	 */
	public boolean hasNext() throws VariableException;
	
	/**
	 * 
	 * @throws LostGroundException 
	 */
	public void next() throws LostGroundException, VariableException;
	
}
