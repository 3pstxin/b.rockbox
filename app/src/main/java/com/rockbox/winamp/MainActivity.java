package com.rockbox.winamp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.rockbox.winamp.ui.WinampView;

/**
 * Main Activity for Rockbox Winamp player.
 * Hosts the WinampView and manages app lifecycle.
 */
public class MainActivity extends Activity {

    private WinampView winampView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar and make fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Keep screen on during playback
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create and set the custom Winamp view
        winampView = new WinampView(this);
        setContentView(winampView);

        // Request focus for keyboard input
        winampView.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (winampView != null) {
            winampView.requestFocus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause playback when app goes to background
        if (winampView != null && winampView.getPlaybackController() != null) {
            winampView.getPlaybackController().pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (winampView != null) {
            winampView.cleanup();
        }
    }
}
