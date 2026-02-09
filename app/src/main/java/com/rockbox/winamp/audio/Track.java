package com.rockbox.winamp.audio;

import java.io.File;

/**
 * Represents an audio track with metadata.
 */
public class Track {

    private String filePath;
    private String title;
    private String artist;
    private String album;
    private int durationMs;
    private long fileSize;

    public Track(String filePath) {
        this.filePath = filePath;
        this.title = null;
        this.artist = null;
        this.album = null;
        this.durationMs = 0;
        this.fileSize = 0;

        // Extract basic info from file
        File file = new File(filePath);
        if (file.exists()) {
            this.fileSize = file.length();
            // Use filename as default title
            this.title = file.getName();
            if (this.title.lastIndexOf('.') > 0) {
                this.title = this.title.substring(0, this.title.lastIndexOf('.'));
            }
        }
    }

    // Getters
    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title != null ? title : "Unknown";
    }

    public String getArtist() {
        return artist != null ? artist : "Unknown Artist";
    }

    public String getAlbum() {
        return album != null ? album : "Unknown Album";
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getDurationSeconds() {
        return durationMs / 1000;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return new File(filePath).getName();
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Get display string for UI
     * Format: "Artist - Title"
     */
    public String getDisplayString() {
        if (artist != null && !artist.equals("Unknown Artist")) {
            return artist + " - " + getTitle();
        }
        return getTitle();
    }

    /**
     * Get formatted duration string
     * Format: "3:45"
     */
    public String getFormattedDuration() {
        int seconds = getDurationSeconds();
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    /**
     * Get formatted file size
     * Format: "3.5 MB"
     */
    public String getFormattedSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        return getDisplayString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Track)) return false;
        Track other = (Track) obj;
        return filePath != null && filePath.equals(other.filePath);
    }

    @Override
    public int hashCode() {
        return filePath != null ? filePath.hashCode() : 0;
    }
}
