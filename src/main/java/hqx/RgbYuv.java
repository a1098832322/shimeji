/*
 * Copyright © 2003 Maxim Stepin (maxst@hiend3d.com)
 *
 * Copyright © 2010 Cameron Zemek (grom@zeminvaders.net)
 *
 * Copyright © 2011 Tamme Schichler (tamme.schichler@googlemail.com)

 * Copyright © 2012 A. Eduardo García (arcnorj@gmail.com)
 *
 * This file is part of hqx-java.
 *
 * hqx-java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * hqx-java is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with hqx-java. If not, see <http://www.gnu.org/licenses/>.
 */

package hqx;

public final class RgbYuv {
	private static final int rgbMask = 0x00FFFFFF;
	private static int[] RGBtoYUV = new int[0x1000000];

	/**
	 * Returns the 24bit YUV equivalent of the provided 24bit RGB color.<b>Any alpha component is dropped</b>
	 *
	 * @param rgb a 24bit rgb color
	 * @return the corresponding 24bit YUV color
	 */
	static int getYuv(final int rgb) {
		return RGBtoYUV[rgb & rgbMask];
	}

	/**
	 * Calculates the lookup table. <b>MUST</b> be called (only once) before doing anything else
	 */
	public static void hqxInit() {
		/* Initalize RGB to YUV lookup table */
		int r, g, b, y, u, v;
		for (int c = 0x1000000 - 1; c >= 0; c--) {
			r = (c & 0xFF0000) >> 16;
			g = (c & 0x00FF00) >> 8;
			b = c & 0x0000FF;
			y = (int) (+0.299d * r + 0.587d * g + 0.114d * b);
			u = (int) (-0.169d * r - 0.331d * g + 0.500d * b) + 128;
			v = (int) (+0.500d * r - 0.419d * g - 0.081d * b) + 128;
			RGBtoYUV[c] = (y << 16) | (u << 8) | v;
		}
	}

	/// <summary>
    /// Releases the reference to the lookup table.
    /// <para>The table has to be calculated again for the next lookup.</para>
    /// </summary>
	/**
	 * Releases the reference to the lookup table. <b>The table has to be calculated again for the next lookup.</b>
	 */
	public static void hqxDeinit() {
        RGBtoYUV = null;
    }
}
