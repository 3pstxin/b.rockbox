# 🎸 Rockbox Winamp - Setup Guide

## 📱 First Time Setup

### Step 1: Install the APK

```bash
# Via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or copy APK to device and install manually
```

### Step 2: Grant Storage Permissions

**On Android/BB10:**
1. Open **Settings**
2. Go to **Apps** or **Applications**
3. Find **Rockbox Winamp**
4. Tap **Permissions**
5. Enable **Storage** permission

**Or grant via ADB:**
```bash
adb shell pm grant com.rockbox.winamp android.permission.READ_EXTERNAL_STORAGE
```

### Step 3: Create Music Directory

```bash
# Create Music folder on device
adb shell mkdir -p /sdcard/Music

# Or manually on device:
# File Manager > Create New Folder > "Music"
```

### Step 4: Add Music Files

```bash
# Copy MP3 files to device
adb push your-music.mp3 /sdcard/Music/
adb push your-folder/*.mp3 /sdcard/Music/

# Or manually:
# Connect device via USB
# Copy MP3 files to: Internal Storage/Music/
```

### Step 5: (Optional) Add Winamp Skins

```bash
# Create Skins folder
adb shell mkdir -p /sdcard/Skins

# Copy .wsz skin files
adb push your-skin.wsz /sdcard/Skins/

# Or manually:
# Create "Skins" folder in root storage
# Copy .wsz files there
```

## ▶️ Using the App

### First Launch

1. **Launch app** - you'll see the Winamp interface
2. **Press A** - opens file browser to add music
3. **Navigate** to Music folder
4. **Select** MP3 files or "Add All"
5. **Press Space** - starts playback!

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| **A** | Add tracks to playlist |
| **Space** | Play / Pause |
| **N** | Next track |
| **B** | Previous track |
| **S** | Stop |
| **+/-** | Volume up/down |
| **L** | Load skin (.wsz) |
| **H** | Show all keyboard shortcuts |

## 🔧 Troubleshooting

### "Cannot access storage" Error

**Solution 1: Check Permissions**
- Settings > Apps > Rockbox Winamp > Permissions
- Enable "Storage" or "Files and Media"

**Solution 2: Grant via ADB**
```bash
adb shell pm grant com.rockbox.winamp android.permission.READ_EXTERNAL_STORAGE
```

**Solution 3: Check Storage Path**
```bash
# Verify storage is accessible
adb shell ls -la /sdcard/

# Should show Music, Download, etc.
```

### "Canceled" When Adding Music

This means no accessible directories found. Try:

1. **Create Music folder:**
   ```bash
   adb shell mkdir -p /sdcard/Music
   ```

2. **Grant permissions** (see above)

3. **Check storage path:**
   - App looks for: `/sdcard/Music`
   - Also checks: `/sdcard/Download`, `/sdcard/Downloads`

### No Skins Available

1. **Create Skins folder:**
   ```bash
   adb shell mkdir -p /sdcard/Skins
   ```

2. **Download Winamp classic skins** (.wsz files)
   - Search: "Winamp classic skins download"
   - Get .wsz format (not .wal)

3. **Copy to device:**
   ```bash
   adb push your-skin.wsz /sdcard/Skins/
   ```

### App Shows Black Screen

- Wait 2-3 seconds for initialization
- Check if toast message appears: "Rockbox Winamp Started!"
- Look for white text at top: "Press A to add music..."
- Try pressing **H** for help

### Buttons Don't Respond

- Make sure app has focus (tap screen once)
- Try using physical keyboard (not on-screen)
- BlackBerry Classic: use hardware keyboard

## 📂 Recommended Folder Structure

```
/sdcard/
├── Music/
│   ├── song1.mp3
│   ├── song2.mp3
│   └── album/
│       └── track.mp3
├── Skins/
│   ├── winamp-classic.wsz
│   └── custom-skin.wsz
└── Playlists/
    └── favorites.m3u
```

## 🎯 Quick Test

After setup, do this quick test:

```bash
# 1. Install app
adb install -r app-debug.apk

# 2. Create folders
adb shell mkdir -p /sdcard/Music
adb shell mkdir -p /sdcard/Skins

# 3. Copy a test MP3
adb push test.mp3 /sdcard/Music/

# 4. Grant permissions
adb shell pm grant com.rockbox.winamp android.permission.READ_EXTERNAL_STORAGE

# 5. Launch app
adb shell am start -n com.rockbox.winamp/.MainActivity

# 6. In app: Press A → Navigate to Music → Select test.mp3 → Press Space
```

You should hear music! 🎵

## 💡 Tips

- **Add multiple tracks:** Press A, navigate to folder, select "Add All"
- **Switch skins:** Press L, navigate to Skins folder, select .wsz file
- **Adjust volume:** Use +/- keys or volume rocker
- **Shuffle:** Press Z to toggle
- **Repeat:** Press R to cycle (Off → All → One)
- **Help:** Press H anytime to see all shortcuts

## 🐛 Still Having Issues?

Check logs:
```bash
adb logcat | grep -i "rockbox\|winamp"
```

Look for errors about:
- Permission denied
- File not found
- Cannot access storage

---

**Enjoy your classic Winamp experience!** 🎸
