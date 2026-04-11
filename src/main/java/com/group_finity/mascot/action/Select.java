package com.group_finity.mascot.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Select extends ComplexAction
{
    private static final Logger log = LoggerFactory.getLogger( Select.class.getName( ) );

    public Select( java.util.ResourceBundle schema, final VariableMap params, final Action... actions )
    {
        super( schema, params, actions);
    }
}
