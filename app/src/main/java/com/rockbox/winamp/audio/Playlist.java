package com.rockbox.winamp.audio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the playlist of tracks.
 * Supports shuffle, repeat, and .m3u playlist format.
 */
public class Playlist {

    private static final String TAG = "Playlist";

    // Repeat modes
    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    private List<Track> tracks;
    private List<Track> shuffledTracks;
    private int currentIndex;
    private boolean shuffleEnabled;
    private int repeatMode;
    private Random random;

    // Listeners
    private List<PlaylistListener> listeners;

    public interface PlaylistListener {
        void onPlaylistChanged();
        void onCurrentTrackChanged(Track track);
    }

    public Playlist() {
        this.tracks = new ArrayList<Track>();
        this.shuffledTracks = new ArrayList<Track>();
        this.currentIndex = 0;
        this.shuffleEnabled = false;
        this.repeatMode = REPEAT_OFF;
        this.random = new Random();
        this.listeners = new ArrayList<PlaylistListener>();
    }

    /**
     * Add a track to the playlist
     */
    public void addTrack(Track track) {
        if (track != null && !tracks.contains(track)) {
            tracks.add(track);
            if (shuffleEnabled) {
                regenerateShuffle();
            }
            notifyPlaylistChanged();
            Log.d(TAG, "Track added: " + track.getDisplayString());
        }
    }

    /**
     * Add multiple tracks
     */
    public void addTracks(List<Track> newTracks) {
        for (Track track : newTracks) {
            if (track != null && !tracks.contains(track)) {
                tracks.add(track);
            }
        }
        if (shuffleEnabled) {
            regenerateShuffle();
        }
        notifyPlaylistChanged();
        Log.d(TAG, "Added " + newTracks.size() + " tracks");
    }

    /**
     * Remove a track by index
     */
    public void removeTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            Track removed = tracks.remove(index);
            if (shuffleEnabled) {
                regenerateShuffle();
            }
            if (index <= currentIndex && currentIndex > 0) {
                currentIndex--;
            }
            notifyPlaylistChanged();
            Log.d(TAG, "Track removed: " + removed.getDisplayString());
        }
    }

    /**
     * Remove a track by object
     */
    public void removeTrack(Track track) {
        int index = tracks.indexOf(track);
        if (index >= 0) {
            removeTrack(index);
        }
    }

    /**
     * Clear all tracks
     */
    public void clear() {
        tracks.clear();
        shuffledTracks.clear();
        currentIndex = 0;
        notifyPlaylistChanged();
        Log.d(TAG, "Playlist cleared");
    }

    /**
     * Get current track
     */
    public Track getCurrentTrack() {
        List<Track> activeList = shuffleEnabled ? shuffledTracks : tracks;
        if (activeList.isEmpty() || currentIndex < 0 || currentIndex >= activeList.size()) {
            return null;
        }
        return activeList.get(currentIndex);
    }

    /**
     * Get track at index
     */
    public Track getTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            return tracks.get(index);
        }
        return null;
    }

    /**
     * Get all tracks
     */
    public List<Track> getTracks() {
        return new ArrayList<Track>(tracks);
    }

    /**
     * Get playlist size
     */
    public int size() {
        return tracks.size();
    }

    /**
     * Check if playlist is empty
     */
    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    /**
     * Get current track index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Set current track index
     */
    public void setCurrentIndex(int index) {
        List<Track> activeList = shuffleEnabled ? shuffledTracks : tracks;
        if (index >= 0 && index < activeList.size()) {
            currentIndex = index;
            notifyCurrentTrackChanged();
        }
    }

    /**
     * Move to next track
     * @return true if moved to next track, false if at end
     */
    public boolean next() {
        List<Track> activeList = shuffleEnabled ? shuffledTracks : tracks;
        if (activeList.isEmpty()) return false;

        if (repeatMode == REPEAT_ONE) {
            // Stay on current track
            notifyCurrentTrackChanged();
            return true;
        }

        if (currentIndex < activeList.size() - 1) {
            currentIndex++;
            notifyCurrentTrackChanged();
            return true;
        } else if (repeatMode == REPEAT_ALL) {
            currentIndex = 0;
            notifyCurrentTrackChanged();
            return true;
        }

        return false;
    }

    /**
     * Move to previous track
     * @return true if moved to previous track, false if at beginning
     */
    public boolean previous() {
        List<Track> activeList = shuffleEnabled ? shuffledTracks : tracks;
        if (activeList.isEmpty()) return false;

        if (repeatMode == REPEAT_ONE) {
            // Stay on current track
            notifyCurrentTrackChanged();
            return true;
        }

        if (currentIndex > 0) {
            currentIndex--;
            notifyCurrentTrackChanged();
            return true;
        } else if (repeatMode == REPEAT_ALL) {
            currentIndex = activeList.size() - 1;
            notifyCurrentTrackChanged();
            return true;
        }

        return false;
    }

    /**
     * Toggle shuffle mode
     */
    public void setShuffle(boolean enabled) {
        if (shuffleEnabled == enabled) return;

        Track currentTrack = getCurrentTrack();
        shuffleEnabled = enabled;

        if (enabled) {
            regenerateShuffle();
            // Find current track in shuffled list
            if (currentTrack != null) {
                currentIndex = shuffledTracks.indexOf(currentTrack);
                if (currentIndex < 0) currentIndex = 0;
            }
        } else {
            // Find current track in original list
            if (currentTrack != null) {
                currentIndex = tracks.indexOf(currentTrack);
                if (currentIndex < 0) currentIndex = 0;
            }
        }

        Log.d(TAG, "Shuffle " + (enabled ? "enabled" : "disabled"));
        notifyPlaylistChanged();
    }

    /**
     * Get shuffle state
     */
    public boolean isShuffle() {
        return shuffleEnabled;
    }

    /**
     * Set repeat mode
     */
    public void setRepeatMode(int mode) {
        if (mode >= REPEAT_OFF && mode <= REPEAT_ONE) {
            repeatMode = mode;
            Log.d(TAG, "Repeat mode: " + mode);
        }
    }

    /**
     * Cycle to next repeat mode
     */
    public void cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        Log.d(TAG, "Repeat mode: " + repeatMode);
    }

    /**
     * Get repeat mode
     */
    public int getRepeatMode() {
        return repeatMode;
    }

    /**
     * Regenerate shuffle order
     */
    private void regenerateShuffle() {
        shuffledTracks.clear();
        shuffledTracks.addAll(tracks);
        Collections.shuffle(shuffledTracks, random);
    }

    /**
     * Load playlist from .m3u file
     */
    public boolean loadFromM3U(File file) {
        if (file == null || !file.exists()) {
            Log.e(TAG, "M3U file not found");
            return false;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int loaded = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check if file exists
                File trackFile = new File(line);
                if (!trackFile.isAbsolute()) {
                    // Relative path - resolve from m3u location
                    trackFile = new File(file.getParentFile(), line);
                }

                if (trackFile.exists()) {
                    Track track = new Track(trackFile.getAbsolutePath());
                    addTrack(track);
                    loaded++;
                }
            }

            Log.i(TAG, "Loaded " + loaded + " tracks from " + file.getName());
            return loaded > 0;

        } catch (IOException e) {
            Log.e(TAG, "Error loading M3U", e);
            return false;
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
     * Save playlist to .m3u file
     */
    public boolean saveToM3U(File file) {
        if (file == null || tracks.isEmpty()) {
            return false;
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("#EXTM3U\n");

            for (Track track : tracks) {
                // Write extended info
                writer.write("#EXTINF:");
                writer.write(String.valueOf(track.getDurationSeconds()));
                writer.write(",");
                writer.write(track.getDisplayString());
                writer.write("\n");

                // Write file path
                writer.write(track.getFilePath());
                writer.write("\n");
            }

            Log.i(TAG, "Saved playlist to " + file.getName());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error saving M3U", e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Add listener
     */
    public void addListener(PlaylistListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove listener
     */
    public void removeListener(PlaylistListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify listeners of playlist change
     */
    private void notifyPlaylistChanged() {
        for (PlaylistListener listener : listeners) {
            listener.onPlaylistChanged();
        }
    }

    /**
     * Notify listeners of current track change
     */
    private void notifyCurrentTrackChanged() {
        Track track = getCurrentTrack();
        for (PlaylistListener listener : listeners) {
            listener.onCurrentTrackChanged(track);
        }
    }
}
