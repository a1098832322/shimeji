package com.group_finity.mascot.behavior;

import java.awt.Point;
import java.awt.event.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ActionBase;
//import com.group_finity.mascot.action.Dragged;
//import com.group_finity.mascot.action.Regist;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;

/**
 * Simple Sample Behavior.
 *
 * Original Author: Yuki Yamada of Group Finity
 * (http://www.group-finity.com/Shimeji/) Currently developed by Shimeji-ee
 * Group.
 */
public class UserBehavior implements Behavior
{
    private static final Logger log = LoggerFactory.getLogger( UserBehavior.class.getName() );

    public static final String BEHAVIOURNAME_FALL = "Fall";

    public static final String BEHAVIOURNAME_DRAGGED = "Dragged";

    public static final String BEHAVIOURNAME_THROWN = "Thrown";
    
    private enum HotspotResult { INACTIVE, ACTIVE_NULL, ACTIVE };

    private final String name;

    private final Configuration configuration;

    private final Action action;

    private Mascot mascot;

    public UserBehavior( final String name, final Action action, final Configuration configuration )
    {
        this.name = name;
        this.configuration = configuration;
        this.action = action;
    }

    @Override
    public String toString( )
    {
        return "Behavior(" + getName( ) + ")";
    }

    @Override
    public synchronized void init( final Mascot mascot ) throws CantBeAliveException
    {
        this.setMascot( mascot );

        try
        {
            getAction( ).init( mascot );
            if( !getAction( ).hasNext( ) )
            {
                try
                {
                    mascot.setBehavior( this.getConfiguration( ).buildNextBehavior( getName( ), mascot ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedInitialiseFollowingBehaviourErrorMessage" ), e );
                }
            }
        }
        catch( final VariableException e )
        {
            throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "VariableEvaluationErrorMessage" ), e );
        }
    }

    private Configuration getConfiguration( )
    {
        return this.configuration;
    }

    private Action getAction( )
    {
        return this.action;
    }

    private String getName( )
    {
        return this.name;
    }

    /**
     * On Mouse Pressed. Start dragging.
     *
     * @ Throws CantBeAliveException
     */
    public synchronized void mousePressed( final MouseEvent event ) throws CantBeAliveException
    {
        if( SwingUtilities.isLeftMouseButton( event ) )
        {
            boolean handled = false;

            // check for hotspots
            if( !mascot.getHotspots( ).isEmpty( ) )
            {
                for( final Hotspot hotspot : mascot.getHotspots( ) )
                {
                    if( hotspot.contains( mascot, event.getPoint( ) ) &&
                        Main.getInstance( ).getConfiguration( mascot.getImageSet( ) ).isBehaviorEnabled( hotspot.getBehaviour( ), mascot ) )
                    {
                        // activate hotspot
                        handled = true;
                        try
                        {
                            getMascot( ).setCursorPosition( event.getPoint( ) );
                            if( hotspot.getBehaviour( ) != null )
                                getMascot( ).setBehavior( configuration.buildBehavior( hotspot.getBehaviour( ), mascot ) );
                        }
                        catch( final BehaviorInstantiationException e )
                        {
                            throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedInitialiseFollowingBehaviourErrorMessage" ), e );
                        }
                        break;
                    }
                }
            }

            // check if this action has dragging disabled
            if( !handled && action != null && action instanceof ActionBase )
            {
                try
                {
                    handled = !( (ActionBase)action ).isDraggable( );
                }
                catch( VariableException ex )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedDragActionInitialiseErrorMessage" ), ex );
                }
            }

            if( !handled )
            {
                // Begin dragging
                try
                {
                    getMascot( ).setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( BEHAVIOURNAME_DRAGGED ) ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedDragActionInitialiseErrorMessage" ), e );
                }
            }
        }
    }

    /**
     * On Mouse Release. End dragging.
     *
     * @ Throws CantBeAliveException
     */
    public synchronized void mouseReleased( final MouseEvent event ) throws CantBeAliveException
    {
        if( SwingUtilities.isLeftMouseButton( event ) )
        {
            if( getMascot( ).isHotspotClicked( ) )
                getMascot( ).setCursorPosition( null );

            // check if we are in the middle of a drag, otherwise we do nothing
            if( getMascot( ).isDragging( ) )
            {
                try
                {
                    getMascot( ).setDragging( false );
                    getMascot( ).setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( BEHAVIOURNAME_THROWN ) ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedDropActionInitialiseErrorMessage" ), e );
                }
            }
        }
    }

    @Override
    public synchronized void next( ) throws CantBeAliveException
    {
        try
        {
            if( getAction( ).hasNext( ) )
            {
                getAction( ).next( );
            }

            HotspotResult hotspotIsActive = HotspotResult.INACTIVE;
            if( getMascot( ).isHotspotClicked( ) )
            {
                // activate any hotspots that emerge while mouse is held down
                if( !mascot.getHotspots( ).isEmpty( ) )
                {
                    for( final Hotspot hotspot : mascot.getHotspots( ) )
                    {
                        if( hotspot.contains( mascot, mascot.getCursorPosition( ) ) )
                        {
                            // activate hotspot
                            hotspotIsActive = HotspotResult.ACTIVE_NULL;
                            try
                            {
                                // no need to set cursor position, it's already set
                                if( hotspot.getBehaviour( ) != null )
                                {
                                    hotspotIsActive = HotspotResult.ACTIVE;
                                    getMascot( ).setBehavior( configuration.buildBehavior( hotspot.getBehaviour( ), mascot ) );
                                }
                            }
                            catch( final BehaviorInstantiationException e )
                            {
                                throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedInitialiseFollowingBehaviourErrorMessage" ), e );
                            }
                            break;
                        }
                    }
                }

                if( hotspotIsActive == HotspotResult.INACTIVE )
                {
                    getMascot( ).setCursorPosition( null );
                }
            }

            if( hotspotIsActive != HotspotResult.ACTIVE )
            {
                if( getAction( ).hasNext( ) )
                {
                    if( ( getMascot( ).getBounds( ).getX( ) + getMascot( ).getBounds( ).getWidth( )
                            <= getEnvironment( ).getScreen( ).getLeft() )
                            || ( getEnvironment( ).getScreen( ).getRight( ) <= getMascot( ).getBounds( ).getX( ) )
                            || ( getEnvironment( ).getScreen( ).getBottom( ) <= getMascot( ).getBounds( ).getY( ) ) )
                    {

                        if( Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Multiscreen", "true" ) ) )
                        {
                            getMascot( ).setAnchor( new Point( (int)( Math.random( ) * ( getEnvironment( ).getScreen( ).getRight( ) - getEnvironment( ).getScreen( ).getLeft( ) ) ) + getEnvironment( ).getScreen( ).getLeft( ),
                                                              getEnvironment( ).getScreen( ).getTop( ) - 256 ) );
                        }
                        else
                        {
                            getMascot( ).setAnchor( new Point( (int)( Math.random( ) * ( getEnvironment( ).getWorkArea( ).getRight( ) - getEnvironment( ).getWorkArea( ).getLeft( ) ) ) + getEnvironment( ).getWorkArea( ).getLeft( ),
                                                              getEnvironment( ).getWorkArea( ).getTop( ) - 256 ) );
                        }

                        try
                        {
                            getMascot( ).setBehavior( this.getConfiguration( ).buildBehavior( configuration.getSchema( ).getString( BEHAVIOURNAME_FALL ) ) );
                        }
                        catch( final BehaviorInstantiationException e )
                        {
                            throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedFallingActionInitialiseErrorMessage" ), e );
                        }
                    }
                }
                else
                {

                    try
                    {
                        getMascot( ).setBehavior( this.getConfiguration( ).buildNextBehavior( getName( ), getMascot( ) ) );
                    }
                    catch( final BehaviorInstantiationException e )
                    {
                        throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedInitialiseFollowingActionsErrorMessage" ), e );
                    }
                }
            }
        }
        catch( final LostGroundException e )
        {

            try
            {
                getMascot( ).setCursorPosition( null );
                getMascot( ).setDragging( false );
                getMascot( ).setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( BEHAVIOURNAME_FALL ) ) );
            }
            catch( final BehaviorInstantiationException ex )
            {
                throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedFallingActionInitialiseErrorMessage" ), e );
            }
        }
        catch( final VariableException e )
        {
            throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getProperty( "VariableEvaluationErrorMessage" ), e );
        }
    }

    private void setMascot( final Mascot mascot )
    {
        this.mascot = mascot;
    }

    private Mascot getMascot( )
    {
        return this.mascot;
    }

    protected MascotEnvironment getEnvironment( )
    {
        return getMascot( ).getEnvironment( );
    }
}
