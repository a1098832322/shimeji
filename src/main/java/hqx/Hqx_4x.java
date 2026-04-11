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

public class Hqx_4x extends Hqx {
	/**
	 * This is the extended Java port of the hq4x algorithm.
	 * <b>The destination image must be exactly 4 times as large in both dimensions as the source image</b>
	 * The Y, U, V, A parameters will be set as 48, 7, 6 and 0, respectively. Also, wrapping will be false.
	 *
	 * @param sp the source image data array in ARGB format
	 * @param dp the destination image data array in ARGB format
	 * @param Xres the horizontal resolution of the source image
	 * @param Yres the vertical resolution of the source image
	 *
	 * @see #hq4x_32_rb(int[], int[], int, int, int, int, int, int, boolean, boolean)
	 */
	public static void hq4x_32_rb(
			final int[] sp, final int[] dp,
			final int Xres, final int Yres)
	{
		hq4x_32_rb(sp, dp, Xres, Yres, 48, 7, 6, 0, false, false);
	}

	/**
	 * This and the next caseXXX methods were used to reduce the code size of the main
	 * #hq4x_32_rb(int[], int[], int, int, int, int, int, int, boolean, boolean) method because of the Java 65K bytecode limit.
	 * Only the necessary methods were created, to leave the maximum code on the original one to avoid excessive calling.
	 * However, this is a very bad design (too much code in the same method)
	 */
	private static final void case0(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case2(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case16(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case64(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case8(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case3(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case6(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case20(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case144(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
	}

	private static final void case192(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
	}

	private static final void case96(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case40(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case9(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case66(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case24(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case7(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case148(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
	}

	private static final void case224(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
	}

	private static final void case41(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
	}

	private static final void case67(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case70(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case28(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case152(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
	}

	private static final void case194(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
	}

	private static final void case98(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case56(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
		dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	private static final void case25(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
		dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
		dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
		dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
		dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
		dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
		dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
		dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
		dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
		dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
		dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
		dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
	}

	/**
	 * This is the extended Java port of the hq4x algorithm.
	 * <b>The destination image must be exactly 4 times as large in both dimensions as the source image</b>
	 * @param sp the source image data array in ARGB format
	 * @param dp the destination image data array in ARGB format
	 * @param Xres the horizontal resolution of the source image
	 * @param Yres the vertical resolution of the source image
	 * @param trY the Y (luminance) threshold
	 * @param trU the U (chrominance) threshold
	 * @param trV the V (chrominance) threshold
	 * @param trA the A (transparency) threshold
	 * @param wrapX used for images that can be seamlessly repeated horizontally
	 * @param wrapY used for images that can be seamlessly repeated vertically
	 */
	public static void hq4x_32_rb(
			final int[] sp, final int[] dp,
			final int Xres, final int Yres,
			int trY, int trU, final int trV, final int trA,
			final boolean wrapX, final boolean wrapY)
	{
		int spIdx = 0, dpIdx = 0;
		//Don't shift trA, as it uses shift right instead of a mask for comparisons.
		trY <<= 2 * 8;
		trU <<= 1 * 8;
		final int dpL = Xres * 4;

		int prevline, nextline;
		final int[] w = new int[9];

		for (int j = 0; j < Yres; j++) {
			prevline = (j > 0)
					? -Xres
					: wrapY
						? Xres * (Yres - 1)
						: 0;
			nextline = (j < Yres - 1)
					? Xres
					: wrapY
						? -(Xres * (Yres - 1))
						: 0;
			for (int i = 0; i < Xres; i++) {
				w[1] = sp[spIdx + prevline];
				w[4] = sp[spIdx];
				w[7] = sp[spIdx + nextline];

				if (i > 0) {
					w[0] = sp[spIdx + prevline - 1];
					w[3] = sp[spIdx - 1];
					w[6] = sp[spIdx + nextline - 1];
				} else {
					if (wrapX) {
						w[0] = sp[spIdx + prevline + Xres - 1];
						w[3] = sp[spIdx + Xres - 1];
						w[6] = sp[spIdx + nextline + Xres - 1];
					} else {
						w[0] = w[1];
						w[3] = w[4];
						w[6] = w[7];
					}
				}

				if (i < Xres - 1) {
					w[2] = sp[spIdx + prevline + 1];
					w[5] = sp[spIdx + 1];
					w[8] = sp[spIdx + nextline + 1];
				} else {
					if (wrapX) {
						w[2] = sp[spIdx + prevline - Xres + 1];
						w[5] = sp[spIdx - Xres + 1];
						w[8] = sp[spIdx + nextline - Xres + 1];
					} else {
						w[2] = w[1];
						w[5] = w[4];
						w[8] = w[7];
					}
				}

				int pattern = 0;
				int flag = 1;

				for (int k = 0; k < 9; k++)
				{
					if (k == 4) continue;

					if (w[k] != w[4])
					{
						if (diff(w[4], w[k], trY, trU, trV, trA))
							pattern |= flag;
					}
					flag <<= 1;
				}

				switch (pattern) {
					case 0:
					case 1:
					case 4:
					case 32:
					case 128:
					case 5:
					case 132:
					case 160:
					case 33:
					case 129:
					case 36:
					case 133:
					case 164:
					case 161:
					case 37:
					case 165: {
						case0(dp, dpIdx, dpL, w);
						break;
					}
					case 2:
					case 34:
					case 130:
					case 162: {
						case2(dp, dpIdx, dpL, w);
						break;
					}
					case 16:
					case 17:
					case 48:
					case 49: {
						case16(dp, dpIdx, dpL, w);
						break;
					}
					case 64:
					case 65:
					case 68:
					case 69: {
						case64(dp, dpIdx, dpL, w);
						break;
					}
					case 8:
					case 12:
					case 136:
					case 140: {
						case8(dp, dpIdx, dpL, w);
						break;
					}
					case 3:
					case 35:
					case 131:
					case 163: {
						case3(dp, dpIdx, dpL, w);
						break;
					}
					case 6:
					case 38:
					case 134:
					case 166: {
						case6(dp, dpIdx, dpL, w);
						break;
					}
					case 20:
					case 21:
					case 52:
					case 53: {
						case20(dp, dpIdx, dpL, w);
						break;
					}
					case 144:
					case 145:
					case 176:
					case 177: {
						case144(dp, dpIdx, dpL, w);
						break;
					}
					case 192:
					case 193:
					case 196:
					case 197: {
						case192(dp, dpIdx, dpL, w);
						break;
					}
					case 96:
					case 97:
					case 100:
					case 101: {
						case96(dp, dpIdx, dpL, w);
						break;
					}
					case 40:
					case 44:
					case 168:
					case 172: {
						case40(dp, dpIdx, dpL, w);
						break;
					}
					case 9:
					case 13:
					case 137:
					case 141: {
						case9(dp, dpIdx, dpL, w);
						break;
					}
					case 18:
					case 50: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 80:
					case 81: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 72:
					case 76: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 10:
					case 138: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 66: {
						case66(dp, dpIdx, dpL, w);
						break;
					}
					case 24: {
						case24(dp, dpIdx, dpL, w);
						break;
					}
					case 7:
					case 39:
					case 135: {
						case7(dp, dpIdx, dpL, w);
						break;
					}
					case 148:
					case 149:
					case 180: {
						case148(dp, dpIdx, dpL, w);
						break;
					}
					case 224:
					case 228:
					case 225: {
						case224(dp, dpIdx, dpL, w);
						break;
					}
					case 41:
					case 169:
					case 45: {
						case41(dp, dpIdx, dpL, w);
						break;
					}
					case 22:
					case 54: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 208:
					case 209: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 104:
					case 108: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 11:
					case 139: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 19:
					case 51: {
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[1], w[4]);
							dp[dpIdx + 2] = Interpolation.Mix5To3(w[1], w[5]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix2To1To1(w[5], w[4], w[1]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 146:
					case 178: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix2To1To1(w[1], w[4], w[5]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[5], w[1]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 84:
					case 85: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[5], w[4]);
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[5], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix2To1To1(w[7], w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 112:
					case 113: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[5], w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[7], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 200:
					case 204: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix2To1To1(w[3], w[4], w[7]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 73:
					case 77: {
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[3], w[4]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[3], w[7]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix2To1To1(w[7], w[4], w[3]);
						}
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 42:
					case 170: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix2To1To1(w[1], w[4], w[3]);
							dp[dpIdx + dpL] = Interpolation.Mix5To3(w[3], w[1]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 14:
					case 142: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix5To3(w[1], w[3]);
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix2To1To1(w[3], w[4], w[1]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						}
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 67: {
						case67(dp, dpIdx, dpL, w);
						break;
					}
					case 70: {
						case70(dp, dpIdx, dpL, w);
						break;
					}
					case 28: {
						case28(dp, dpIdx, dpL, w);
						break;
					}
					case 152: {
						case152(dp, dpIdx, dpL, w);
						break;
					}
					case 194: {
						case194(dp, dpIdx, dpL, w);
						break;
					}
					case 98: {
						case98(dp, dpIdx, dpL, w);
						break;
					}
					case 56: {
						case56(dp, dpIdx, dpL, w);
						break;
					}
					case 25: {
						case25(dp, dpIdx, dpL, w);
						break;
					}
					case 26:
					case 31: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 82:
					case 214: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 88:
					case 248: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 74:
					case 107: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 27: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 86: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 216: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 106: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 30: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 210: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 120: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 75: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 29: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 198: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 184: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 99: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 57: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 71: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 156: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 226: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 60: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 195: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 102: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 153: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 58: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 83: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 92: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 202: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 78: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 154: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 114: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						break;
					}
					case 89: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 90: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 55:
					case 23: {
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[1], w[4]);
							dp[dpIdx + 2] = Interpolation.Mix5To3(w[1], w[5]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix2To1To1(w[5], w[4], w[1]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 182:
					case 150: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix2To1To1(w[1], w[4], w[5]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[5], w[1]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 213:
					case 212: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[5], w[4]);
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[5], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix2To1To1(w[7], w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 241:
					case 240: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[5], w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[7], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 236:
					case 232: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix2To1To1(w[3], w[4], w[7]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 109:
					case 105: {
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[3], w[4]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[3], w[7]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix2To1To1(w[7], w[4], w[3]);
						}
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 171:
					case 43: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
							dp[dpIdx + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix2To1To1(w[1], w[4], w[3]);
							dp[dpIdx + dpL] = Interpolation.Mix5To3(w[3], w[1]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 143:
					case 15: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
							dp[dpIdx + dpL] = w[4];
							dp[dpIdx + dpL + 1] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix5To3(w[1], w[3]);
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix2To1To1(w[3], w[4], w[1]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						}
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 124: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 203: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 62: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 211: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 118: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 217: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 110: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 155: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 188: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 185: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 61: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 157: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 103: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 227: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 230: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 199: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 220: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 158: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 234: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 242: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						break;
					}
					case 59: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 121: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 87: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 79: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 122: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 94: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL + 2] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 218: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 91: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL + 1] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 229: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 167: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 173: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 181: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 186: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 115: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						break;
					}
					case 93: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 206: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 205:
					case 201: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 174:
					case 46: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL + 1] = w[4];
						}
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 179:
					case 147: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 117:
					case 116: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						} else {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						break;
					}
					case 189: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 231: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 126: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 219: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 125: {
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix3To1(w[4], w[3]);
							dp[dpIdx + dpL] = Interpolation.Mix3To1(w[3], w[4]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[3], w[7]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix2To1To1(w[7], w[4], w[3]);
						}
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 221: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix3To1(w[4], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[5], w[4]);
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[5], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix2To1To1(w[7], w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 207: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
							dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
							dp[dpIdx + dpL] = w[4];
							dp[dpIdx + dpL + 1] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix5To3(w[1], w[3]);
							dp[dpIdx + 2] = Interpolation.Mix3To1(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + dpL] = Interpolation.Mix2To1To1(w[3], w[4], w[1]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						}
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 238: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.Mix2To1To1(w[3], w[4], w[7]);
							dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[7]);
						}
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 190: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						} else {
							dp[dpIdx + 2] = Interpolation.Mix2To1To1(w[1], w[4], w[5]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[5], w[1]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 187: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
							dp[dpIdx + dpL + 1] = w[4];
							dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix2To1To1(w[1], w[4], w[3]);
							dp[dpIdx + dpL] = Interpolation.Mix5To3(w[3], w[1]);
							dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
							dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix3To1(w[4], w[3]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 243: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
							dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[5], w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix3To1(w[4], w[7]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[7], w[5]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 119: {
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
							dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 2] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix3To1(w[4], w[1]);
							dp[dpIdx + 1] = Interpolation.Mix3To1(w[1], w[4]);
							dp[dpIdx + 2] = Interpolation.Mix5To3(w[1], w[5]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
							dp[dpIdx + dpL + 3] = Interpolation.Mix2To1To1(w[5], w[4], w[1]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 237:
					case 233: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[1]);
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 175:
					case 47: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix6To1To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						break;
					}
					case 183:
					case 151: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 245:
					case 244: {
						dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[3]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix6To1To1(w[4], w[3], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 250: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						break;
					}
					case 123: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 95: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 222: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 252: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix4To2To1(w[4], w[1], w[0]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 249: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix4To2To1(w[4], w[1], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						break;
					}
					case 235: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[2]);
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 111: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix4To2To1(w[4], w[5], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 63: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix4To2To1(w[4], w[7], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 159: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix4To2To1(w[4], w[7], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 215: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 246: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix4To2To1(w[4], w[3], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 254: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[0]);
						dp[dpIdx + 1] = Interpolation.Mix3To1(w[4], w[0]);
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix3To1(w[4], w[0]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[0]);
						dp[dpIdx + dpL + 2] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 253: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 1] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 2] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[1]);
						dp[dpIdx + dpL] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix7To1(w[4], w[1]);
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 251: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[2]);
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[2]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix3To1(w[4], w[2]);
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						break;
					}
					case 239: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						dp[dpIdx + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[5]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[5]);
						break;
					}
					case 127: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 2] = w[4];
							dp[dpIdx + 3] = w[4];
							dp[dpIdx + dpL + 3] = w[4];
						} else {
							dp[dpIdx + 2] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + 3] = Interpolation.MixEven(w[1], w[5]);
							dp[dpIdx + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
						}
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						} else {
							dp[dpIdx + dpL + dpL] = Interpolation.MixEven(w[3], w[4]);
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.MixEven(w[7], w[3]);
							dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.MixEven(w[7], w[4]);
						}
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix3To1(w[4], w[8]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[8]);
						break;
					}
					case 191: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 2] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + 3] = Interpolation.Mix7To1(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.Mix5To3(w[4], w[7]);
						dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix5To3(w[4], w[7]);
						break;
					}
					case 223: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
							dp[dpIdx + 1] = w[4];
							dp[dpIdx + dpL] = w[4];
						} else {
							dp[dpIdx] = Interpolation.MixEven(w[1], w[3]);
							dp[dpIdx + 1] = Interpolation.MixEven(w[1], w[4]);
							dp[dpIdx + dpL] = Interpolation.MixEven(w[3], w[4]);
						}
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix3To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[6]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + 3] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + 3] = Interpolation.MixEven(w[5], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 2] = Interpolation.MixEven(w[7], w[4]);
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.MixEven(w[7], w[5]);
						}
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[6]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix3To1(w[4], w[6]);
						break;
					}
					case 247: {
						dp[dpIdx] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix5To3(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 1] = Interpolation.Mix7To1(w[4], w[3]);
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
					case 255: {
						if (diff(w[3], w[1], trY, trU, trV, trA)) {
							dp[dpIdx] = w[4];
						} else {
							dp[dpIdx] = Interpolation.Mix2To1To1(w[4], w[1], w[3]);
						}
						dp[dpIdx + 1] = w[4];
						dp[dpIdx + 2] = w[4];
						if (diff(w[1], w[5], trY, trU, trV, trA)) {
							dp[dpIdx + 3] = w[4];
						} else {
							dp[dpIdx + 3] = Interpolation.Mix2To1To1(w[4], w[1], w[5]);
						}
						dp[dpIdx + dpL] = w[4];
						dp[dpIdx + dpL + 1] = w[4];
						dp[dpIdx + dpL + 2] = w[4];
						dp[dpIdx + dpL + 3] = w[4];
						dp[dpIdx + dpL + dpL] = w[4];
						dp[dpIdx + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + 2] = w[4];
						dp[dpIdx + dpL + dpL + 3] = w[4];
						if (diff(w[7], w[3], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL] = Interpolation.Mix2To1To1(w[4], w[7], w[3]);
						}
						dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
						dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
						if (diff(w[5], w[7], trY, trU, trV, trA)) {
							dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
						} else {
							dp[dpIdx + dpL + dpL + dpL + 3] = Interpolation.Mix2To1To1(w[4], w[7], w[5]);
						}
						break;
					}
				}
				spIdx++;
				dpIdx += 4;
			}
			dpIdx += (dpL * 3);
		}
	}
}
