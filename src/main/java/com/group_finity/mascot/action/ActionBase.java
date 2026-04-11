package com.group_finity.mascot.action;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public abstract class ActionBase implements Action
{
    private static final Logger log = LoggerFactory.getLogger(ActionBase.class);

    public static final String PARAMETER_DURATION = "Duration";

    private static final boolean DEFAULT_CONDITION = true;

    public static final String PARAMETER_CONDITION = "Condition";

    private static final int DEFAULT_DURATION = Integer.MAX_VALUE;

    public static final String PARAMETER_DRAGGABLE = "Draggable";

    private static final boolean DEFAULT_DRAGGABLE = true;
    
    public static final String PARAMETER_AFFORDANCE = "Affordance";

    private static final String DEFAULT_AFFORDANCE = "";

    private Mascot mascot;

    private int startTime;

    private List<Animation> animations;

    private VariableMap variables;

    private ResourceBundle schema;

    public ActionBase( ResourceBundle schema, final List<Animation> animations, final VariableMap context )
    {
        this.schema = schema;
        this.animations = animations;
        this.variables = context;
    }

    @Override
    public String toString( )
    {
        try
        {
            return "Action (" + getClass( ).getSimpleName( ) + "," + getName( ) + ")";
        }
        catch( final VariableException e )
        {
            return "Action (" + getClass( ).getSimpleName( ) + "," + null + ")";
        }
    }

    @Override
    public void init(final Mascot mascot) throws VariableException
    {
        setMascot( mascot );
        setTime( 0 );

        getVariables( ).put( "mascot", mascot );
        getVariables( ).put( "action", this );

        getVariables( ).init( );

        for( final Animation animation : animations )
        {
            animation.init( );
        }
    }

    @Override
    public void next( ) throws LostGroundException, VariableException
    {
        initFrame( );
        
        // affordances
        if( !getMascot( ).getAffordances( ).isEmpty( ) )
            getMascot( ).getAffordances( ).clear( );
        if( !getAffordance( ).trim( ).isEmpty( ) )
            getMascot( ).getAffordances( ).add( getAffordance( ) );
        
        // hotspots
        refreshHotspots( );
        
        tick( );
    }

    private void initFrame( )
    {
        getVariables( ).initFrame( );

        for( final Animation animation : getAnimations( ) )
        {
            animation.initFrame( );
        }
    }

    protected List<Animation> getAnimations( )
    {
        return animations;
    }

    protected abstract void tick( ) throws LostGroundException, VariableException;

    @Override
    public boolean hasNext( ) throws VariableException
    {
        final boolean effective = isEffective( );
        final boolean intime = getTime( ) < getDuration( );

        return effective && intime;
    }

    protected void refreshHotspots( )
    {
        getMascot( ).getHotspots( ).clear( );
        try
        {
            if( getAnimation( ) != null )
            {
                for( final Hotspot hotspot : getAnimation( ).getHotspots( ) )
                    getMascot( ).getHotspots( ).add( hotspot );
            }
        }
        catch( VariableException ex )
        {
            getMascot( ).getHotspots( ).clear( );
        }
    }
        
    public Boolean isDraggable( ) throws VariableException
    {
        return eval( schema.getString( PARAMETER_DRAGGABLE ), Boolean.class, DEFAULT_DRAGGABLE );
    }

    private Boolean isEffective( ) throws VariableException
    {
        return eval( schema.getString( PARAMETER_CONDITION ), Boolean.class, DEFAULT_CONDITION );
    }

    private int getDuration( ) throws VariableException
    {
        return eval( schema.getString( PARAMETER_DURATION ), Number.class, DEFAULT_DURATION ).intValue( );
    }

    protected String getAffordance( ) throws VariableException
    {
        return eval( schema.getString( PARAMETER_AFFORDANCE ), String.class, DEFAULT_AFFORDANCE );
    }

    private void setMascot( final Mascot mascot )
    {
        this.mascot = mascot;
    }

    protected Mascot getMascot( )
    {
        return mascot;
    }

    protected int getTime( )
    {
        return getMascot( ).getTime( ) - startTime;
    }

    protected void setTime( final int time )
    {
        startTime = getMascot( ).getTime( ) - time;
    }

    private String getName( ) throws VariableException
    {
        return eval( schema.getString( "Name" ), String.class, null );
    }

    protected Animation getAnimation( ) throws VariableException
    {
        for( final Animation animation : getAnimations( ) )
        {
            if( animation.isEffective( getVariables( ) ) )
            {
                return animation;
            }
        }

        return null;
    }

    protected VariableMap getVariables( )
    {
        return variables;
    }

    protected void putVariable( final String key, final Object value )
    {
        synchronized( getVariables( ) )
        {
            getVariables( ).put( key, value );
        }
    }

    protected <T> T eval(final String name, final Class<T> type, final T defaultValue) throws VariableException
    {
        synchronized( getVariables( ) )
        {
            final Variable variable = getVariables( ).getRawMap( ).get( name );
            if( variable != null )
            {
                return type.cast( variable.get( getVariables( ) ) );
            }
        }

        return defaultValue;
    }

    protected MascotEnvironment getEnvironment( )
    {
        return getMascot( ).getEnvironment( );
    }

    protected ResourceBundle getSchema( )
    {
        return schema;
    }
}
