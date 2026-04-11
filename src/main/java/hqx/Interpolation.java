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

/**
 * Helper class to interpolate colors. Nothing to see here, move along...
 */
final class Interpolation {
	private static final int Mask4  = 0xFF000000;
	private static final int Mask2  = 0x0000FF00;
	private static final int Mask13 = 0x00FF00FF;

	// return statements:
	//	 1. line: green
	//	 2. line: red and blue
	//	 3. line: alpha

	static final int Mix3To1(final int c1, final int c2) {
		//return (c1*3+c2) >> 2;
		if (c1 == c2) {
			return c1;
		}
		return ((((c1 & Mask2) * 3 + (c2 & Mask2)) >> 2) & Mask2) |
			((((c1 & Mask13) * 3 + (c2 & Mask13)) >> 2) & Mask13) |
			((((c1 & Mask4) >> 2) * 3 + ((c2 & Mask4) >> 2)) & Mask4);
	}

	static final int Mix2To1To1(final int c1, final int c2, final int c3) {
		//return (c1*2+c2+c3) >> 2;
		return ((((c1 & Mask2) * 2 + (c2 & Mask2) + (c3 & Mask2)) >> 2) & Mask2) |
			  ((((c1 & Mask13) * 2 + (c2 & Mask13) + (c3 & Mask13)) >> 2) & Mask13) |
			((((c1 & Mask4) >> 2) * 2 + ((c2 & Mask4) >> 2) + ((c3 & Mask4) >> 2)) & Mask4);
	}

	static final int Mix7To1(final int c1, final int c2) {
		//return (c1*7+c2)/8;
		if (c1 == c2) {
			return c1;
		}
		return ((((c1 & Mask2) * 7 + (c2 & Mask2)) >> 3) & Mask2) |
			((((c1 & Mask13) * 7 + (c2 & Mask13)) >> 3) & Mask13) |
			((((c1 & Mask4) >> 3) * 7 + ((c2 & Mask4) >> 3)) & Mask4);
	}

	static final int Mix2To7To7(final int c1, final int c2, final int c3) {
		//return (c1*2+(c2+c3)*7)/16;
		return ((((c1 & Mask2) * 2 + (c2 & Mask2) * 7 + (c3 & Mask2) * 7) >> 4) & Mask2) |
			  ((((c1 & Mask13) * 2 + (c2 & Mask13) * 7 + (c3 & Mask13) * 7) >> 4) & Mask13) |
			((((c1 & Mask4) >> 4) * 2 + ((c2 & Mask4) >> 4) * 7 + ((c3 & Mask4) >> 4) * 7) & Mask4);
	}

	static final int MixEven(final int c1, final int c2) {
		//return (c1+c2) >> 1;
		if (c1 == c2) {
			return c1;
		}
		return ((((c1 & Mask2) + (c2 & Mask2)) >> 1) & Mask2) |
			((((c1 & Mask13) + (c2 & Mask13)) >> 1) & Mask13) |
			((((c1 & Mask4) >> 1) + ((c2 & Mask4) >> 1)) & Mask4);
	}

	static final int Mix4To2To1(final int c1, final int c2, final int c3) {
		//return (c1*5+c2*2+c3)/8;
		return ((((c1 & Mask2) * 5 + (c2 & Mask2) * 2 + (c3 & Mask2)) >> 3) & Mask2) |
			  ((((c1 & Mask13) * 5 + (c2 & Mask13) * 2 + (c3 & Mask13)) >> 3) & Mask13) |
			((((c1 & Mask4) >> 3) * 5 + ((c2 & Mask4) >> 3) * 2 + ((c3 & Mask4) >> 3)) & Mask4);
	}

	static final int Mix6To1To1(final int c1, final int c2, final int c3) {
		//return (c1*6+c2+c3)/8;
		return ((((c1 & Mask2) * 6 + (c2 & Mask2) + (c3 & Mask2)) >> 3) & Mask2) |
			  ((((c1 & Mask13) * 6 + (c2 & Mask13) + (c3 & Mask13)) >> 3) & Mask13) |
			((((c1 & Mask4) >> 3) * 6 + ((c2 & Mask4) >> 3) + ((c3 & Mask4) >> 3)) & Mask4);
	}

	static final int Mix5To3(final int c1, final int c2) {
		//return (c1*5+c2*3)/8;
		if (c1 == c2) {
			return c1;
		}
		return ((((c1 & Mask2) * 5 + (c2 & Mask2) * 3) >> 3) & Mask2) |
			  ((((c1 & Mask13) * 5 + (c2 & Mask13) * 3) >> 3) & Mask13) |
			((((c1 & Mask4) >> 3) * 5 + ((c2 & Mask4) >> 3) * 3) & Mask4);
	}

	static final int Mix2To3To3(final int c1, final int c2, final int c3) {
		//return (c1*2+(c2+c3)*3)/8;
		return ((((c1 & Mask2) * 2 + (c2 & Mask2) * 3 + (c3 & Mask2) * 3) >> 3) & Mask2) |
			  ((((c1 & Mask13) * 2 + (c2 & Mask13) * 3 + (c3 & Mask13) * 3) >> 3) & Mask13) |
			((((c1 & Mask4) >> 3) * 2 + ((c2 & Mask4) >> 3) * 3 + ((c3 & Mask4) >> 3) * 3) & Mask4);
	}

	static final int Mix14To1To1(final int c1, final int c2, final int c3) {
		//return (c1*14+c2+c3)/16;
		return ((((c1 & Mask2) * 14 + (c2 & Mask2) + (c3 & Mask2)) >> 4) & Mask2) |
			  ((((c1 & Mask13) * 14 + (c2 & Mask13) + (c3 & Mask13)) >> 4) & Mask13) |
			((((c1 & Mask4) >> 4) * 14 + ((c2 & Mask4) >> 4) + ((c3 & Mask4) >> 4)) & Mask4);
	}
}
