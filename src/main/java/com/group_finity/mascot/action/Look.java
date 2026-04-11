package com.group_finity.mascot.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Look extends InstantAction
{
    private static final Logger log = LoggerFactory.getLogger( Look.class.getName( ) );

    public static final String PARAMETER_LOOKRIGHT = "LookRight";

    public Look( java.util.ResourceBundle schema, final VariableMap params )
    {
        super( schema, params );
    }

    @Override
    protected void apply( ) throws VariableException
    {
        getMascot( ).setLookRight( isLookRight( ) );
    }

    private Boolean isLookRight( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_LOOKRIGHT ), Boolean.class, !getMascot( ).isLookRight( ) );
    }
}
