package com.rockbox.winamp.audio;

import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.util.Log;

/**
 * 10-band equalizer using Android AudioEffect API.
 * Controls frequency bands and presets.
 */
public class Equalizer {

    private static final String TAG = "Equalizer";

    private android.media.audiofx.Equalizer androidEqualizer;
    private BassBoost bassBoost;
    private boolean enabled;
    private int audioSessionId;

    // Standard 10-band frequencies (Hz)
    private static final int[] BAND_FREQUENCIES = {
        31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000
    };

    // Presets
    public static final int PRESET_FLAT = 0;
    public static final int PRESET_ROCK = 1;
    public static final int PRESET_JAZZ = 2;
    public static final int PRESET_CLASSICAL = 3;
    public static final int PRESET_POP = 4;
    public static final int PRESET_BASS_BOOST = 5;

    private static final String[] PRESET_NAMES = {
        "Flat", "Rock", "Jazz", "Classical", "Pop", "Bass Boost"
    };

    // Preset values (in millibels, -1500 to +1500)
    private static final short[][] PRESET_VALUES = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},              // Flat
        {500, 300, -100, -300, -100, 200, 500, 700, 700, 700},  // Rock
        {0, 0, 0, 300, 300, 300, 0, 200, 300, 400},  // Jazz
        {0, 0, 0, 0, 0, 0, -200, -200, -200, -300},  // Classical
        {-200, -100, 0, 200, 400, 400, 200, 0, -100, -200},  // Pop
        {800, 600, 400, 200, 0, 0, 0, 0, 0, 0}       // Bass Boost
    };

    public Equalizer(int audioSessionId) {
        this.audioSessionId = audioSessionId;
        this.enabled = false;

        try {
            // Initialize Android Equalizer
            androidEqualizer = new android.media.audiofx.Equalizer(0, audioSessionId);
            androidEqualizer.setEnabled(false);

            // Initialize Bass Boost
            bassBoost = new BassBoost(0, audioSessionId);
            bassBoost.setEnabled(false);

            Log.d(TAG, "Equalizer initialized: " + getNumberOfBands() + " bands available");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize equalizer", e);
            androidEqualizer = null;
            bassBoost = null;
        }
    }

    /**
     * Check if equalizer is available
     */
    public boolean isAvailable() {
        return androidEqualizer != null;
    }

    /**
     * Enable or disable equalizer
     */
    public void setEnabled(boolean enabled) {
        if (!isAvailable()) return;

        try {
            this.enabled = enabled;
            androidEqualizer.setEnabled(enabled);
            Log.d(TAG, "Equalizer " + (enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Log.e(TAG, "Error setting enabled state", e);
        }
    }

    /**
     * Check if equalizer is enabled
     */
    public boolean isEnabled() {
        return enabled && isAvailable();
    }

    /**
     * Get number of bands
     */
    public int getNumberOfBands() {
        if (!isAvailable()) return 0;
        return Math.min(10, androidEqualizer.getNumberOfBands());
    }

    /**
     * Set band level (-1500 to +1500 millibels)
     */
    public void setBandLevel(int band, short level) {
        if (!isAvailable()) return;
        if (band < 0 || band >= getNumberOfBands()) return;

        try {
            short actualBand = (short) band;
            androidEqualizer.setBandLevel(actualBand, level);
        } catch (Exception e) {
            Log.e(TAG, "Error setting band level", e);
        }
    }

    /**
     * Get band level
     */
    public short getBandLevel(int band) {
        if (!isAvailable()) return 0;
        if (band < 0 || band >= getNumberOfBands()) return 0;

        try {
            return androidEqualizer.getBandLevel((short) band);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get band frequency (Hz)
     */
    public int getBandFrequency(int band) {
        if (band >= 0 && band < BAND_FREQUENCIES.length) {
            return BAND_FREQUENCIES[band];
        }
        return 0;
    }

    /**
     * Apply preset
     */
    public void applyPreset(int presetIndex) {
        if (!isAvailable()) return;
        if (presetIndex < 0 || presetIndex >= PRESET_VALUES.length) return;

        short[] values = PRESET_VALUES[presetIndex];
        int numBands = Math.min(getNumberOfBands(), values.length);

        for (int i = 0; i < numBands; i++) {
            setBandLevel(i, values[i]);
        }

        Log.d(TAG, "Applied preset: " + PRESET_NAMES[presetIndex]);
    }

    /**
     * Get preset name
     */
    public static String getPresetName(int presetIndex) {
        if (presetIndex >= 0 && presetIndex < PRESET_NAMES.length) {
            return PRESET_NAMES[presetIndex];
        }
        return "Unknown";
    }

    /**
     * Get number of presets
     */
    public static int getNumberOfPresets() {
        return PRESET_NAMES.length;
    }

    /**
     * Reset to flat (all bands at 0)
     */
    public void reset() {
        applyPreset(PRESET_FLAT);
    }

    /**
     * Get min band level
     */
    public short getMinLevel() {
        return -1500; // -15 dB
    }

    /**
     * Get max band level
     */
    public short getMaxLevel() {
        return 1500; // +15 dB
    }

    /**
     * Set bass boost strength (0-1000)
     */
    public void setBassBoost(short strength) {
        if (bassBoost == null) return;

        try {
            bassBoost.setStrength(strength);
            bassBoost.setEnabled(strength > 0);
        } catch (Exception e) {
            Log.e(TAG, "Error setting bass boost", e);
        }
    }

    /**
     * Get bass boost strength
     */
    public short getBassBoost() {
        if (bassBoost == null) return 0;

        try {
            return bassBoost.getRoundedStrength();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Release resources
     */
    public void release() {
        if (androidEqualizer != null) {
            try {
                androidEqualizer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing equalizer", e);
            }
            androidEqualizer = null;
        }

        if (bassBoost != null) {
            try {
                bassBoost.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing bass boost", e);
            }
            bassBoost = null;
        }

        Log.d(TAG, "Equalizer released");
    }
}
