package com.rockbox.winamp.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;

/**
 * Dialog showing keyboard shortcuts and help information.
 */
public class KeyboardHelpDialog {

    /**
     * Show keyboard shortcuts help dialog
     */
    public static void show(Context context, KeyboardHandler keyboardHandler) {
        // Build help text
        StringBuilder helpText = new StringBuilder();
        helpText.append("ROCKBOX WINAMP - KEYBOARD SHORTCUTS\n\n");

        // Get current bindings
        Map<Integer, Integer> bindings = keyboardHandler.getKeyBindings();
        Map<Integer, Integer> longPressBindings = keyboardHandler.getLongPressBindings();

        // Playback controls
        helpText.append("=== PLAYBACK ===\n");
        addBinding(helpText, bindings, KeyboardHandler.ACTION_PLAY_PAUSE);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_STOP);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_NEXT);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_PREVIOUS);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_PAUSE);
        helpText.append("\n");

        // Volume
        helpText.append("=== VOLUME ===\n");
        addBinding(helpText, bindings, KeyboardHandler.ACTION_VOLUME_UP);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_VOLUME_DOWN);
        helpText.append("\n");

        // Playlist
        helpText.append("=== PLAYLIST ===\n");
        addBinding(helpText, bindings, KeyboardHandler.ACTION_ADD_TRACKS);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_CLEAR_PLAYLIST);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_TOGGLE_SHUFFLE);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_CYCLE_REPEAT);
        helpText.append("\n");

        // Skin
        helpText.append("=== SKIN ===\n");
        addBinding(helpText, bindings, KeyboardHandler.ACTION_LOAD_SKIN);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_DEFAULT_SKIN);
        helpText.append("\n");

        // Navigation
        helpText.append("=== NAVIGATION ===\n");
        addBinding(helpText, bindings, KeyboardHandler.ACTION_FOCUS_UP);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_FOCUS_DOWN);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_FOCUS_LEFT);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_FOCUS_RIGHT);
        addBinding(helpText, bindings, KeyboardHandler.ACTION_ACTIVATE_FOCUSED);
        helpText.append("\n");

        // Long-press actions
        if (!longPressBindings.isEmpty()) {
            helpText.append("=== LONG-PRESS ===\n");
            for (Map.Entry<Integer, Integer> entry : longPressBindings.entrySet()) {
                int action = entry.getKey();
                int keycode = entry.getValue();
                helpText.append(String.format("%-18s : %s (hold)\n",
                    KeyboardHandler.getActionName(action),
                    KeyboardHandler.getKeyName(keycode)));
            }
            helpText.append("\n");
        }

        // Tips
        helpText.append("=== TIPS ===\n");
        helpText.append("• Use trackpad/D-pad to navigate between buttons\n");
        helpText.append("• Hold Space to stop playback\n");
        helpText.append("• Hold B to seek backward\n");
        helpText.append("• Hold N to seek forward\n");
        helpText.append("• Press H or ? to show this help\n");

        // Create TextView with monospace font
        TextView textView = new TextView(context);
        textView.setText(helpText.toString());
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(12);
        textView.setPadding(20, 20, 20, 20);

        // Wrap in ScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(textView);

        // Show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Keyboard Shortcuts");
        builder.setView(scrollView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Add binding to help text
     */
    private static void addBinding(StringBuilder sb, Map<Integer, Integer> bindings, int action) {
        if (bindings.containsKey(action)) {
            int keycode = bindings.get(action);
            sb.append(String.format("%-18s : %s\n",
                KeyboardHandler.getActionName(action),
                KeyboardHandler.getKeyName(keycode)));
        }
    }
}
