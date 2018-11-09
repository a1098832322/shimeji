package com.group_finity.mascot.imagesetchooser;

import javax.swing.*;
import java.awt.*;

/**
 * A JList that can be populated with ImageSetChooserPanel objects
 */

public class ShimejiList extends JList {

  public ShimejiList() {
    setCellRenderer(new CustomCellRenderer());
  }

  class CustomCellRenderer implements ListCellRenderer {
    public Component getListCellRendererComponent (JList list, Object value,
			int index,boolean isSelected,boolean cellHasFocus) {
      if (value instanceof ImageSetChooserPanel)
      {
        ImageSetChooserPanel component = (ImageSetChooserPanel) value;
        component.setForeground (Color.white);
        component.setBackground (isSelected ? SystemColor.controlHighlight : Color.white);
        component.setCheckbox( isSelected );
        return component;
      }
      else
      {
        return new JLabel("???");
      }
    }
  }
}