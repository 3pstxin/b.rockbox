package com.rockbox.winamp.skin;

import android.graphics.Rect;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Winamp classic skin configuration files.
 * Handles region.txt (window shape), pledit.txt (playlist config),
 * and extracts button coordinates from bitmaps.
 */
public class SkinParser {

    private static final String TAG = "SkinParser";

    // Standard Winamp main window button coordinates (default skin)
    // Format: [x, y, width, height]
    private static final int[][] DEFAULT_BUTTON_COORDS = {
        {16, 88, 23, 18},   // Previous
        {39, 88, 23, 18},   // Play
        {62, 88, 23, 18},   // Pause
        {85, 88, 23, 18},   // Stop
        {108, 88, 23, 18},  // Next
        {136, 89, 22, 16},  // Eject (open file)
    };

    private static final String[] BUTTON_NAMES = {
        "previous", "play", "pause", "stop", "next", "eject"
    };

    // Parsed data
    private Map<String, Rect> buttonCoords;
    private List<int[]> regionPoints;
    private Map<String, String> pleditConfig;

    public SkinParser() {
        buttonCoords = new HashMap<String, Rect>();
        regionPoints = new ArrayList<int[]>();
        pleditConfig = new HashMap<String, String>();
        initDefaultButtonCoords();
    }

    /**
     * Initialize default button coordinates
     */
    private void initDefaultButtonCoords() {
        for (int i = 0; i < BUTTON_NAMES.length && i < DEFAULT_BUTTON_COORDS.length; i++) {
            int[] coords = DEFAULT_BUTTON_COORDS[i];
            Rect rect = new Rect(coords[0], coords[1], coords[0] + coords[2], coords[1] + coords[3]);
            buttonCoords.put(BUTTON_NAMES[i], rect);
        }
    }

    /**
     * Parse region.txt file for window shape definition
     * Format: PointList = x1,y1,x2,y2,...
     */
    public void parseRegionFile(File regionFile) {
        if (regionFile == null || !regionFile.exists()) {
            Log.w(TAG, "Region file not found, using default rectangular shape");
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(regionFile));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                    continue;
                }

                // Parse PointList
                if (line.startsWith("PointList") || line.startsWith("pointlist")) {
                    parsePointList(line);
                }
                // Parse NumPoints
                else if (line.startsWith("NumPoints") || line.startsWith("numpoints")) {
                    // Number of points (for validation)
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        try {
                            int numPoints = Integer.parseInt(parts[1].trim());
                            Log.d(TAG, "Expected region points: " + numPoints);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid NumPoints value");
                        }
                    }
                }
            }

            Log.i(TAG, "Parsed region file: " + regionPoints.size() + " points");

        } catch (IOException e) {
            Log.e(TAG, "Error parsing region file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Parse PointList from region.txt
     * Format: PointList = x1,y1, x2,y2, x3,y3, ...
     */
    private void parsePointList(String line) {
        String[] parts = line.split("=");
        if (parts.length < 2) return;

        String pointsStr = parts[1].trim();
        String[] coords = pointsStr.split(",");

        for (int i = 0; i + 1 < coords.length; i += 2) {
            try {
                int x = Integer.parseInt(coords[i].trim());
                int y = Integer.parseInt(coords[i + 1].trim());
                regionPoints.add(new int[]{x, y});
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid coordinate in PointList");
            }
        }
    }

    /**
     * Parse pledit.txt file for playlist editor configuration
     * Contains colors, fonts, and layout information
     */
    public void parsePleditFile(File pleditFile) {
        if (pleditFile == null || !pleditFile.exists()) {
            Log.w(TAG, "Pledit file not found, using defaults");
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(pleditFile));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                    continue;
                }

                // Parse key=value pairs
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();
                    pleditConfig.put(key, value);
                }
            }

            Log.i(TAG, "Parsed pledit file: " + pleditConfig.size() + " config items");

        } catch (IOException e) {
            Log.e(TAG, "Error parsing pledit file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Get button coordinate rectangle
     */
    public Rect getButtonRect(String buttonName) {
        return buttonCoords.get(buttonName.toLowerCase());
    }

    /**
     * Get all button coordinates
     */
    public Map<String, Rect> getButtonCoords() {
        return new HashMap<String, Rect>(buttonCoords);
    }

    /**
     * Get region points for window shaping
     */
    public List<int[]> getRegionPoints() {
        return new ArrayList<int[]>(regionPoints);
    }

    /**
     * Check if custom region is defined
     */
    public boolean hasCustomRegion() {
        return !regionPoints.isEmpty();
    }

    /**
     * Get playlist editor config value
     */
    public String getPleditConfig(String key) {
        return pleditConfig.get(key.toLowerCase());
    }

    /**
     * Get playlist text color from pledit config
     * Format: "R,G,B" string
     */
    public int[] getPleditTextColor() {
        String colorStr = getPleditConfig("normalbgcolour");
        if (colorStr == null) {
            colorStr = getPleditConfig("normalbgcolor"); // Alternative spelling
        }

        if (colorStr != null) {
            return parseRGB(colorStr);
        }

        // Default: white
        return new int[]{255, 255, 255};
    }

    /**
     * Get playlist background color
     */
    public int[] getPleditBgColor() {
        String colorStr = getPleditConfig("normal");
        if (colorStr != null) {
            return parseRGB(colorStr);
        }

        // Default: black
        return new int[]{0, 0, 0};
    }

    /**
     * Parse RGB color string "R,G,B"
     */
    private int[] parseRGB(String colorStr) {
        try {
            String[] parts = colorStr.split(",");
            if (parts.length >= 3) {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new int[]{r, g, b};
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid RGB color format: " + colorStr);
        }
        return new int[]{255, 255, 255};
    }

    /**
     * Extract button coordinates from cbuttons.bmp
     * Standard Winamp button layout: each button is 23x18px
     * Arranged in vertical strips for different states
     */
    public void extractButtonCoordinates() {
        // Winamp standard button dimensions
        int buttonWidth = 23;
        int buttonHeight = 18;

        // Standard layout (may vary by skin)
        // This is a simplified version - real implementation would
        // analyze the bitmap to find button boundaries

        // For now, use default coordinates
        Log.d(TAG, "Using default button coordinates");
    }

    /**
     * Parse number coordinates from numbers.bmp
     * Returns width and height of each digit
     */
    public int[] getNumberDimensions() {
        // Standard Winamp numbers are 9x13px per digit
        // 11 characters total: 0-9 plus blank
        return new int[]{9, 13};
    }

    /**
     * Get main window dimensions
     * Standard: 275x116px
     */
    public int[] getMainWindowDimensions() {
        return new int[]{275, 116};
    }

    /**
     * Get title bar height
     * Standard: 14px
     */
    public int getTitleBarHeight() {
        return 14;
    }

    /**
     * Get visualizer area rectangle
     * Standard position in main window
     */
    public Rect getVisualizerRect() {
        // Standard Winamp visualizer position: [24, 43, 75+24, 15+43]
        return new Rect(24, 43, 99, 58);
    }

    /**
     * Get time display position
     * Standard position in main window
     */
    public Rect getTimeDisplayRect() {
        // Standard Winamp time display: [36, 26, width, height]
        return new Rect(36, 26, 108, 39);
    }

    /**
     * Get song title/info display area
     * Standard position in main window
     */
    public Rect getInfoDisplayRect() {
        // Standard Winamp info text: [111, 27, 156+111, 12+27]
        return new Rect(111, 27, 267, 39);
    }

    /**
     * Check if all required configs are loaded
     */
    public boolean isConfigured() {
        return !buttonCoords.isEmpty();
    }

    /**
     * Reset parser state
     */
    public void reset() {
        buttonCoords.clear();
        regionPoints.clear();
        pleditConfig.clear();
        initDefaultButtonCoords();
    }
}
