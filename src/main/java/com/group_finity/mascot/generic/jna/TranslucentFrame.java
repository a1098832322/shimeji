package com.group_finity.mascot.generic.jna;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JWindow;

import com.sun.jna.examples.WindowUtils;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class TranslucentFrame extends JWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private float alpha = 1.0f;

	public TranslucentFrame() {
		super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
		this.init();
	}

	private void init() {
		System.setProperty("sun.java2d.noddraw", "true");
		System.setProperty("sun.java2d.opengl", "true");
	}

	@Override
	public void setVisible(final boolean b) {
		super.setVisible(b);
		if (b) {
			WindowUtils.setWindowTransparent(this, true);
		}
	}

	@Override
	protected void addImpl(final Component comp, final Object constraints, final int index) {
		super.addImpl(comp, constraints, index);
		if (comp instanceof JComponent) {
			final JComponent jcomp = (JComponent) comp;
			jcomp.setOpaque(false);
		}
	}

	public void setAlpha(final float alpha) {
		WindowUtils.setWindowAlpha(this, alpha);
	}

	public float getAlpha() {
		return this.alpha;
	}

}
