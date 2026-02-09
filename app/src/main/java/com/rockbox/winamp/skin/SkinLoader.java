package com.rockbox.winamp.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads Winamp classic skins from .wsz files (ZIP archives).
 * Extracts skin assets to cache directory and validates skin structure.
 */
public class SkinLoader {

    private static final String TAG = "SkinLoader";

    // Skin cache directory
    private static final String CACHE_DIR = "skins";

    private Context context;
    private SkinAssets skinAssets;
    private SkinParser skinParser;

    public SkinLoader(Context context) {
        this.context = context;
        this.skinAssets = new SkinAssets();
        this.skinParser = new SkinParser();
    }

    /**
     * Load a .wsz skin from file path
     *
     * @param wszPath Path to .wsz file
     * @return SkinAssets object with loaded bitmaps, or null on failure
     */
    public SkinAssets loadSkin(String wszPath) {
        if (wszPath == null || !wszPath.toLowerCase().endsWith(".wsz")) {
            Log.e(TAG, "Invalid skin path: " + wszPath);
            return null;
        }

        File wszFile = new File(wszPath);
        if (!wszFile.exists() || !wszFile.canRead()) {
            Log.e(TAG, "Cannot read skin file: " + wszPath);
            return null;
        }

        Log.i(TAG, "Loading skin: " + wszFile.getName());

        // Clear previous skin
        skinAssets.release();
        skinAssets = new SkinAssets();
        skinAssets.setSkinPath(wszPath);
        skinAssets.setSkinName(getSkinNameFromPath(wszPath));

        // Extract and load skin
        try {
            extractAndLoadSkin(wszFile);
            skinAssets.setLoaded(true);
            Log.i(TAG, "Skin loaded successfully: " + skinAssets.getBitmapCount() + " bitmaps");
            return skinAssets;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load skin: " + e.getMessage(), e);
            skinAssets.release();
            return null;
        }
    }

    /**
     * Extract .wsz (ZIP) and load bitmaps
     */
    private void extractAndLoadSkin(File wszFile) throws IOException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(wszFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            // Create temp directory for extraction
            File cacheDir = getCacheDirectory();
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().toLowerCase();

                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }

                // Extract bitmap files
                if (name.endsWith(".bmp")) {
                    loadBitmapFromZip(zipFile, entry);
                }
                // Extract text config files
                else if (name.endsWith(".txt")) {
                    extractTextFile(zipFile, entry, cacheDir);
                }
            }

            // Parse config files
            parseConfigFiles(cacheDir);

            // Validate skin
            if (!skinAssets.hasMinimumBitmaps()) {
                throw new IOException("Skin missing required bitmaps");
            }

        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing zip file", e);
                }
            }
        }
    }

    /**
     * Load a bitmap directly from ZIP entry
     */
    private void loadBitmapFromZip(ZipFile zipFile, ZipEntry entry) throws IOException {
        InputStream in = null;
        try {
            in = zipFile.getInputStream(entry);

            // Decode bitmap with options for memory efficiency
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Lower memory
            options.inPurgeable = true;
            options.inInputShareable = true;

            Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
            if (bitmap != null) {
                String name = new File(entry.getName()).getName().toLowerCase();
                skinAssets.putBitmap(name, bitmap);
                Log.d(TAG, "Loaded bitmap: " + name + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Extract text config file (region.txt, pledit.txt)
     */
    private void extractTextFile(ZipFile zipFile, ZipEntry entry, File cacheDir) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = zipFile.getInputStream(entry);
            File outFile = new File(cacheDir, new File(entry.getName()).getName());
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            Log.d(TAG, "Extracted config: " + outFile.getName());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Parse text config files (region.txt, pledit.txt)
     */
    private void parseConfigFiles(File cacheDir) {
        // Parse region.txt for window shape
        File regionFile = new File(cacheDir, "region.txt");
        if (regionFile.exists()) {
            skinParser.parseRegionFile(regionFile);
        }

        // Parse pledit.txt for playlist editor config
        File pleditFile = new File(cacheDir, "pledit.txt");
        if (pleditFile.exists()) {
            skinParser.parsePleditFile(pleditFile);
        }
    }

    /**
     * Get cache directory for skin extraction
     */
    private File getCacheDirectory() {
        return new File(context.getCacheDir(), CACHE_DIR);
    }

    /**
     * Extract skin name from file path
     */
    private String getSkinNameFromPath(String path) {
        String name = new File(path).getName();
        if (name.toLowerCase().endsWith(".wsz")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    /**
     * Load default embedded skin (fallback)
     */
    public SkinAssets loadDefaultSkin() {
        Log.i(TAG, "Loading default embedded skin");

        skinAssets.release();
        skinAssets = new SkinAssets();
        skinAssets.setSkinName("Default");
        skinAssets.setLoaded(false); // No bitmaps, will use SkinRenderer primitives

        return skinAssets;
    }

    /**
     * Get currently loaded skin assets
     */
    public SkinAssets getSkinAssets() {
        return skinAssets;
    }

    /**
     * List available .wsz files in a directory
     */
    public static File[] listSkins(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return new File[0];
        }

        return directory.listFiles(new java.io.FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().toLowerCase().endsWith(".wsz");
            }
        });
    }

    /**
     * Check if a file is a valid .wsz skin
     */
    public static boolean isValidSkin(String path) {
        if (path == null || !path.toLowerCase().endsWith(".wsz")) {
            return false;
        }

        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        // Quick validation: try to open as ZIP
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);

            // Check for at least one .bmp file
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().toLowerCase().endsWith(".bmp")) {
                    return true;
                }
            }
            return false;

        } catch (IOException e) {
            return false;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Clean up skin cache
     */
    public void clearCache() {
        File cacheDir = getCacheDirectory();
        if (cacheDir.exists()) {
            deleteRecursive(cacheDir);
        }
    }

    /**
     * Recursively delete directory
     */
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
