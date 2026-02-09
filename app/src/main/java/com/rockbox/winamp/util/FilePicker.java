package com.rockbox.winamp.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple file picker for selecting .wsz skin files.
 * Displays a dialog with file list for user selection.
 */
public class FilePicker {

    private static final String TAG = "FilePicker";

    // File filter for .wsz files
    public interface FileFilter {
        boolean accept(File file);
    }

    // Callback for file selection
    public interface FilePickerCallback {
        void onFileSelected(File file);
        void onCancelled();
    }

    // Default filter for .wsz files
    public static final FileFilter WSZ_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory() || file.getName().toLowerCase().endsWith(".wsz");
        }
    };

    private Context context;
    private File currentDirectory;
    private FileFilter filter;
    private FilePickerCallback callback;

    public FilePicker(Context context) {
        this.context = context;
        this.filter = WSZ_FILTER;
        this.currentDirectory = getDefaultStartDirectory();
    }

    /**
     * Show file picker dialog
     */
    public void show(FilePickerCallback callback) {
        this.callback = callback;
        showDirectoryDialog(currentDirectory);
    }

    /**
     * Show file picker starting from specific directory
     */
    public void show(File startDirectory, FilePickerCallback callback) {
        this.callback = callback;
        this.currentDirectory = startDirectory;
        showDirectoryDialog(currentDirectory);
    }

    /**
     * Set custom file filter
     */
    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    /**
     * Show directory contents in dialog
     */
    private void showDirectoryDialog(final File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Log.e(TAG, "Invalid directory: " + directory);
            if (callback != null) {
                callback.onCancelled();
            }
            return;
        }

        currentDirectory = directory;

        // Get file list
        File[] files = directory.listFiles();
        if (files == null) {
            Log.e(TAG, "Cannot read directory: " + directory);
            if (callback != null) {
                callback.onCancelled();
            }
            return;
        }

        // Filter and sort files
        List<File> fileList = new ArrayList<File>();
        for (File file : files) {
            if (filter.accept(file)) {
                fileList.add(file);
            }
        }

        // Sort: directories first, then files, alphabetically
        Collections.sort(fileList, new Comparator<File>() {
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            }
        });

        // Add parent directory option if not at root
        boolean hasParent = directory.getParentFile() != null;
        if (hasParent) {
            fileList.add(0, null); // Placeholder for ".."
        }

        // Build display names
        final File[] finalFiles = fileList.toArray(new File[fileList.size()]);
        String[] displayNames = new String[finalFiles.length];

        for (int i = 0; i < finalFiles.length; i++) {
            if (finalFiles[i] == null) {
                displayNames[i] = "..";
            } else if (finalFiles[i].isDirectory()) {
                displayNames[i] = "[" + finalFiles[i].getName() + "]";
            } else {
                displayNames[i] = finalFiles[i].getName();
            }
        }

        // Show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Skin: " + directory.getName());
        builder.setItems(displayNames, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                File selected = finalFiles[which];

                if (selected == null) {
                    // Navigate to parent directory
                    showDirectoryDialog(directory.getParentFile());
                } else if (selected.isDirectory()) {
                    // Navigate into directory
                    showDirectoryDialog(selected);
                } else {
                    // File selected
                    if (callback != null) {
                        callback.onFileSelected(selected);
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
     * Get default start directory for skin files
     */
    private File getDefaultStartDirectory() {
        // Try /sdcard/Skins first
        File skinsDir = new File(Environment.getExternalStorageDirectory(), "Skins");
        if (skinsDir.exists() && skinsDir.isDirectory()) {
            return skinsDir;
        }

        // Try /sdcard/Download
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            return downloadDir;
        }

        // Fallback to external storage root
        return Environment.getExternalStorageDirectory();
    }

    /**
     * Create default skin directory if it doesn't exist
     */
    public static File createDefaultSkinsDirectory() {
        File skinsDir = new File(Environment.getExternalStorageDirectory(), "Skins");
        if (!skinsDir.exists()) {
            skinsDir.mkdirs();
        }
        return skinsDir;
    }

    /**
     * Quick file picker that shows common skin locations
     */
    public static void showQuickPicker(final Context context, final FilePickerCallback callback) {
        final File[] commonDirs = {
            new File(Environment.getExternalStorageDirectory(), "Skins"),
            new File(Environment.getExternalStorageDirectory(), "Download"),
            new File(Environment.getExternalStorageDirectory(), "Downloads"),
            Environment.getExternalStorageDirectory()
        };

        List<String> dirNames = new ArrayList<String>();
        final List<File> validDirs = new ArrayList<File>();

        for (File dir : commonDirs) {
            if (dir.exists() && dir.isDirectory()) {
                dirNames.add(dir.getAbsolutePath());
                validDirs.add(dir);
            }
        }

        if (validDirs.isEmpty()) {
            Log.e(TAG, "No accessible directories found");

            // Show error with details
            String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String errorMsg = "Cannot access storage!\n\nLooking for:\n" + sdcardPath + "\n\n" +
                             "Permissions granted? Check Settings > Apps > Rockbox Winamp > Permissions";

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
        builder.setTitle("Choose Location");
        builder.setItems(dirNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                FilePicker picker = new FilePicker(context);
                picker.show(validDirs.get(which), callback);
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
