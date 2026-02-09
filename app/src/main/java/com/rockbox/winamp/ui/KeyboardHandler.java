package com.rockbox.winamp.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized keyboard input handler with configurable key bindings.
 * Maps physical keys to playback actions with long-press support.
 */
public class KeyboardHandler {

    private static final String TAG = "KeyboardHandler";
    private static final String PREFS_NAME = "KeyBindings";

    // Action types
    public static final int ACTION_PLAY_PAUSE = 1;
    public static final int ACTION_PLAY = 2;
    public static final int ACTION_PAUSE = 3;
    public static final int ACTION_STOP = 4;
    public static final int ACTION_NEXT = 5;
    public static final int ACTION_PREVIOUS = 6;
    public static final int ACTION_VOLUME_UP = 7;
    public static final int ACTION_VOLUME_DOWN = 8;
    public static final int ACTION_ADD_TRACKS = 9;
    public static final int ACTION_CLEAR_PLAYLIST = 10;
    public static final int ACTION_TOGGLE_SHUFFLE = 11;
    public static final int ACTION_CYCLE_REPEAT = 12;
    public static final int ACTION_LOAD_SKIN = 13;
    public static final int ACTION_DEFAULT_SKIN = 14;
    public static final int ACTION_SHOW_HELP = 15;
    public static final int ACTION_SEEK_FORWARD = 16;
    public static final int ACTION_SEEK_BACKWARD = 17;
    public static final int ACTION_FOCUS_UP = 18;
    public static final int ACTION_FOCUS_DOWN = 19;
    public static final int ACTION_FOCUS_LEFT = 20;
    public static final int ACTION_FOCUS_RIGHT = 21;
    public static final int ACTION_ACTIVATE_FOCUSED = 22;

    // Key binding maps
    private Map<Integer, Integer> keyBindings;
    private Map<Integer, Integer> longPressBindings;

    // Long-press detection
    private static final long LONG_PRESS_DURATION = 500; // ms
    private Map<Integer, Long> keyDownTimes;
    private Map<Integer, Boolean> longPressTriggered;

    // Listener
    private KeyboardListener listener;

    public interface KeyboardListener {
        void onAction(int action);
        void onLongPressAction(int action);
    }

    public KeyboardHandler(Context context) {
        this.keyBindings = new HashMap<Integer, Integer>();
        this.longPressBindings = new HashMap<Integer, Integer>();
        this.keyDownTimes = new HashMap<Integer, Long>();
        this.longPressTriggered = new HashMap<Integer, Boolean>();

        // Load key bindings from preferences
        loadKeyBindings(context);
    }

    /**
     * Initialize default Winamp-style key bindings
     */
    private void initDefaultBindings() {
        // Playback controls
        keyBindings.put(KeyEvent.KEYCODE_SPACE, ACTION_PLAY_PAUSE);
        keyBindings.put(KeyEvent.KEYCODE_N, ACTION_NEXT);
        keyBindings.put(KeyEvent.KEYCODE_B, ACTION_PREVIOUS);
        keyBindings.put(KeyEvent.KEYCODE_S, ACTION_STOP);
        keyBindings.put(KeyEvent.KEYCODE_P, ACTION_PAUSE);

        // Volume
        keyBindings.put(KeyEvent.KEYCODE_PLUS, ACTION_VOLUME_UP);
        keyBindings.put(KeyEvent.KEYCODE_EQUALS, ACTION_VOLUME_UP);
        keyBindings.put(KeyEvent.KEYCODE_MINUS, ACTION_VOLUME_DOWN);

        // Playlist
        keyBindings.put(KeyEvent.KEYCODE_A, ACTION_ADD_TRACKS);
        keyBindings.put(KeyEvent.KEYCODE_C, ACTION_CLEAR_PLAYLIST);
        keyBindings.put(KeyEvent.KEYCODE_Z, ACTION_TOGGLE_SHUFFLE);
        keyBindings.put(KeyEvent.KEYCODE_R, ACTION_CYCLE_REPEAT);

        // Skin
        keyBindings.put(KeyEvent.KEYCODE_L, ACTION_LOAD_SKIN);
        keyBindings.put(KeyEvent.KEYCODE_D, ACTION_DEFAULT_SKIN);

        // Help
        keyBindings.put(KeyEvent.KEYCODE_H, ACTION_SHOW_HELP);
        keyBindings.put(KeyEvent.KEYCODE_SLASH, ACTION_SHOW_HELP); // ? key

        // Seeking
        keyBindings.put(KeyEvent.KEYCODE_DPAD_RIGHT, ACTION_SEEK_FORWARD);
        keyBindings.put(KeyEvent.KEYCODE_DPAD_LEFT, ACTION_SEEK_BACKWARD);

        // Focus navigation
        keyBindings.put(KeyEvent.KEYCODE_DPAD_UP, ACTION_FOCUS_UP);
        keyBindings.put(KeyEvent.KEYCODE_DPAD_DOWN, ACTION_FOCUS_DOWN);
        keyBindings.put(KeyEvent.KEYCODE_DPAD_CENTER, ACTION_ACTIVATE_FOCUSED);
        keyBindings.put(KeyEvent.KEYCODE_ENTER, ACTION_ACTIVATE_FOCUSED);

        // Long-press actions
        longPressBindings.put(KeyEvent.KEYCODE_SPACE, ACTION_STOP);
        longPressBindings.put(KeyEvent.KEYCODE_B, ACTION_SEEK_BACKWARD);
        longPressBindings.put(KeyEvent.KEYCODE_N, ACTION_SEEK_FORWARD);

        Log.d(TAG, "Default key bindings initialized");
    }

    /**
     * Load key bindings from SharedPreferences
     */
    private void loadKeyBindings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (prefs.getAll().isEmpty()) {
            // No saved bindings, use defaults
            initDefaultBindings();
        } else {
            // Load saved bindings
            // Format: "action_N" -> keycode
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                try {
                    String key = entry.getKey();
                    if (key.startsWith("action_")) {
                        int action = Integer.parseInt(key.substring(7));
                        int keycode = (Integer) entry.getValue();
                        keyBindings.put(keycode, action);
                    } else if (key.startsWith("longpress_")) {
                        int action = Integer.parseInt(key.substring(10));
                        int keycode = (Integer) entry.getValue();
                        longPressBindings.put(keycode, action);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error loading key binding: " + entry.getKey());
                }
            }
            Log.d(TAG, "Loaded " + keyBindings.size() + " key bindings");
        }
    }

    /**
     * Save key bindings to SharedPreferences
     */
    public void saveKeyBindings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();

        // Save regular bindings
        for (Map.Entry<Integer, Integer> entry : keyBindings.entrySet()) {
            editor.putInt("action_" + entry.getValue(), entry.getKey());
        }

        // Save long-press bindings
        for (Map.Entry<Integer, Integer> entry : longPressBindings.entrySet()) {
            editor.putInt("longpress_" + entry.getValue(), entry.getKey());
        }

        editor.apply();
        Log.d(TAG, "Saved key bindings");
    }

    /**
     * Reset to default key bindings
     */
    public void resetToDefaults(Context context) {
        keyBindings.clear();
        longPressBindings.clear();
        initDefaultBindings();
        saveKeyBindings(context);
        Log.d(TAG, "Reset to default key bindings");
    }

    /**
     * Handle key down event
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Record key down time for long-press detection
        if (!keyDownTimes.containsKey(keyCode)) {
            keyDownTimes.put(keyCode, System.currentTimeMillis());
            longPressTriggered.put(keyCode, false);
        }

        // Check for long-press
        if (longPressBindings.containsKey(keyCode)) {
            long downTime = keyDownTimes.get(keyCode);
            long holdDuration = System.currentTimeMillis() - downTime;

            if (holdDuration >= LONG_PRESS_DURATION && !longPressTriggered.get(keyCode)) {
                // Trigger long-press action
                longPressTriggered.put(keyCode, true);
                int action = longPressBindings.get(keyCode);
                if (listener != null) {
                    listener.onLongPressAction(action);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Handle key up event
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!keyDownTimes.containsKey(keyCode)) {
            return false;
        }

        long downTime = keyDownTimes.get(keyCode);
        long holdDuration = System.currentTimeMillis() - downTime;
        boolean wasLongPress = longPressTriggered.get(keyCode);

        // Clean up tracking
        keyDownTimes.remove(keyCode);
        longPressTriggered.remove(keyCode);

        // If long-press was triggered, don't trigger normal action
        if (wasLongPress) {
            return true;
        }

        // If held too long but no long-press action defined, ignore
        if (holdDuration >= LONG_PRESS_DURATION) {
            return longPressBindings.containsKey(keyCode);
        }

        // Normal key press
        if (keyBindings.containsKey(keyCode)) {
            int action = keyBindings.get(keyCode);
            if (listener != null) {
                listener.onAction(action);
            }
            return true;
        }

        return false;
    }

    /**
     * Set keyboard listener
     */
    public void setListener(KeyboardListener listener) {
        this.listener = listener;
    }

    /**
     * Get action name for display
     */
    public static String getActionName(int action) {
        switch (action) {
            case ACTION_PLAY_PAUSE: return "Play/Pause";
            case ACTION_PLAY: return "Play";
            case ACTION_PAUSE: return "Pause";
            case ACTION_STOP: return "Stop";
            case ACTION_NEXT: return "Next Track";
            case ACTION_PREVIOUS: return "Previous Track";
            case ACTION_VOLUME_UP: return "Volume Up";
            case ACTION_VOLUME_DOWN: return "Volume Down";
            case ACTION_ADD_TRACKS: return "Add Tracks";
            case ACTION_CLEAR_PLAYLIST: return "Clear Playlist";
            case ACTION_TOGGLE_SHUFFLE: return "Toggle Shuffle";
            case ACTION_CYCLE_REPEAT: return "Cycle Repeat";
            case ACTION_LOAD_SKIN: return "Load Skin";
            case ACTION_DEFAULT_SKIN: return "Default Skin";
            case ACTION_SHOW_HELP: return "Show Help";
            case ACTION_SEEK_FORWARD: return "Seek Forward";
            case ACTION_SEEK_BACKWARD: return "Seek Backward";
            case ACTION_FOCUS_UP: return "Focus Up";
            case ACTION_FOCUS_DOWN: return "Focus Down";
            case ACTION_FOCUS_LEFT: return "Focus Left";
            case ACTION_FOCUS_RIGHT: return "Focus Right";
            case ACTION_ACTIVATE_FOCUSED: return "Activate";
            default: return "Unknown";
        }
    }

    /**
     * Get key name for display
     */
    public static String getKeyName(int keyCode) {
        return KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
    }

    /**
     * Get current key bindings as map (action -> keycode)
     */
    public Map<Integer, Integer> getKeyBindings() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : keyBindings.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    /**
     * Get long-press bindings
     */
    public Map<Integer, Integer> getLongPressBindings() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : longPressBindings.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    /**
     * Set custom key binding
     */
    public void setKeyBinding(int keyCode, int action) {
        keyBindings.put(keyCode, action);
    }

    /**
     * Set long-press binding
     */
    public void setLongPressBinding(int keyCode, int action) {
        longPressBindings.put(keyCode, action);
    }
}
