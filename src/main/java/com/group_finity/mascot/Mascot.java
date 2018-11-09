package com.group_finity.mascot;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.sound.Sounds;

/**
 * Mascot object.
 * <p>
 * The mascot represents the long-term, complex behavior and (@link Behavior),
 * Represents a short-term movements in the monotonous work with (@link Action).
 * <p>
 * The mascot they have an internal timer, at a constant interval to call (@link Action).
 * (@link Action) is (@link #animate (Point, MascotImage, boolean)) method or by calling
 * To animate the mascot.
 * <p>
 * (@link Action) or exits, the other at a certain time is called (@link Behavior), the next move to (@link Action).
 * <p>
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Mascot {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(Mascot.class.getName());

    private static AtomicInteger lastId = new AtomicInteger();

    private final int id;

    private String imageSet = "";

    /**
     * A window that displays the mascot.
     */
    private final TranslucentWindow window = NativeFactory.getInstance().newTransparentWindow();

    /**
     * Managers are managing the mascot.
     */
    private Manager manager = null;

    /**
     * Mascot ground coordinates.
     * Or feet, for example, when part of the hand is hanging.
     */
    private Point anchor = new Point(0, 0);

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

    private MascotEnvironment environment = new MascotEnvironment(this);

    private String sound = null;

    protected DebugWindow debugWindow = null;

    public Mascot(final String imageSet) {
        this.id = lastId.incrementAndGet();
        this.imageSet = imageSet;

        log.log(Level.INFO, "Created a mascot ({0})", this);

        // Always show on top
        getWindow().asJWindow().setAlwaysOnTop(true);

        // Register the mouse handler
        getWindow().asJWindow().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                Mascot.this.mousePressed(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                Mascot.this.mouseReleased(e);
            }
        });

    }

    @Override
    public String toString() {
        return "mascot" + this.id;
    }

    private void mousePressed(final MouseEvent event) {

        // Switch to drag the animation when the mouse is down
        if (getBehavior() != null) {
            try {
                getBehavior().mousePressed(event);
            } catch (final CantBeAliveException e) {
                log.log(Level.SEVERE, "Fatal Error", e);
                Main.showError(Main.getInstance().getLanguageBundle().getProperty("SevereShimejiErrorErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getProperty("SeeLogForDetails"));
                dispose();
            }
        }

    }

    private void mouseReleased(final MouseEvent event) {

        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    showPopup(event.getX(), event.getY());
                }
            });
        } else {
            if (getBehavior() != null) {
                try {
                    getBehavior().mouseReleased(event);
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Fatal Error", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getProperty("SevereShimejiErrorErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getProperty("SeeLogForDetails"));
                    dispose();
                }
            }
        }

    }

    public void showPopup(final int x, final int y) {
        final JPopupMenu popup = new JPopupMenu();

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                setAnimating(true);
            }

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                setAnimating(false);
            }
        });

        // "Another One!" menu item
        final JMenuItem increaseMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("CallAnother"));
        increaseMenu.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                Main.getInstance().createMascot(imageSet);
            }
        });

        // "Bye Bye!" menu item
        final JMenuItem disposeMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("Dismiss"));
        disposeMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dispose();
            }
        });

        // "Follow Mouse!" Menu item
        final JMenuItem gatherMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("FollowCursor"));
        gatherMenu.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                getManager().setBehaviorAll(Main.getInstance().getConfiguration(imageSet), Main.BEHAVIOR_GATHER, imageSet);
            }
        });

        // "Reduce to One!" menu item
        final JMenuItem oneMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("DismissOthers"));
        oneMenu.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                getManager().remainOne(imageSet);
            }
        });

        // "Restore IE!" menu item
        final JMenuItem restoreMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("RestoreWindows"));
        restoreMenu.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                NativeFactory.getInstance().getEnvironment().restoreIE();
            }
        });

        // Debug menu item
        final JMenuItem debugMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("RevealStatistics"));
        debugMenu.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                if (debugWindow == null) {
                    debugWindow = new DebugWindow();
                }
                debugWindow.setVisible(true);
            }
        });

        // Solo mode menu item
        /*final JMenuItem soloMenu = new JMenuItem("Solo Mode");
		debugMenu.addActionListener(new ActionListener( )
        {
			public void actionPerformed( final ActionEvent event )
            {
                // dismiss others
                window.setStayOnTop( true );
                //no reproduction = true
			}
		});*/

        // "Bye Everyone!" menu item
        final JMenuItem closeMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getProperty("DismissAll"));
        closeMenu.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Main.getInstance().exit();
            }
        });

        // Add the Behaviors submenu.  Currently slightly buggy, sometimes the menu ghosts.
        com.group_finity.mascot.menu.JLongMenu submenu = new com.group_finity.mascot.menu.JLongMenu(Main.getInstance().getLanguageBundle().getProperty("SetBehaviour"), 30);
        // The MenuScroller would look better than the JLongMenu, but the initial positioning is not working correctly.
        //MenuScroller.setScrollerFor(submenu, 30, 125);
        submenu.setAutoscrolls(true);
        JMenuItem item;
        com.group_finity.mascot.config.Configuration config = Main.getInstance().getConfiguration(getImageSet());
        Behavior behaviour = null;
        for (String behaviorName : config.getBehaviorNames()) {
            final String command = behaviorName;
            try {
                behaviour = Main.getInstance().getConfiguration(getImageSet()).buildBehavior(command);
                if (!behaviour.isHidden()) {
                    item = new JMenuItem(Main.getInstance().getLanguageBundle().containsKey(behaviorName) ?
                            Main.getInstance().getLanguageBundle().getProperty(behaviorName) :
                            behaviorName.replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll("  ", " "));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(final ActionEvent e) {
                            try {
                                //点击时进行操作
                                setBehavior(Main.getInstance().getConfiguration(getImageSet()).buildBehavior(command));
                            } catch (Exception err) {
                                log.log(Level.SEVERE, "Error ({0})", this);
                                Main.showError(Main.getInstance().getLanguageBundle().getProperty("CouldNotSetBehaviourErrorMessage") + "\n" + err.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getProperty("SeeLogForDetails"));
                            }
                        }
                    });
                    submenu.add(item);
                }
            } catch (Exception err) {
                // just skip if something goes wrong
            }
        }

        popup.add(increaseMenu);
        popup.add(new JSeparator());
        popup.add(gatherMenu);
        popup.add(restoreMenu);
        popup.add(debugMenu);
        //popup.add(soloMenu);
        popup.add(new JSeparator());
        popup.add(submenu);
        popup.add(new JSeparator());
        popup.add(disposeMenu);
        popup.add(oneMenu);
        popup.add(closeMenu);

        getWindow().asJWindow().requestFocus();

        popup.show(getWindow().asJWindow(), x, y);

    }

    public void tick() {
        if (isAnimating()) {
            if (getBehavior() != null) {

                try {
                    getBehavior().next();
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Fatal Error.", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getProperty("CouldNotGetNextBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getProperty("SeeLogForDetails"));
                    dispose();
                }

                setTime(getTime() + 1);
            }
            if (debugWindow != null) {
                debugWindow.setBehaviour(behavior.toString().substring(9, behavior.toString().length() - 1).replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll("  ", " "));
                debugWindow.setShimejiX(anchor.x);
                debugWindow.setShimejiY(anchor.y);
                debugWindow.setWindowX(environment.getActiveIE().getLeft());
                debugWindow.setWindowY(environment.getActiveIE().getTop());
                debugWindow.setWindowWidth(environment.getActiveIE().getWidth());
                debugWindow.setWindowHeight(environment.getActiveIE().getHeight());
            }
        }
    }

    public void apply() {
        if (isAnimating()) {
            // Make sure there's an image
            if (getImage() != null) {
                // Set the window region
                getWindow().asJWindow().setBounds(getBounds());

                // Set Images
                getWindow().setImage(getImage().getImage());

                // Display
                if (!getWindow().asJWindow().isVisible()) {
                    getWindow().asJWindow().setVisible(true);
                }

                // Redraw
                getWindow().updateImage();
            } else {
                if (getWindow().asJWindow().isVisible()) {
                    getWindow().asJWindow().setVisible(false);
                }
            }

            // play sound if requested
            if (sound != null && !Sounds.getSound(sound).isRunning() && !Sounds.isMuted()) {
                Sounds.getSound(sound).setMicrosecondPosition(0);
                Sounds.getSound(sound).start();
            }
        }
    }

    public void dispose() {
        log.log(Level.INFO, "destroy mascot ({0})", this);

        if (debugWindow != null) {
            debugWindow.setVisible(false);
            debugWindow = null;
        }

        getWindow().asJWindow().dispose();
        if (getManager() != null) {
            getManager().remove(Mascot.this);
        }
    }

    public Manager getManager() {
        return this.manager;
    }

    public void setManager(final Manager manager) {
        this.manager = manager;
    }

    public Point getAnchor() {
        return this.anchor;
    }

    public void setAnchor(Point anchor) {
        this.anchor = anchor;
    }

    public MascotImage getImage() {
        return this.image;
    }

    public void setImage(final MascotImage image) {
        this.image = image;
    }

    public boolean isLookRight() {
        return this.lookRight;
    }

    public void setLookRight(final boolean lookRight) {
        this.lookRight = lookRight;
    }

    public Rectangle getBounds() {
        // Central area of the window find the image coordinates and ground coordinates.
        final int top = getAnchor().y - getImage().getCenter().y;
        final int left = getAnchor().x - getImage().getCenter().x;

        final Rectangle result = new Rectangle(left, top, getImage().getSize().width, getImage().getSize().height);

        return result;
    }

    public int getTime() {
        return this.time;
    }

    public void setTime(final int time) {
        this.time = time;
    }

    public Behavior getBehavior() {
        return this.behavior;
    }

    public synchronized void setBehavior(final Behavior behavior) throws CantBeAliveException {

        this.behavior = behavior;
        if (behavior != null) {
            System.out.println("behavior:" + behavior.getName());
            this.behavior.init(this);
        }else{
            System.out.println("behavior is null" );
        }
    }

    public int getTotalCount() {
        return getManager().getCount();
    }

    public boolean isAnimating() {
        return this.animating;
    }

    public void setAnimating(final boolean animating) {
        this.animating = animating;
    }

    private TranslucentWindow getWindow() {
        return this.window;
    }

    public MascotEnvironment getEnvironment() {
        return environment;
    }

    public String getImageSet() {
        return imageSet;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(final String name) {
        sound = name;
    }
}
