package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class Animation
{
    private Variable condition;
    private final Pose[ ] poses;
    private final Hotspot[ ] hotspots;
    private boolean turn;

    public Animation( final Variable condition, final Pose[ ] poses, final Hotspot[ ] hotspots, final boolean turn )
    {
        if( poses.length == 0 )
        {
            throw new IllegalArgumentException( "poses.length==0" );
        }

        this.condition = condition;
        this.poses = poses;
        this.hotspots = hotspots;
        this.turn = turn;
    }

    public boolean isEffective( final VariableMap variables ) throws VariableException
    {
        return (Boolean)getCondition( ).get( variables );
    }

    public void init( )
    {
        getCondition( ).init( );
    }

    public void initFrame( )
    {
        getCondition( ).initFrame( );
    }

    public void next( final Mascot mascot, final int time )
    {
        getPoseAt( time ).next( mascot );
    }

    public Pose getPoseAt( int time )
    {
        time %= getDuration( );

        for( final Pose pose : getPoses( ) )
        {
            time -= pose.getDuration( );
            if( time < 0 )
            {
                return pose;
            }
        }

        return null;
    }

    public int getDuration( )
    {
        int duration = 0;
        for( final Pose pose : getPoses( ) )
        {
            duration += pose.getDuration( );
        }

        return duration;
    }

    private Variable getCondition( )
    {
        return condition;
    }

    private Pose[ ] getPoses( )
    {
        return poses;
    }

    public Hotspot[ ] getHotspots( )
    {
        return hotspots;
    }
        
    public boolean isTurn( )
    {
        return turn;
    }
}
