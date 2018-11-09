package com.group_finity.mascot;

/**
 * The fix is so simple but it has taken us all 5 years. *facepalm*
 *
 * @author TigerHix
 */
public enum Platform {
    x86(20), x86_64(24);

    private final int bitmapSize;

    Platform(int bitmapSize) {
        this.bitmapSize = bitmapSize;
    }

    public int getBitmapSize() {
        return bitmapSize;
    }
}
