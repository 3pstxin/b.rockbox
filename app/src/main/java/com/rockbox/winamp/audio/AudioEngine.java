package com.rockbox.winamp.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Audio playback engine wrapping Android MediaPlayer.
 * Handles audio decoding, playback, and audio focus.
 */
public class AudioEngine implements MediaPlayer.OnCompletionListener,
                                     MediaPlayer.OnErrorListener,
                                     MediaPlayer.OnPreparedListener {

    private static final String TAG = "AudioEngine";

    // Playback states
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_STOPPED = 5;
    public static final int STATE_ERROR = 6;

    private Context context;
    private MediaPlayer mediaPlayer;
    private Track currentTrack;
    private int state;
    private float volume;
    private Equalizer equalizer;

    // Listeners
    private List<AudioEngineListener> listeners;

    public interface AudioEngineListener {
        void onPlaybackStarted(Track track);
        void onPlaybackPaused();
        void onPlaybackStopped();
        void onPlaybackCompleted(Track track);
        void onPlaybackError(String error);
        void onProgressUpdate(int currentMs, int durationMs);
    }

    public AudioEngine(Context context) {
        this.context = context.getApplicationContext();
        this.state = STATE_IDLE;
        this.volume = 1.0f;
        this.listeners = new ArrayList<AudioEngineListener>();
        initMediaPlayer();
    }

    /**
     * Initialize MediaPlayer
     */
    private void initMediaPlayer() {
        if (mediaPlayer != null) {
            release();
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setVolume(volume, volume);

        // Initialize equalizer with audio session
        int audioSessionId = mediaPlayer.getAudioSessionId();
        if (equalizer != null) {
            equalizer.release();
        }
        equalizer = new Equalizer(audioSessionId);

        Log.d(TAG, "MediaPlayer initialized (session: " + audioSessionId + ")");
    }

    /**
     * Load and prepare a track for playback
     */
    public void loadTrack(Track track) {
        if (track == null) {
            Log.e(TAG, "Cannot load null track");
            return;
        }

        try {
            // Stop current playback
            stop();

            // Reset player
            mediaPlayer.reset();
            state = STATE_IDLE;

            // Set data source
            mediaPlayer.setDataSource(track.getFilePath());

            // Prepare asynchronously
            state = STATE_PREPARING;
            currentTrack = track;
            mediaPlayer.prepareAsync();

            Log.d(TAG, "Loading track: " + track.getDisplayString());

        } catch (IOException e) {
            Log.e(TAG, "Error loading track", e);
            state = STATE_ERROR;
            notifyPlaybackError("Failed to load track: " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal state when loading track", e);
            initMediaPlayer(); // Reinitialize
            state = STATE_ERROR;
            notifyPlaybackError("Player error, reinitialized");
        }
    }

    /**
     * Start or resume playback
     */
    public void play() {
        if (state == STATE_PREPARED || state == STATE_PAUSED) {
            try {
                mediaPlayer.start();
                state = STATE_PLAYING;
                notifyPlaybackStarted();
                Log.d(TAG, "Playback started");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error starting playback", e);
                notifyPlaybackError("Cannot start playback");
            }
        } else {
            Log.w(TAG, "Cannot play in state: " + state);
        }
    }

    /**
     * Pause playback
     */
    public void pause() {
        if (state == STATE_PLAYING) {
            try {
                mediaPlayer.pause();
                state = STATE_PAUSED;
                notifyPlaybackPaused();
                Log.d(TAG, "Playback paused");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error pausing playback", e);
            }
        }
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (state == STATE_PLAYING || state == STATE_PAUSED || state == STATE_PREPARED) {
            try {
                mediaPlayer.stop();
                state = STATE_STOPPED;
                notifyPlaybackStopped();
                Log.d(TAG, "Playback stopped");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping playback", e);
            }
        }
    }

    /**
     * Seek to position in milliseconds
     */
    public void seekTo(int positionMs) {
        if (state == STATE_PREPARED || state == STATE_PLAYING || state == STATE_PAUSED) {
            try {
                mediaPlayer.seekTo(positionMs);
                Log.d(TAG, "Seeked to " + positionMs + "ms");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error seeking", e);
            }
        }
    }

    /**
     * Get current playback position in milliseconds
     */
    public int getCurrentPosition() {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Get track duration in milliseconds
     */
    public int getDuration() {
        if (state == STATE_PREPARED || state == STATE_PLAYING || state == STATE_PAUSED) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Set volume (0.0 - 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setVolume(this.volume, this.volume);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error setting volume", e);
            }
        }
    }

    /**
     * Get current volume (0.0 - 1.0)
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Check if currently playing
     */
    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }

    /**
     * Check if paused
     */
    public boolean isPaused() {
        return state == STATE_PAUSED;
    }

    /**
     * Get current playback state
     */
    public int getState() {
        return state;
    }

    /**
     * Get current track
     */
    public Track getCurrentTrack() {
        return currentTrack;
    }

    /**
     * Get equalizer
     */
    public Equalizer getEqualizer() {
        return equalizer;
    }

    /**
     * Release resources
     */
    public void release() {
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }

        if (mediaPlayer != null) {
            try {
                if (state == STATE_PLAYING) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error releasing MediaPlayer", e);
            }
            mediaPlayer = null;
        }
        state = STATE_IDLE;
        currentTrack = null;
        Log.d(TAG, "AudioEngine released");
    }

    // MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "Playback completed");
        state = STATE_STOPPED;
        notifyPlaybackCompleted();
    }

    // MediaPlayer.OnErrorListener
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        state = STATE_ERROR;
        notifyPlaybackError("Playback error: " + what);

        // Reinitialize player
        initMediaPlayer();
        return true; // Handled
    }

    // MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Track prepared: " + currentTrack.getDisplayString());
        state = STATE_PREPARED;

        // Update track duration
        if (currentTrack != null) {
            int duration = getDuration();
            currentTrack.setDurationMs(duration);
        }

        // Auto-play after preparing
        play();
    }

    /**
     * Add listener
     */
    public void addListener(AudioEngineListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove listener
     */
    public void removeListener(AudioEngineListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify progress update (call from UI thread periodically)
     */
    public void notifyProgress() {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            int current = getCurrentPosition();
            int duration = getDuration();
            for (AudioEngineListener listener : listeners) {
                listener.onProgressUpdate(current, duration);
            }
        }
    }

    // Notification methods
    private void notifyPlaybackStarted() {
        for (AudioEngineListener listener : listeners) {
            listener.onPlaybackStarted(currentTrack);
        }
    }

    private void notifyPlaybackPaused() {
        for (AudioEngineListener listener : listeners) {
            listener.onPlaybackPaused();
        }
    }

    private void notifyPlaybackStopped() {
        for (AudioEngineListener listener : listeners) {
            listener.onPlaybackStopped();
        }
    }

    private void notifyPlaybackCompleted() {
        Track completedTrack = currentTrack;
        for (AudioEngineListener listener : listeners) {
            listener.onPlaybackCompleted(completedTrack);
        }
    }

    private void notifyPlaybackError(String error) {
        for (AudioEngineListener listener : listeners) {
            listener.onPlaybackError(error);
        }
    }
}
