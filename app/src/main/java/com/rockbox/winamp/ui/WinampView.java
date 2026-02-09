package com.rockbox.winamp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.rockbox.winamp.audio.PlaybackController;
import com.rockbox.winamp.audio.Track;
import com.rockbox.winamp.skin.SkinAssets;
import com.rockbox.winamp.skin.SkinLoader;
import com.rockbox.winamp.skin.SkinRenderer;
import com.rockbox.winamp.util.FilePicker;
import com.rockbox.winamp.util.TrackBrowser;

import java.io.File;
import java.util.List;

/**
 * Custom SurfaceView that renders the Winamp UI using Canvas.
 * Handles keyboard/trackpad input and delegates rendering to SkinRenderer.
 */
public class WinampView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "WinampView";

    private Context context;
    private SurfaceHolder holder;
    private RenderThread renderThread;
    private SkinRenderer skinRenderer;
    private SkinLoader skinLoader;
    private PlaybackController playbackController;
    private KeyboardHandler keyboardHandler;
    private FocusManager focusManager;

    public WinampView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGB_565); // Optimize for performance

        setFocusable(true);
        setFocusableInTouchMode(true);

        skinRenderer = new SkinRenderer();
        skinLoader = new SkinLoader(context);
        playbackController = new PlaybackController(context);
        keyboardHandler = new KeyboardHandler(context);
        focusManager = new FocusManager();

        // Set up keyboard listener
        keyboardHandler.setListener(new KeyboardHandler.KeyboardListener() {
            public void onAction(int action) {
                handleKeyboardAction(action);
            }

            public void onLongPressAction(int action) {
                handleLongPressAction(action);
            }
        });

        // Load default skin (no bitmaps, uses primitives)
        loadDefaultSkin();

        // Set initial volume
        playbackController.setVolume(50);

        // Initialize focus on first button
        focusManager.setFocus(FocusManager.ELEMENT_PLAY_BUTTON);

        // Show startup message
        post(new Runnable() {
            public void run() {
                Toast.makeText(context, "Rockbox Winamp Started! Press A to add music, H for help", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Load default embedded skin
     */
    private void loadDefaultSkin() {
        SkinAssets assets = skinLoader.loadDefaultSkin();
        skinRenderer.setSkinAssets(assets);
        Log.i(TAG, "Default skin loaded");
    }

    /**
     * Load custom .wsz skin from file
     */
    private void loadSkin(final File wszFile) {
        if (wszFile == null || !wszFile.exists()) {
            Toast.makeText(context, "Skin file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading message
        post(new Runnable() {
            public void run() {
                Toast.makeText(context, "Loading skin...", Toast.LENGTH_SHORT).show();
            }
        });

        // Load skin in background to avoid blocking UI
        new Thread(new Runnable() {
            public void run() {
                final SkinAssets assets = skinLoader.loadSkin(wszFile.getAbsolutePath());

                post(new Runnable() {
                    public void run() {
                        if (assets != null && assets.isLoaded()) {
                            skinRenderer.setSkinAssets(assets);
                            Toast.makeText(context, "Skin loaded: " + assets.getSkinName(), Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Skin loaded: " + assets.getSkinName() + " (" + assets.getBitmapCount() + " bitmaps)");
                        } else {
                            Toast.makeText(context, "Failed to load skin", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to load skin from: " + wszFile);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Show skin file picker dialog
     */
    private void showSkinPicker() {
        FilePicker.showQuickPicker(context, new FilePicker.FilePickerCallback() {
            public void onFileSelected(File file) {
                loadSkin(file);
            }

            public void onCancelled() {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderThread = new RenderThread();
        renderThread.setRunning(true);
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Update renderer with display size
        skinRenderer.setDisplaySize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        renderThread.setRunning(false);
        while (retry) {
            try {
                renderThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // Retry
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Delegate to keyboard handler
        if (keyboardHandler.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Delegate to keyboard handler
        if (keyboardHandler.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Handle keyboard action from KeyboardHandler
     */
    private void handleKeyboardAction(int action) {
        switch (action) {
            case KeyboardHandler.ACTION_PLAY_PAUSE:
                playbackController.togglePlayPause();
                break;
            case KeyboardHandler.ACTION_PLAY:
                playbackController.play();
                break;
            case KeyboardHandler.ACTION_PAUSE:
                playbackController.pause();
                break;
            case KeyboardHandler.ACTION_STOP:
                playbackController.stop();
                break;
            case KeyboardHandler.ACTION_NEXT:
                playbackController.next();
                break;
            case KeyboardHandler.ACTION_PREVIOUS:
                playbackController.previous();
                break;
            case KeyboardHandler.ACTION_VOLUME_UP:
                volumeUp();
                break;
            case KeyboardHandler.ACTION_VOLUME_DOWN:
                volumeDown();
                break;
            case KeyboardHandler.ACTION_ADD_TRACKS:
                showTrackBrowser();
                break;
            case KeyboardHandler.ACTION_CLEAR_PLAYLIST:
                playbackController.getPlaylist().clear();
                Toast.makeText(context, "Playlist cleared", Toast.LENGTH_SHORT).show();
                break;
            case KeyboardHandler.ACTION_TOGGLE_SHUFFLE:
                playbackController.toggleShuffle();
                Toast.makeText(context, "Shuffle: " + (playbackController.isShuffle() ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                break;
            case KeyboardHandler.ACTION_CYCLE_REPEAT:
                playbackController.cycleRepeat();
                String[] repeatModes = {"OFF", "ALL", "ONE"};
                Toast.makeText(context, "Repeat: " + repeatModes[playbackController.getRepeatMode()], Toast.LENGTH_SHORT).show();
                break;
            case KeyboardHandler.ACTION_LOAD_SKIN:
                showSkinPicker();
                break;
            case KeyboardHandler.ACTION_DEFAULT_SKIN:
                loadDefaultSkin();
                Toast.makeText(context, "Default skin", Toast.LENGTH_SHORT).show();
                break;
            case KeyboardHandler.ACTION_SHOW_HELP:
                KeyboardHelpDialog.show(context, keyboardHandler);
                break;
            case KeyboardHandler.ACTION_SEEK_FORWARD:
                playbackController.seekToPercent(
                    (float)(playbackController.getCurrentPositionSeconds() + 5) / playbackController.getDurationSeconds()
                );
                break;
            case KeyboardHandler.ACTION_SEEK_BACKWARD:
                playbackController.seekToPercent(
                    (float)(playbackController.getCurrentPositionSeconds() - 5) / playbackController.getDurationSeconds()
                );
                break;
            case KeyboardHandler.ACTION_FOCUS_UP:
                focusManager.focusUp();
                updateFocusVisualization();
                break;
            case KeyboardHandler.ACTION_FOCUS_DOWN:
                focusManager.focusDown();
                updateFocusVisualization();
                break;
            case KeyboardHandler.ACTION_FOCUS_LEFT:
                focusManager.focusLeft();
                updateFocusVisualization();
                break;
            case KeyboardHandler.ACTION_FOCUS_RIGHT:
                focusManager.focusRight();
                updateFocusVisualization();
                break;
            case KeyboardHandler.ACTION_ACTIVATE_FOCUSED:
                activateFocused();
                break;
        }
    }

    /**
     * Handle long-press action
     */
    private void handleLongPressAction(int action) {
        switch (action) {
            case KeyboardHandler.ACTION_STOP:
                playbackController.stop();
                Toast.makeText(context, "Stopped (long-press)", Toast.LENGTH_SHORT).show();
                break;
            case KeyboardHandler.ACTION_SEEK_FORWARD:
                // Seek forward 10 seconds
                playbackController.seekToPercent(
                    (float)(playbackController.getCurrentPositionSeconds() + 10) / playbackController.getDurationSeconds()
                );
                break;
            case KeyboardHandler.ACTION_SEEK_BACKWARD:
                // Seek backward 10 seconds
                playbackController.seekToPercent(
                    (float)(playbackController.getCurrentPositionSeconds() - 10) / playbackController.getDurationSeconds()
                );
                break;
        }
    }

    /**
     * Update focus visualization in renderer
     */
    private void updateFocusVisualization() {
        FocusManager.FocusableElement elem = focusManager.getCurrentElement();
        if (elem != null) {
            // Map element type to button index
            int buttonIndex = -1;
            switch (elem.type) {
                case FocusManager.ELEMENT_PREV_BUTTON:
                    buttonIndex = 0;
                    break;
                case FocusManager.ELEMENT_PLAY_BUTTON:
                    buttonIndex = 1;
                    break;
                case FocusManager.ELEMENT_PAUSE_BUTTON:
                    buttonIndex = 2;
                    break;
                case FocusManager.ELEMENT_STOP_BUTTON:
                    buttonIndex = 3;
                    break;
                case FocusManager.ELEMENT_NEXT_BUTTON:
                    buttonIndex = 4;
                    break;
            }
            skinRenderer.setFocusedButton(buttonIndex);
        }
    }

    /**
     * Activate currently focused element
     */
    private void activateFocused() {
        FocusManager.FocusableElement elem = focusManager.getCurrentElement();
        if (elem == null) return;

        switch (elem.type) {
            case FocusManager.ELEMENT_PREV_BUTTON:
                playbackController.previous();
                break;
            case FocusManager.ELEMENT_PLAY_BUTTON:
                playbackController.play();
                break;
            case FocusManager.ELEMENT_PAUSE_BUTTON:
                playbackController.pause();
                break;
            case FocusManager.ELEMENT_STOP_BUTTON:
                playbackController.stop();
                break;
            case FocusManager.ELEMENT_NEXT_BUTTON:
                playbackController.next();
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Handle touch/trackpad events
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            requestFocus();

            // Check if button was clicked
            int buttonId = skinRenderer.hitTest((int)event.getX(), (int)event.getY());
            switch (buttonId) {
                case 0: // PREV
                    playbackController.previous();
                    break;
                case 1: // PLAY
                    playbackController.play();
                    break;
                case 2: // PAUSE
                    playbackController.pause();
                    break;
                case 3: // STOP
                    playbackController.stop();
                    break;
                case 4: // NEXT
                    playbackController.next();
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void drawFrame(Canvas canvas) {
        if (canvas == null) return;

        // Update renderer state from playback controller
        Track currentTrack = playbackController.getCurrentTrack();
        if (currentTrack != null) {
            skinRenderer.setTrackTitle(currentTrack.getDisplayString());
        } else {
            skinRenderer.setTrackTitle("No track loaded");
        }

        skinRenderer.setPlaybackState(
            playbackController.isPlaying(),
            playbackController.isPaused()
        );
        skinRenderer.setCurrentTime(playbackController.getCurrentPositionSeconds());
        skinRenderer.setTotalTime(playbackController.getDurationSeconds());
        skinRenderer.setVolume(playbackController.getVolume());
        skinRenderer.setShuffle(playbackController.isShuffle());
        skinRenderer.setRepeat(playbackController.getRepeatMode() != 0);

        // Delegate rendering to SkinRenderer
        skinRenderer.draw(canvas);
    }

    /**
     * Show track browser to add music to playlist
     */
    private void showTrackBrowser() {
        TrackBrowser.showQuickBrowser(context, new TrackBrowser.TrackBrowserCallback() {
            public void onTracksSelected(List<Track> tracks) {
                playbackController.getPlaylist().addTracks(tracks);
                Toast.makeText(context, "Added " + tracks.size() + " track(s)", Toast.LENGTH_SHORT).show();

                // Auto-play if playlist was empty
                if (playbackController.getPlaylist().size() == tracks.size()) {
                    playbackController.play();
                }
            }

            public void onCancelled() {
                // User cancelled
            }
        });
    }

    private void volumeUp() {
        int newVolume = Math.min(100, playbackController.getVolume() + 5);
        playbackController.setVolume(newVolume);
    }

    private void volumeDown() {
        int newVolume = Math.max(0, playbackController.getVolume() - 5);
        playbackController.setVolume(newVolume);
    }

    /**
     * Release resources when view is destroyed
     */
    public void cleanup() {
        if (playbackController != null) {
            playbackController.release();
        }
    }

    /**
     * Get playback controller for external access
     */
    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    /**
     * Rendering thread that continuously draws to the surface
     */
    private class RenderThread extends Thread {
        private boolean running = false;

        public void setRunning(boolean run) {
            running = run;
        }

        @Override
        public void run() {
            while (running) {
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    synchronized (holder) {
                        drawFrame(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }

                // Notify progress update
                if (playbackController != null) {
                    playbackController.notifyProgress();
                }

                // Target 30 FPS (conservative for BB Classic)
                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    // Continue
                }
            }
        }
    }
}
