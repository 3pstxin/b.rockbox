package com.rockbox.winamp.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

import com.rockbox.winamp.audio.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Simple browser for selecting audio files to add to playlist.
 */
public class TrackBrowser {

    private static final String TAG = "TrackBrowser";

    public interface TrackBrowserCallback {
        void onTracksSelected(List<Track> tracks);
        void onCancelled();
    }

    private Context context;
    private File currentDirectory;
    private FileScanner fileScanner;

    public TrackBrowser(Context context) {
        this.context = context;
        this.fileScanner = new FileScanner(context);
        this.currentDirectory = FileScanner.getDefaultMusicDirectory();
    }

    /**
     * Show track browser dialog
     */
    public void show(TrackBrowserCallback callback) {
        showDirectoryDialog(currentDirectory, callback);
    }

    /**
     * Show browser starting from specific directory
     */
    public void show(File startDirectory, TrackBrowserCallback callback) {
        this.currentDirectory = startDirectory;
        showDirectoryDialog(currentDirectory, callback);
    }

    /**
     * Show directory contents
     */
    private void showDirectoryDialog(final File directory, final TrackBrowserCallback callback) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Log.e(TAG, "Invalid directory: " + directory);
            if (callback != null) {
                callback.onCancelled();
            }
            return;
        }

        currentDirectory = directory;

        // Get files and directories
        File[] files = directory.listFiles();
        if (files == null) {
            Log.e(TAG, "Cannot read directory: " + directory);
            if (callback != null) {
                callback.onCancelled();
            }
            return;
        }

        // Separate directories and audio files
        List<File> dirs = new ArrayList<File>();
        List<File> audioFiles = new ArrayList<File>();

        for (File file : files) {
            if (file.isDirectory()) {
                dirs.add(file);
            } else if (FileScanner.isAudioFile(file)) {
                audioFiles.add(file);
            }
        }

        // Sort both lists
        Comparator<File> nameComparator = new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        };
        if (!dirs.isEmpty()) {
            File[] dirArray = dirs.toArray(new File[dirs.size()]);
            Arrays.sort(dirArray, nameComparator);
            dirs = Arrays.asList(dirArray);
        }
        if (!audioFiles.isEmpty()) {
            File[] fileArray = audioFiles.toArray(new File[audioFiles.size()]);
            Arrays.sort(fileArray, nameComparator);
            audioFiles = Arrays.asList(fileArray);
        }

        // Build combined list
        final List<File> allFiles = new ArrayList<File>();

        // Add parent directory option
        if (directory.getParentFile() != null) {
            allFiles.add(null); // Placeholder for ".."
        }

        // Add subdirectories
        allFiles.addAll(dirs);

        // Add audio files
        allFiles.addAll(audioFiles);

        // Add "Add All" option if there are audio files
        final boolean hasAudioFiles = !audioFiles.isEmpty();
        final List<File> finalAudioFiles = audioFiles;

        // Build display names
        String[] displayNames = new String[allFiles.size() + (hasAudioFiles ? 1 : 0)];
        int index = 0;

        for (File file : allFiles) {
            if (file == null) {
                displayNames[index++] = ".. (Parent)";
            } else if (file.isDirectory()) {
                displayNames[index++] = "[" + file.getName() + "]";
            } else {
                displayNames[index++] = file.getName();
            }
        }

        // Add "Add All" option
        if (hasAudioFiles) {
            displayNames[index] = "*** Add All " + finalAudioFiles.size() + " Tracks ***";
        }

        // Show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Browse: " + directory.getName());
        builder.setItems(displayNames, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Check if "Add All" was selected
                if (which == allFiles.size() && hasAudioFiles) {
                    addAllTracks(finalAudioFiles, callback);
                    return;
                }

                File selected = allFiles.get(which);

                if (selected == null) {
                    // Navigate to parent
                    showDirectoryDialog(directory.getParentFile(), callback);
                } else if (selected.isDirectory()) {
                    // Navigate into directory
                    showDirectoryDialog(selected, callback);
                } else {
                    // Single audio file selected
                    Track track = new Track(selected.getAbsolutePath());
                    fileScanner.extractMetadata(track);

                    List<Track> tracks = new ArrayList<Track>();
                    tracks.add(track);

                    if (callback != null) {
                        callback.onTracksSelected(tracks);
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    callback.onCancelled();
                }
            }
        });

        builder.show();
    }

    /**
     * Add all audio files in list
     */
    private void addAllTracks(List<File> audioFiles, TrackBrowserCallback callback) {
        List<Track> tracks = new ArrayList<Track>();

        for (File file : audioFiles) {
            Track track = new Track(file.getAbsolutePath());
            fileScanner.extractMetadata(track);
            tracks.add(track);
        }

        if (callback != null) {
            callback.onTracksSelected(tracks);
        }
    }

    /**
     * Show quick browser with common music locations
     */
    public static void showQuickBrowser(final Context context, final TrackBrowserCallback callback) {
        final File[] commonDirs = {
            FileScanner.getDefaultMusicDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStorageDirectory()
        };

        List<String> dirNames = new ArrayList<String>();
        final List<File> validDirs = new ArrayList<File>();

        for (File dir : commonDirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                dirNames.add(dir.getAbsolutePath());
                validDirs.add(dir);
            }
        }

        if (validDirs.isEmpty()) {
            Log.e(TAG, "No accessible directories");

            // Show error with details
            String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String errorMsg = "Cannot access storage!\n\nLooking for:\n" + sdcardPath + "/Music\n\n" +
                             "To add music:\n1. Grant storage permissions\n2. Put MP3 files in Music folder\n3. Try again";

            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(context);
            errorBuilder.setTitle("Storage Access Error");
            errorBuilder.setMessage(errorMsg);
            errorBuilder.setPositiveButton("OK", null);
            errorBuilder.show();

            if (callback != null) {
                callback.onCancelled();
            }
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Music Location");
        builder.setItems(dirNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                TrackBrowser browser = new TrackBrowser(context);
                browser.show(validDirs.get(which), callback);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    callback.onCancelled();
                }
            }
        });

        builder.show();
    }
}
