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

abstract class Hqx {
	private static final int Ymask = 0x00FF0000;
	private static final int Umask = 0x0000FF00;
	private static final int Vmask = 0x000000FF;

	/**
	 * Compares two ARGB colors according to the provided Y, U, V and A thresholds.
	 * @param c1 an ARGB color
	 * @param c2 a second ARGB color
	 * @param trY the Y (luminance) threshold
	 * @param trU the U (chrominance) threshold
	 * @param trV the V (chrominance) threshold
	 * @param trA the A (transparency) threshold
	 * @return true if colors differ more than the thresholds permit, false otherwise
	 */
	protected static boolean diff(final int c1, final int c2, final int trY, final int trU, final int trV, final int trA) {
		final int YUV1 = RgbYuv.getYuv(c1);
		final int YUV2 = RgbYuv.getYuv(c2);

		return (
				(Math.abs((YUV1 & Ymask) - (YUV2 & Ymask)) > trY) ||
				(Math.abs((YUV1 & Umask) - (YUV2 & Umask)) > trU) ||
				(Math.abs((YUV1 & Vmask) - (YUV2 & Vmask)) > trV) ||
				(Math.abs(((c1 >> 24) - (c2 >> 24))) > trA)
				);
	}

}
