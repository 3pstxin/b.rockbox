package com.rockbox.winamp.audio;

import android.content.Context;
import android.util.Log;

/**
 * Coordinates playback between AudioEngine and Playlist.
 * Implements Winamp-like playback logic.
 */
public class PlaybackController implements AudioEngine.AudioEngineListener,
                                           Playlist.PlaylistListener {

    private static final String TAG = "PlaybackController";

    private Context context;
    private AudioEngine audioEngine;
    private Playlist playlist;

    public PlaybackController(Context context) {
        this.context = context;
        this.audioEngine = new AudioEngine(context);
        this.playlist = new Playlist();

        // Wire listeners
        audioEngine.addListener(this);
        playlist.addListener(this);

        Log.d(TAG, "PlaybackController initialized");
    }

    /**
     * Play current track in playlist
     */
    public void play() {
        Track track = playlist.getCurrentTrack();
        if (track != null) {
            if (audioEngine.isPaused() && track.equals(audioEngine.getCurrentTrack())) {
                // Resume current track
                audioEngine.play();
            } else {
                // Load and play new track
                audioEngine.loadTrack(track);
            }
        } else {
            Log.w(TAG, "No track to play");
        }
    }

    /**
     * Pause playback
     */
    public void pause() {
        audioEngine.pause();
    }

    /**
     * Stop playback
     */
    public void stop() {
        audioEngine.stop();
    }

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        if (audioEngine.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Next track
     */
    public void next() {
        if (playlist.next()) {
            play();
        } else {
            Log.d(TAG, "At end of playlist");
            stop();
        }
    }

    /**
     * Previous track
     */
    public void previous() {
        // If more than 3 seconds into track, restart current track
        if (audioEngine.getCurrentPosition() > 3000) {
            audioEngine.seekTo(0);
        } else {
            // Otherwise go to previous track
            if (playlist.previous()) {
                play();
            } else {
                Log.d(TAG, "At beginning of playlist");
            }
        }
    }

    /**
     * Seek to position (0-100%)
     */
    public void seekToPercent(float percent) {
        int duration = audioEngine.getDuration();
        if (duration > 0) {
            int position = (int) (duration * Math.max(0, Math.min(1, percent)));
            audioEngine.seekTo(position);
        }
    }

    /**
     * Set volume (0-100)
     */
    public void setVolume(int volume) {
        float normalizedVolume = Math.max(0, Math.min(100, volume)) / 100.0f;
        audioEngine.setVolume(normalizedVolume);
    }

    /**
     * Get volume (0-100)
     */
    public int getVolume() {
        return (int) (audioEngine.getVolume() * 100);
    }

    /**
     * Toggle shuffle
     */
    public void toggleShuffle() {
        playlist.setShuffle(!playlist.isShuffle());
    }

    /**
     * Set shuffle
     */
    public void setShuffle(boolean enabled) {
        playlist.setShuffle(enabled);
    }

    /**
     * Get shuffle state
     */
    public boolean isShuffle() {
        return playlist.isShuffle();
    }

    /**
     * Cycle repeat mode (Off -> All -> One -> Off)
     */
    public void cycleRepeat() {
        playlist.cycleRepeatMode();
    }

    /**
     * Get repeat mode
     */
    public int getRepeatMode() {
        return playlist.getRepeatMode();
    }

    /**
     * Get playlist
     */
    public Playlist getPlaylist() {
        return playlist;
    }

    /**
     * Get audio engine
     */
    public AudioEngine getAudioEngine() {
        return audioEngine;
    }

    /**
     * Get current track
     */
    public Track getCurrentTrack() {
        return playlist.getCurrentTrack();
    }

    /**
     * Check if playing
     */
    public boolean isPlaying() {
        return audioEngine.isPlaying();
    }

    /**
     * Check if paused
     */
    public boolean isPaused() {
        return audioEngine.isPaused();
    }

    /**
     * Get current position in seconds
     */
    public int getCurrentPositionSeconds() {
        return audioEngine.getCurrentPosition() / 1000;
    }

    /**
     * Get duration in seconds
     */
    public int getDurationSeconds() {
        return audioEngine.getDuration() / 1000;
    }

    /**
     * Notify progress (call periodically from UI)
     */
    public void notifyProgress() {
        audioEngine.notifyProgress();
    }

    /**
     * Release resources
     */
    public void release() {
        audioEngine.release();
        Log.d(TAG, "PlaybackController released");
    }

    // AudioEngine.AudioEngineListener implementation
    @Override
    public void onPlaybackStarted(Track track) {
        Log.d(TAG, "Playback started: " + track.getDisplayString());
    }

    @Override
    public void onPlaybackPaused() {
        Log.d(TAG, "Playback paused");
    }

    @Override
    public void onPlaybackStopped() {
        Log.d(TAG, "Playback stopped");
    }

    @Override
    public void onPlaybackCompleted(Track track) {
        Log.d(TAG, "Playback completed: " + track.getDisplayString());
        // Auto-advance to next track
        next();
    }

    @Override
    public void onPlaybackError(String error) {
        Log.e(TAG, "Playback error: " + error);
        // Try next track on error
        next();
    }

    @Override
    public void onProgressUpdate(int currentMs, int durationMs) {
        // Progress updates are forwarded to UI by getters
    }

    // Playlist.PlaylistListener implementation
    @Override
    public void onPlaylistChanged() {
        Log.d(TAG, "Playlist changed: " + playlist.size() + " tracks");
    }

    @Override
    public void onCurrentTrackChanged(Track track) {
        Log.d(TAG, "Current track changed: " + (track != null ? track.getDisplayString() : "null"));
    }
}
