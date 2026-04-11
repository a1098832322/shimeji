package com.group_finity.mascot.environment;

import java.awt.Point;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class Wall implements Border {

	private Area area;

	private boolean right;

	public Wall(final Area area, final boolean right) {
		this.area = area;
		this.right = right;
	}

	public Area getArea() {
		return this.area;
	}

	public boolean isRight() {
		return this.right;
	}

	public int getX() {
		return isRight() ? getArea().getRight() : getArea().getLeft();
	}

	public int getTop() {
		return getArea().getTop();
	}

	public int getBottom() {
		return getArea().getBottom();
	}

	public int getDX() {
		return isRight() ? getArea().getDright() : getArea().getDleft();
	}

	public int getDTop() {
		return getArea().getDtop();
	}

	public int getDBottom() {
		return getArea().getDbottom();
	}
	
	public int getHeight() {
		return getArea().getHeight();
	}

	@Override
	public boolean isOn(final Point location) {
		// 高 DPI 环境下需要使用容差比较，避免舍入误差导致碰撞检测失败
		// 例如：在 200% 缩放下，坐标可能出现 0.5 的偏差
		final int TOLERANCE = 2; // 允许 2 像素的误差
		return getArea().isVisible() && 
		       Math.abs(getX() - location.x) <= TOLERANCE && 
		       (getTop() <= location.y) &&
		       (location.y <= getBottom());
	}

	public Point move(final Point location) {

		if (!getArea().isVisible()) {
			return location;
		}
		
		final int d = getBottom() - getDBottom() - (getTop() - getDTop());
		if ( d==0 ) {
			return location;
		}

		final Point newLocation = new Point(location.x + getDX(), (location.y - (getTop() - getDTop()))
				* (getBottom() - getTop()) / d + getTop());

		if ((Math.abs(newLocation.x - location.x) >= 80) || (Math.abs(newLocation.y - location.y) >= 80)) {
			return location;
		}
		return newLocation;
	}
}
