package com.rockbox.winamp.skin;

import android.graphics.Bitmap;
import android.graphics.Rect;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages loaded skin bitmaps and provides efficient caching.
 * Handles bitmap slicing for UI components and memory management.
 */
public class SkinAssets {

    // Standard Winamp skin bitmap names
    public static final String MAIN = "main.bmp";
    public static final String CBUTTONS = "cbuttons.bmp";
    public static final String TITLEBAR = "titlebar.bmp";
    public static final String NUMBERS = "numbers.bmp";
    public static final String PLAYPAUS = "playpaus.bmp";
    public static final String POSBAR = "posbar.bmp";
    public static final String VOLUME = "volume.bmp";
    public static final String BALANCE = "balance.bmp";
    public static final String MONOSTER = "monoster.bmp";
    public static final String SHUFREP = "shufrep.bmp";
    public static final String TEXT = "text.bmp";
    public static final String NUMS_EX = "nums_ex.bmp";

    // Bitmap cache
    private Map<String, Bitmap> bitmaps;

    // Sliced button regions (cached for performance)
    private Map<String, Rect> buttonRegions;

    // Metadata
    private boolean isLoaded = false;
    private String skinName = "Default";
    private String skinPath = null;

    public SkinAssets() {
        bitmaps = new HashMap<String, Bitmap>();
        buttonRegions = new HashMap<String, Rect>();
    }

    /**
     * Add a bitmap to the cache
     */
    public void putBitmap(String name, Bitmap bitmap) {
        if (bitmap != null) {
            bitmaps.put(name, bitmap);
        }
    }

    /**
     * Get a bitmap from the cache
     */
    public Bitmap getBitmap(String name) {
        return bitmaps.get(name);
    }

    /**
     * Check if a specific bitmap is loaded
     */
    public boolean hasBitmap(String name) {
        return bitmaps.containsKey(name) && bitmaps.get(name) != null;
    }

    /**
     * Get sliced region of a bitmap
     * Useful for extracting individual buttons from sprite sheets
     */
    public Bitmap getSlice(String bitmapName, int x, int y, int width, int height) {
        Bitmap source = bitmaps.get(bitmapName);
        if (source == null) return null;

        try {
            return Bitmap.createBitmap(source, x, y, width, height);
        } catch (IllegalArgumentException e) {
            // Invalid dimensions
            return null;
        }
    }

    /**
     * Cache a button region for hit testing
     */
    public void putButtonRegion(String buttonName, Rect region) {
        buttonRegions.put(buttonName, region);
    }

    /**
     * Get cached button region
     */
    public Rect getButtonRegion(String buttonName) {
        return buttonRegions.get(buttonName);
    }

    /**
     * Get main window bitmap (275x116px standard)
     */
    public Bitmap getMainWindow() {
        return getBitmap(MAIN);
    }

    /**
     * Get control buttons bitmap
     */
    public Bitmap getControlButtons() {
        return getBitmap(CBUTTONS);
    }

    /**
     * Get numbers bitmap for time display
     */
    public Bitmap getNumbers() {
        return getBitmap(NUMBERS);
    }

    /**
     * Extract a single digit from numbers.bmp
     * Winamp numbers.bmp contains digits 0-9 in a row
     */
    public Bitmap getDigit(int digit) {
        Bitmap numbers = getBitmap(NUMBERS);
        if (numbers == null || digit < 0 || digit > 9) return null;

        // Standard digit width in Winamp skins (typically 9px)
        int digitWidth = numbers.getWidth() / 11; // 11 chars: 0-9 plus blank
        int digitHeight = numbers.getHeight();

        return getSlice(NUMBERS, digit * digitWidth, 0, digitWidth, digitHeight);
    }

    /**
     * Get individual button from cbuttons.bmp
     * Winamp button states: normal, pressed, disabled
     */
    public Bitmap getButton(String buttonName, int state) {
        // Will be implemented when we parse button coordinates
        // For now, return null
        return null;
    }

    /**
     * Release all bitmaps to free memory
     */
    public void release() {
        for (Bitmap bitmap : bitmaps.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmaps.clear();
        buttonRegions.clear();
        isLoaded = false;
    }

    /**
     * Check if skin is fully loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Mark skin as loaded
     */
    public void setLoaded(boolean loaded) {
        this.isLoaded = loaded;
    }

    /**
     * Get skin name
     */
    public String getSkinName() {
        return skinName;
    }

    /**
     * Set skin name
     */
    public void setSkinName(String name) {
        this.skinName = name;
    }

    /**
     * Get skin file path
     */
    public String getSkinPath() {
        return skinPath;
    }

    /**
     * Set skin file path
     */
    public void setSkinPath(String path) {
        this.skinPath = path;
    }

    /**
     * Get total memory used by cached bitmaps (bytes)
     */
    public long getMemoryUsage() {
        long total = 0;
        for (Bitmap bitmap : bitmaps.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                total += bitmap.getByteCount();
            }
        }
        return total;
    }

    /**
     * Check if minimum required bitmaps are loaded
     */
    public boolean hasMinimumBitmaps() {
        return hasBitmap(MAIN) || hasBitmap(CBUTTONS);
    }

    /**
     * Get count of loaded bitmaps
     */
    public int getBitmapCount() {
        int count = 0;
        for (Bitmap bitmap : bitmaps.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                count++;
            }
        }
        return count;
    }
}
