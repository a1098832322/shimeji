package com.group_finity.mascot;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.sound.sampled.Clip;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.glados.GladosEventReporter;
import com.group_finity.mascot.glados.GladosOptions;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.menu.JLongMenu;
import com.group_finity.mascot.script.VariableMap;
import com.group_finity.mascot.sound.Sounds;

/**
 * Mascot object.
 *
 * The mascot represents the long-term, complex behavior and (@link Behavior),
 * Represents a short-term movements in the monotonous work with (@link Action).
 *
 * The mascot they have an internal timer, at a constant interval to call (@link Action).
 * (@link Action) is (@link #animate (Point, MascotImage, boolean)) method or by calling
 * To animate the mascot.
 *
 * (@link Action) or exits, the other at a certain time is called (@link Behavior), the next move to (@link Action).
 *
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Mascot
{
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(Mascot.class);

    private static AtomicInteger lastId = new AtomicInteger( );

    private final int id;

    private String imageSet = "";
    /**
     * A window that displays the mascot.
     */
    private final TranslucentWindow window = NativeFactory.getInstance( ).newTransparentWindow( );

    /**
     * Managers are managing the mascot.
     */
    private Manager manager = null;

    /**
     * Mascot ground coordinates.
     * Or feet, for example, when part of the hand is hanging.
     */
    private Point anchor = new Point( 0, 0 );

    /**
     * Image to display.
     */
    private MascotImage image = null;

    /**
     * Whether looking right or left.
     * The original image is treated as left, true means picture must be inverted.
     */
    private boolean lookRight = false;

    /**
     * Object representing the long-term behavior.
     */
    private Behavior behavior = null;

    /**
     * Increases with each tick of the timer.
     */
    private int time = 0;

    /**
     * Whether the animation is running.
     */
    private boolean animating = true;

    private boolean paused = false;
    
    /**
     * Set by behaviours when the shimeji is being dragged by the mouse cursor, 
     * as opposed to hotspots or the like.
     */
    private boolean dragging = false;

    /** 左键按下时的屏幕坐标，用于区分「点击」与「拖动」。null 表示当前没有按下过。 */
    private Point pressPoint = null;

    /** 位移超过该像素数视为拖动，不当作点击。取 4px 容忍手抖。 */
    private static final int CLICK_SLOP_PX = 4;

    private MascotEnvironment environment = new MascotEnvironment( this );

    private String sound = null;
    
    protected DebugWindow debugWindow = null;
    
    private ArrayList<String> affordances = new ArrayList( 5 );
    
    private ArrayList<Hotspot> hotspots = new ArrayList( 5 );
    
    /**
     * Set by behaviours when the user has triggered a hotspot on this shimeji, 
     * so that the shimeji knows to check for any new hotspots that emerge while 
     * the mouse is held down.
     */
    private Point cursor = null;
    
    private VariableMap variables = null;

    public Mascot( final String imageSet )
    {
        id = lastId.incrementAndGet( );
        this.imageSet = imageSet;

        log.info("Created a mascot ({})", this);

        // Always show on top
        getWindow( ).setAlwaysOnTop( true );

        // Register the mouse handler
        getWindow( ).asComponent( ).addMouseListener( new MouseAdapter( )
        {
            @Override
            public void mousePressed( final MouseEvent e )
            {
                Mascot.this.mousePressed( e );
            }

            @Override
            public void mouseReleased( final MouseEvent e )
            {
                Mascot.this.mouseReleased( e );
            }
        } );
        getWindow( ).asComponent( ).addMouseMotionListener( new MouseMotionListener( )
        {
            @Override
            public void mouseMoved( final MouseEvent e )
            {
                if( paused )
                    refreshCursor( false );
                else
                {
                    if( isHotspotClicked( ) )
                        setCursorPosition( e.getPoint( ) );
                    else
                        refreshCursor( e.getPoint( ) );
                }
            }

            @Override
            public void mouseDragged( final MouseEvent e )
            {
                if( paused )
                    refreshCursor( false );
                else
                {
                    if( isHotspotClicked( ) )
                        setCursorPosition( e.getPoint( ) );
                    else
                        refreshCursor( e.getPoint( ) );
                }
            }
        } );
    }

    @Override
    public String toString( )
    {
        return "mascot" + id;
    }

    private void mousePressed( final MouseEvent event )
    {
        // 记录按下点，供 mouseReleased 判断这是一次点击还是拖动。
        // 必须在 behavior.mousePressed 之前记录——那里可能改变坐标。
        if( event.getButton( ) == MouseEvent.BUTTON1 )
        {
            pressPoint = event.getLocationOnScreen( );
        }

        // Switch to drag the animation when the mouse is down
        if( !isPaused( ) && getBehavior( ) != null )
        {
            try
            {
                getBehavior( ).mousePressed( event );
            }
            catch( final CantBeAliveException e )
            {
                log.error("Fatal Error", e);
                Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "SevereShimejiErrorErrorMessage" ));
                dispose( );
            }
        }
    }

    private void mouseReleased(final MouseEvent event)
    {
        // GLaDOS 托管模式：不提供右键菜单。
        // 原版菜单里有「增加一个/克隆」等项，会绕过 Breeding 与行为表约束直接
        // 调用 Main.createMascot，让用户手动生成多个角色；同时菜单还会打开
        // Shimeji 自己的设置窗口，绕过 GLaDOS 设置页。
        // 托管下把点击交给 GLaDOS 处理（点击唤出桌宠对话），不复用原版菜单。
        if( GladosOptions.isManagedProcess( ) )
        {
            if( event.getButton( ) == MouseEvent.BUTTON3 || event.isPopupTrigger( ) )
            {
                return;
            }
        }

        // macOS 上 isPopupTrigger() 不会触发，需要检查右键按钮
        if( event.isPopupTrigger( ) || event.getButton( ) == MouseEvent.BUTTON3 )
        {
            SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    showPopup( event.getX( ), event.getY( ) );
                }
            } );
        }
        else
        {
            if( !isPaused( ) && getBehavior( ) != null )
            {
                try
                {
                    getBehavior( ).mouseReleased( event );
                }
                catch( final CantBeAliveException e )
                {
                    log.error("Fatal Error", e);
                    Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "SevereShimejiErrorErrorMessage" ) );
                    dispose( );
                }
            }

            // GLaDOS 托管模式：把「点一下」上报给宿主，用于唤出桌宠对话。
            // 注意不能判断 isDragging()：UserBehavior.mouseReleased 内部会先
            // setDragging(false)，到这里已经归零，拖拽松手会被误判成点击。
            // 改为比较按下与释放的屏幕坐标——移动距离超过阈值就算拖拽。
            if( GladosOptions.isManagedProcess( )
                && event.getButton( ) == MouseEvent.BUTTON1
                && !isDragGesture( event ) )
            {
                GladosEventReporter.report( GladosEventReporter.EVENT_CLICK );
            }
            // 本次手势结束，清空按下点，避免下一次释放沿用旧坐标
            pressPoint = null;
        }
    }

    /** 本次按下到释放的位移是否构成拖拽（用于区分点击与拖动）。 */
    private boolean isDragGesture( final MouseEvent event )
    {
        if( pressPoint == null )
        {
            // 没有记录到按下点，保守认为不是点击，避免误开对话
            return true;
        }
        final Point current = event.getLocationOnScreen( );
        final int dx = current.x - pressPoint.x;
        final int dy = current.y - pressPoint.y;
        return Math.hypot( dx, dy ) > CLICK_SLOP_PX;
    }

    private void showPopup( final int x, final int y )
    {
        final JPopupMenu popup = new JPopupMenu( );
        final Properties languageBundle = Main.getInstance( ).getLanguageBundle( );

        popup.addPopupMenuListener( new PopupMenuListener( )
        {
            @Override
            public void popupMenuCanceled( final PopupMenuEvent e )
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible( final PopupMenuEvent e )
            {
                setAnimating( true );
            }

            @Override
            public void popupMenuWillBecomeVisible( final PopupMenuEvent e )
            {
                setAnimating( false );
            }
        } );

        // "Another One!" menu item
        final JMenuItem increaseMenu = new JMenuItem( languageBundle.getProperty( "CallAnother" ) );
        increaseMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                Main.getInstance( ).createMascot( imageSet );
            }
        } );

        // "Bye Bye!" menu item
        final JMenuItem disposeMenu = new JMenuItem( languageBundle.getProperty( "Dismiss" ) );
        disposeMenu.addActionListener( new ActionListener( )
        {
            @Override
            public void actionPerformed( final ActionEvent e )
            {
                dispose( );
            }
        } );

        // "Follow Mouse!" Menu item
        final JMenuItem gatherMenu = new JMenuItem( languageBundle.getProperty( "FollowCursor" ) );
        gatherMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                getManager( ).setBehaviorAll( Main.getInstance( ).getConfiguration(imageSet), Main.BEHAVIOR_GATHER, imageSet );
            }
        } );

        // "Reduce to One!" menu item
        final JMenuItem oneMenu = new JMenuItem( languageBundle.getProperty( "DismissOthers" ) );
        oneMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                getManager( ).remainOne( imageSet );
            }
        } );

        // "Reduce to One!" menu item
        final JMenuItem onlyOneMenu = new JMenuItem( languageBundle.getProperty( "DismissAllOthers" ) );
        onlyOneMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                getManager( ).remainOne( Mascot.this );
            }
        } );

        // "Restore IE!" menu item
        final JMenuItem restoreMenu = new JMenuItem( languageBundle.getProperty( "RestoreWindows" ) );
        restoreMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                NativeFactory.getInstance( ).getEnvironment( ).restoreIE( );
            }
        } );

        // Debug menu item
        final JMenuItem debugMenu = new JMenuItem( languageBundle.getProperty( "RevealStatistics" ) );
        debugMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                if( debugWindow == null )
                {
                    debugWindow = new DebugWindow( );
                    //debugWindow.setIcon
                }
                debugWindow.setVisible( true );
            }
        } );
        
        // "Bye Everyone!" menu item
        final JMenuItem closeMenu = new JMenuItem( languageBundle.getProperty( "DismissAll" ) );
        closeMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent e )
            {
                Main.getInstance( ).exit( );
            }
        } );

        // "Paused" Menu item
        final String pauseText = isAnimating( ) ? languageBundle.getProperty( "PauseAnimations" ) : languageBundle.getProperty( "ResumeAnimations" );
        final JMenuItem pauseMenu = new JMenuItem( pauseText );
        pauseMenu.addActionListener( new ActionListener( )
        {
            public void actionPerformed( final ActionEvent event )
            {
                Mascot.this.setPaused( !Mascot.this.isPaused( ) );
            }
        } );

        // Add the Behaviors submenu.  Currently slightly buggy, sometimes the menu ghosts.
        JLongMenu submenu = new JLongMenu( languageBundle.getProperty( "SetBehaviour" ), 30 );
        JLongMenu allowedSubmenu = new JLongMenu( languageBundle.getProperty( "AllowedBehaviours" ), 30 );
        // The MenuScroller would look better than the JLongMenu, but the initial positioning is not working correctly.
        //MenuScroller.setScrollerFor(submenu, 30, 125);
        submenu.setAutoscrolls( true );
        JMenuItem item;
        JCheckBoxMenuItem toggleItem;
        final com.group_finity.mascot.config.Configuration config = Main.getInstance( ).getConfiguration( getImageSet( ) );
        for( String behaviorName : config.getBehaviorNames( ) )
        {
            final String command = behaviorName;
            try
            {
                if( !config.isBehaviorHidden( command ) )
                {
                    String caption = behaviorName.replaceAll( "([a-z])(IE)?([A-Z])", "$1 $2 $3" ).replaceAll( "  ", " " );
                    if( config.isBehaviorEnabled( command, Mascot.this ) && !command.contains( "/" ) )
                    {
                        item = new JMenuItem( languageBundle.containsKey( behaviorName ) ? 
                                              languageBundle.getProperty( behaviorName ) : 
                                              caption );
                        item.addActionListener( new ActionListener( )
                        {
                            @Override
                            public void actionPerformed( final ActionEvent e )
                            {
                                try
                                {	
                                    setBehavior( config.buildBehavior( command ) );
                                }
                                catch( Exception err )
                                {
                                    log.error("Error ({})", this);
                                    Main.showError( languageBundle.getProperty( "CouldNotSetBehaviourErrorMessage" ) );
                                }
                            }
                        } );
                        submenu.add( item );
                    }
                    
                    if( config.isBehaviorToggleable( command ) && !command.contains( "/" ) )
                    {
                        toggleItem = new JCheckBoxMenuItem( caption, config.isBehaviorEnabled( command, Mascot.this ) );
                        toggleItem.addItemListener( new ItemListener( )
                        {
                            public void itemStateChanged( final ItemEvent e )
                            {
                                // TODO: Implement setMascotBehaviorEnabled
                                // Main.getInstance( ).setMascotBehaviorEnabled( command, Mascot.this, !config.isBehaviorEnabled( command, Mascot.this ) );
                            }
                        } );
                        allowedSubmenu.add( toggleItem );
                    }
                }
            }
            catch( Exception err )
            {
                // just skip if something goes wrong
            }
        }

        popup.add( increaseMenu );
        popup.add( new JSeparator( ) );
        popup.add( gatherMenu );
        popup.add( restoreMenu );
        popup.add( debugMenu );
        popup.add( new JSeparator( ) );
        if( submenu.getMenuComponentCount( ) > 0 )
            popup.add( submenu );
        if( allowedSubmenu.getMenuComponentCount( ) > 0 )
            popup.add( allowedSubmenu );
        popup.add( new JSeparator( ) );
        popup.add( pauseMenu );
        popup.add( new JSeparator( ) );
        popup.add( disposeMenu );
        popup.add( oneMenu );
        popup.add( onlyOneMenu );
        popup.add( closeMenu );

        getWindow( ).asComponent( ).requestFocus( );
                
        // lightweight popups expect the shimeji window to draw them if they fall inside the shimeji window boundary
        // as the shimeji window can't support this we need to set them to heavyweight
        popup.setLightWeightPopupEnabled( false );
        popup.show( getWindow( ).asComponent( ), x, y );
    }

    void tick( )
    {
        if( isAnimating( ) )
        {
            if( getBehavior( ) != null )
            {
                try
                {
                    getBehavior( ).next( );
                }
                catch( final CantBeAliveException e )
                {
                    log.error("Fatal Error.", e);
                    Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "CouldNotGetNextBehaviourErrorMessage" ) );
                    dispose( );
                }

                setTime( getTime( ) + 1 );
            }
            
            if( debugWindow != null )
            {
                debugWindow.setBehaviour( behavior.toString( ).substring( 9, behavior.toString( ).length( ) - 1 ).replaceAll( "([a-z])(IE)?([A-Z])", "$1 $2 $3" ).replaceAll( "  ", " " ) );
                debugWindow.setShimejiX( anchor.x );
                debugWindow.setShimejiY( anchor.y );
                
                Area activeWindow = environment.getActiveIE( );
                debugWindow.setWindowTitle( environment.getActiveIETitle( ) );
                debugWindow.setWindowX( activeWindow.getLeft( ) );
                debugWindow.setWindowY( activeWindow.getTop( ) );
                debugWindow.setWindowWidth( activeWindow.getWidth( ) );
                debugWindow.setWindowHeight( activeWindow.getHeight( ) );
                
                Area workArea = environment.getWorkArea( );
                debugWindow.setEnvironmentX( workArea.getLeft( ) );
                debugWindow.setEnvironmentY( workArea.getTop( ) );
                debugWindow.setEnvironmentWidth( workArea.getWidth( ) );
                debugWindow.setEnvironmentHeight( workArea.getHeight( ) );
                
                // 设置 DPI 缩放信息
                double scaleFactor = com.group_finity.mascot.util.DPIScaler.getSystemScaleFactor();
                debugWindow.setDPIScaleFactor(scaleFactor);
            }
        }
    }

    public void apply( )
    {
        if (isAnimating())
        {
            // Make sure there's an image
            if (getImage() != null)
            {
                // Set the window region
                getWindow().asComponent().setBounds(getBounds());

                // 角色移动上报给 GLaDOS（带节流）：宿主靠它实时移动回复气泡。
                // 只在受管模式下生效；非受管模式 isEnabled() 返回 false，零开销。
                if (GladosOptions.isManagedProcess()) {
                    Rectangle bounds = getBounds();
                    GladosEventReporter.reportMascotMove(bounds.x, bounds.y, bounds.width, bounds.height);
                }

                // Set Images
                getWindow().setImage(getImage().getImage());

                // Display
                if (!getWindow().asComponent().isVisible())
                {
                    getWindow().asComponent().setVisible(true);
                }

                // Redraw
                getWindow().updateImage();
            }
            else
            {
                if( getWindow().asComponent().isVisible() )
                {
                    getWindow().asComponent().setVisible(false);
                }
            }
            
            // play sound if requested
            if( !Sounds.isMuted( ) && sound != null && Sounds.contains( sound ) )
            {
                synchronized( log )
                {
                    Clip clip = Sounds.getSound( sound );
                    if( !clip.isRunning( ) )
                    {
                        clip.stop( );
                        clip.setMicrosecondPosition( 0 );
                        clip.start( );
                    }
                }
            }
        }
    }

    public void dispose( )
    {
        log.info("destroy mascot ({})", this);
        
        if( debugWindow != null )
        {
            debugWindow.setVisible( false );
            debugWindow = null;
        }

        animating = false;
        getWindow( ).dispose( );
        affordances.clear( );
        if( getManager( ) != null )
        {
            getManager( ).remove( Mascot.this );
        }
    }
    
    /**
     * 打开 DebugWindow（用于程序启动时默认显示）
     */
    public void openDebugWindow()
    {
        if( debugWindow == null )
        {
            debugWindow = new DebugWindow( );
        }
        debugWindow.setVisible( true );
    }
        
    private void refreshCursor( Point position )
    {
        boolean useHand = false;
        for( final Hotspot hotspot : hotspots )
        {
            if( hotspot.contains( this, position ) && 
                Main.getInstance( ).getConfiguration( imageSet ).isBehaviorEnabled( hotspot.getBehaviour( ), this ) )
            {
                useHand = true;
                break;
            }
        }

        refreshCursor( useHand );
    }
    
    private void refreshCursor( Boolean useHand )
    {
        getWindow( ).asComponent( ).setCursor( Cursor.getPredefinedCursor( useHand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR ) );
    }

    public Manager getManager( )
    {
        return manager;
    }

    public void setManager( final Manager manager )
    {
        this.manager = manager;
    }

    public Point getAnchor( )
    {
        return anchor;
    }

    public void setAnchor( Point anchor )
    {
        this.anchor = anchor;
    }

    public MascotImage getImage( )
    {
        return image;
    }

    public void setImage( final MascotImage image )
    {
        this.image = image;
    }

    public boolean isLookRight( )
    {
        return lookRight;
    }

    public void setLookRight( final boolean lookRight )
    {
        this.lookRight = lookRight;
    }

    public Rectangle getBounds( )
    {
        if( getImage( ) != null )
        {
            // Central area of the window find the image coordinates and ground coordinates. The centre has already been adjusted for scaling
            final int top = getAnchor( ).y - getImage( ).getCenter( ).y;
            final int left = getAnchor( ).x - getImage( ).getCenter( ).x;

            final Rectangle result = new Rectangle( left, top, getImage( ).getSize( ).width, getImage( ).getSize( ).height );

            return result;
        }
        else
        {
            // as we have no image let's return what we were last frame
            return getWindow( ).asComponent( ).getBounds( );
        }
    }

    public int getTime( )
    {
        return time;
    }

    private void setTime( final int time )
    {
        this.time = time;
    }

    public Behavior getBehavior( )
    {
        return behavior;
    }

    public void setBehavior( final Behavior behavior ) throws CantBeAliveException
    {
        this.behavior = behavior;
        this.behavior.init( this );
    }

    public int getCount( )
    {
        return manager != null ? getManager( ).getCount( imageSet ) : 0;
    }

    public int getTotalCount( )
    {
        return manager != null ? getManager( ).getCount( ) : 0;
    }

    private boolean isAnimating( )
    {
        return animating && !paused;
    }

    private void setAnimating( final boolean animating )
    {
        this.animating = animating;
    }

    private TranslucentWindow getWindow( )
    {
        return this.window;
    }

    public MascotEnvironment getEnvironment( )
    {
        return environment;
    }

    public ArrayList<String> getAffordances( )
    {
	return affordances;
    }

    public ArrayList<Hotspot> getHotspots( )
    {
	return hotspots;
    }
        
    public void setImageSet( final String set )
    {
        imageSet = set;
    }

    public String getImageSet( )
    {
        return imageSet;
    }

    public String getSound( )
    {
        return sound;
    }

    public void setSound( final String name )
    {
        sound = name;
    }
    
    public boolean isPaused( )
    {
        return paused;
    }
    
    public void setPaused( final boolean paused )
    {
        this.paused = paused;
    }

    public boolean isDragging( )
    {
        return dragging;
    }

    public void setDragging( final boolean isDragging )
    {
        dragging = isDragging;
    }

    public boolean isHotspotClicked( )
    {
        return cursor != null;
    }
    
    public Point getCursorPosition( )
    {
        return cursor;
    }

    public void setCursorPosition( final Point point )
    {
        cursor = point;
        
        if( point == null )
            refreshCursor( false );
        else
            refreshCursor( point );
    }

    public VariableMap getVariables( )
    {
        if( variables == null )
            variables = new VariableMap( );
        return variables;
    }
}
