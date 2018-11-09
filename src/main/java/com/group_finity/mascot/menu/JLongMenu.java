package com.group_finity.mascot.menu;

import com.group_finity.mascot.Main;
import javax.swing.*;
import java.awt.*;
 
/**
 * A subclass of JMenu which adds a simple heuristic for ensuring
 * that the popup menu gets placed onscreen.
 *
 * IMPORTANT: This only supports FIXED menus, that get only additions of JMenuItems!
 * If you like to remove items from the menu in run-time, or add other types of components,
 * it needs to be developed! (but this is good for most of the cases).
 *
 * @author Moti Pinhassi, 10-Apr-2002.
 * Location change is based on Sun's workaround for bug 4236438.
 */
 
public class JLongMenu extends JMenu {
    JLongMenu moreMenu = null;
    int maxItems = 15; // default
 
    public JLongMenu(String label) {
        super(label);
        JMenuItem getHeightMenu = new JMenuItem("Temporary");
        int menuItemHeight = getHeightMenu.getPreferredSize().height;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
 
        maxItems = screenHeight / menuItemHeight - 2;
        // leave one for the "more" menu and one for the windows task bar.
    }
    
    public JLongMenu(String label, int maxitems) {
        super(label);
        this.maxItems = maxitems;
        // leave one for the "more" menu and one for the windows task bar.
    }    
 
    // We override this method just so we can call our version of
    // getPopupMenuOrigin. It is pretty much just a copy of
    // JMenu.setPopupMenuVisible
 
    public void setPopupMenuVisible(boolean b) {
        if (!isEnabled())
            return;
        boolean isVisible = isPopupMenuVisible();
        if (b != isVisible) {
            // We can't call ensurePopupMenuCreated() since it is private so
            // we call a method that calls it. (Sneaky huh?).
            isPopupMenuVisible();
            // Set location of popupMenu (pulldown or pullright)
            //  Perhaps this should be dictated by L&F
            if ((b==true) && isShowing()) {
                Point p = getPopupMenuOrigin();
                getPopupMenu().show(this, p.x, p.y);
            } else {
                getPopupMenu().setVisible(false);
            }
        }
    }
 
    /**
     * Compute the origin for the JMenu's popup menu.
     *
     * @returns a Point in the coordinate space of the menu instance
     * which should be used as the origin of the JMenu's popup menu.
     */
    protected Point getPopupMenuOrigin() {
        int x = 0;
        int y = 0;
        JPopupMenu pm = getPopupMenu();
        // Figure out the sizes needed to caclulate the menu position
        Dimension screenSize =Toolkit.getDefaultToolkit().getScreenSize();
        Dimension s = getSize();
        Dimension pmSize = pm.getSize();
        // For the first time the menu is popped up,
        // the size has not yet been initiated
        if (pmSize.width==0) {
            pmSize = pm.getPreferredSize();
        }
        Point position = getLocationOnScreen();
 
        Container parent = getParent();
        if (parent instanceof JPopupMenu) {
            // We are a submenu (pull-right)
 
            // if( SwingUtilities.isLeftToRight(this) ) { // Package private.
            if( getComponentOrientation() == ComponentOrientation.LEFT_TO_RIGHT ) {
                // First determine x:
                if (position.x+s.width + pmSize.width < screenSize.width) {
                    x = s.width;         // Prefer placement to the right
                } else {
                    x = 0-pmSize.width;  // Otherwise place to the left
                }
            } else {
                // First determine x:
                if (position.x < pmSize.width) {
                    x = s.width;         // Prefer placement to the right
                } else {
                    x = 0-pmSize.width;  // Otherwise place to the left
                }
            }
            // Then the y:
            if (position.y+pmSize.height < screenSize.height) {
                y = 0;                       // Prefer dropping down
            } else {
                y = s.height-pmSize.height;  // Otherwise drop 'up'
                if(y < 0-position.y)
                    y = 0-position.y;
            }
        } else {
            // We are a toplevel menu (pull-down)
 
            // if( SwingUtilities.isLeftToRight(this) ) { // Package private.
            if( getComponentOrientation() == ComponentOrientation.LEFT_TO_RIGHT ) {
                // First determine the x:
                if (position.x+pmSize.width < screenSize.width) {
                    x = 0;                     // Prefer extending to right
                } else {
                    x = s.width-pmSize.width;  // Otherwise extend to left
                }
            } else {
                // First determine the x:
                if (position.x+s.width < pmSize.width) {
                    x = 0;                     // Prefer extending to right
                } else {
                    x = s.width-pmSize.width;  // Otherwise extend to left
                }
            }
            // Then the y:
            if (position.y+s.height+pmSize.height < screenSize.height) {
                y = s.height;          // Prefer dropping down
            } else {
                y = 0-pmSize.height;   // Otherwise drop 'up'
                if(y < 0-position.y)
                    y = 0-position.y;
            }
        }
        return new Point(x,y);
    }
 
    public JMenuItem add (JMenuItem item) {
        if (moreMenu != null) {
            // We already have a more menu - add it there.
            return moreMenu.add(item);
        }
 
        if (getItemCount() < maxItems) {
            // We don't go over the limit - just add it.
            return super.add(item);
        }
 
        // If we reached here, we reached the limit and we don't have a more menu.
        // Lets create it and add the item there.
        moreMenu = new JLongMenu( Main.getInstance( ).getLanguageBundle( ).getProperty( "More" ), maxItems);
 
        super.add (moreMenu);
        return moreMenu.add(item);
    }

}
