package com.group_finity.mascot.environment;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class ComplexArea {

	private Map<String, Area> areas = new HashMap<String, Area>();

	public void set(Map<String, Rectangle> rectangles) {
		retain(rectangles.keySet());
		for (Map.Entry<String, Rectangle> e : rectangles.entrySet()) {
			set(e.getKey(), e.getValue());
		}
	}

	public void set(String name, final Rectangle value) {

		for (Area area : areas.values()) {
			if ( area.getLeft()==value.x &&
					area.getTop()==value.y &&
					area.getWidth()==value.width &&
					area.getHeight()==value.height ) {
				return;
			}
		}

		Area area = areas.get(name);
		if (area == null) {
			area = new Area();
			areas.put(name, area);
		}
		area.set(value);
	}

	public void retain(Collection<String> deviceNames) {

		for (Iterator<String> i = areas.keySet().iterator(); i.hasNext();) {
			String key = i.next();
			if (!deviceNames.contains(key)) {
				i.remove();
			}
		}
	}

	public FloorCeiling getBottomBorder(Point location) {

		FloorCeiling ret = null;

		for (Area area : areas.values()) {
			if (area.getBottomBorder().isOn(location)) {
				ret = area.getBottomBorder();
			}
		}

		for (Area area : areas.values()) {
			if (area.getTopBorder().isOn(location)) {
				ret = null;
			}
		}

		return ret;
	}

	public FloorCeiling getTopBorder(Point location) {

		FloorCeiling ret = null;

		for (Area area : areas.values()) {
			if (area.getTopBorder().isOn(location)) {
				ret = area.getTopBorder();
			}
		}

		for (Area area : areas.values()) {
			if (area.getBottomBorder().isOn(location)) {
				ret = null;
			}
		}

		return ret;
	}

	public Wall getLeftBorder(Point location) {

		Wall ret = null;

		for (Area area : areas.values()) {
			if (area.getLeftBorder().isOn(location)) {
				ret = area.getRightBorder();
			}
		}
		for (Area area : areas.values()) {
			if (area.getRightBorder().isOn(location)) {
				ret = null;
			}
		}
		return ret;
	}

	public Wall getRightBorder(Point location) {

		Wall ret = null;

		for (Area area : areas.values()) {
			if (area.getRightBorder().isOn(location)) {
				ret = area.getRightBorder();
			}
		}
		for (Area area : areas.values()) {
			if (area.getLeftBorder().isOn(location)) {
				ret = null;
			}
		}
		return ret;
	}

	public Collection<Area> getAreas() {
		return areas.values();
	}
}
