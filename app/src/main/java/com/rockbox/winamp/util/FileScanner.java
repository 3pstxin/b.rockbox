package com.rockbox.winamp.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;

import com.rockbox.winamp.audio.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Scans device storage for audio files.
 * Supports MP3, FLAC, OGG, WAV formats.
 */
public class FileScanner {

    private static final String TAG = "FileScanner";

    // Supported audio extensions
    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".flac", ".ogg", ".wav", ".m4a", ".aac"
    };

    // Common music directories
    private static final String[] MUSIC_DIRS = {
        "Music", "music", "Audio", "audio", "Songs", "songs"
    };

    private Context context;

    public FileScanner(Context context) {
        this.context = context;
    }

    /**
     * Scan default music directories for audio files
     */
    public List<Track> scanMusicLibrary() {
        List<Track> tracks = new ArrayList<Track>();

        // Scan external storage
        File externalStorage = Environment.getExternalStorageDirectory();
        if (externalStorage != null && externalStorage.exists()) {
            // Scan Music directory
            File musicDir = new File(externalStorage, "Music");
            if (musicDir.exists() && musicDir.isDirectory()) {
                scanDirectory(musicDir, tracks, true);
            }

            // Scan Download directory
            File downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            );
            if (downloadDir != null && downloadDir.exists()) {
                scanDirectory(downloadDir, tracks, false); // Non-recursive for downloads
            }
        }

        Log.i(TAG, "Scan complete: " + tracks.size() + " tracks found");
        return tracks;
    }

    /**
     * Scan specific directory for audio files
     *
     * @param directory Directory to scan
     * @param recursive Scan subdirectories
     */
    public List<Track> scanDirectory(File directory, boolean recursive) {
        List<Track> tracks = new ArrayList<Track>();
        scanDirectory(directory, tracks, recursive);
        return tracks;
    }

    /**
     * Internal recursive scan method
     */
    private void scanDirectory(File directory, List<Track> tracks, boolean recursive) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        // Sort files alphabetically
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        for (File file : files) {
            if (file.isDirectory() && recursive) {
                // Recursively scan subdirectory
                scanDirectory(file, tracks, true);
            } else if (file.isFile() && isAudioFile(file)) {
                // Create track and extract metadata
                Track track = new Track(file.getAbsolutePath());
                extractMetadata(track);
                tracks.add(track);
            }
        }
    }

    /**
     * Check if file is a supported audio format
     */
    public static boolean isAudioFile(File file) {
        if (file == null || !file.isFile()) return false;

        String name = file.getName().toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract metadata from audio file
     */
    public void extractMetadata(Track track) {
        if (track == null) return;

        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(track.getFilePath());

            // Extract title
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null && !title.isEmpty()) {
                track.setTitle(title);
            }

            // Extract artist
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist != null && !artist.isEmpty()) {
                track.setArtist(artist);
            }

            // Extract album
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (album != null && !album.isEmpty()) {
                track.setAlbum(album);
            }

            // Extract duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                try {
                    int duration = Integer.parseInt(durationStr);
                    track.setDurationMs(duration);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Could not extract metadata from: " + track.getFileName(), e);
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Get default music directory
     */
    public static File getDefaultMusicDirectory() {
        File musicDir = new File(Environment.getExternalStorageDirectory(), "Music");
        if (!musicDir.exists()) {
            musicDir.mkdirs();
        }
        return musicDir;
    }

    /**
     * List audio files in directory (non-recursive)
     */
    public static File[] listAudioFiles(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return new File[0];
        }

        return directory.listFiles(new java.io.FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && isAudioFile(file);
            }
        });
    }
}
