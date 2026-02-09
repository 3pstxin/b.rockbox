# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Rockbox Winamp** is an Android music player that recreates the classic Winamp 2.x experience for BlackBerry Classic (SQC100-1). It features:
- Classic Winamp skin support (.wsz files)
- Keyboard-first navigation optimized for physical keyboards
- Android 4.x compatibility (BB10 Android Runtime)
- Lightweight, offline-first design
- Custom Canvas-based UI rendering

**Target Device**: BlackBerry Classic SQC100-1 (720x720 display, physical keyboard, BB10 Android Runtime)

## Build Commands

### Prerequisites
- JDK 7 or 8
- Android SDK with Build Tools 29.0.3
- Gradle 7.0.2 (auto-downloaded by wrapper)

### Build the APK
```bash
# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease

# Clean build
./gradlew clean
```

Output APK location: `app/build/outputs/apk/`

### Install to Device
```bash
# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# For BB Classic: Can also sideload via file transfer
```

### Run Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Architecture

### Package Structure
- `com.rockbox.winamp.audio` - Audio playback engine (MediaPlayer, playlists, equalizer)
- `com.rockbox.winamp.skin` - Winamp skin loading and parsing (.wsz support)
- `com.rockbox.winamp.ui` - Custom UI rendering (WinampView, KeyboardHandler)
- `com.rockbox.winamp.util` - Utilities (file scanning, preferences)

### Key Components

**WinampView** (`ui/WinampView.java`)
- Custom SurfaceView that renders the entire UI using Canvas
- Handles keyboard input and trackpad navigation
- Manages rendering thread (target 30 FPS)
- Delegates rendering to SkinRenderer

**SkinLoader** (`skin/SkinLoader.java`)
- Extracts .wsz files (ZIP archives)
- Validates and caches skin assets
- Provides fallback to default embedded skin

**SkinRenderer** (`skin/SkinRenderer.java`)
- Draws Winamp UI using skin bitmaps
- Handles button states, time display, visualizer
- Scales/centers UI for 720x720 display

**AudioEngine** (`audio/AudioEngine.java`)
- Wraps Android MediaPlayer API
- Manages audio focus and playback state
- Implements Winamp-like playback logic

**PlaybackController** (`audio/PlaybackController.java`)
- Coordinates playback actions (play/pause/stop/next/previous)
- Manages shuffle/repeat modes
- Controls volume

**Playlist** (`audio/Playlist.java`)
- Manages song queue
- Supports .m3u/.pls playlist formats
- Handles add/remove/reorder operations

### Rendering Pipeline
```
User Input (Keyboard/Trackpad)
    ↓
WinampView (KeyEvent/MotionEvent)
    ↓
PlaybackController
    ↓
AudioEngine (MediaPlayer)
    ↓
Audio Output

Parallel:
RenderThread (30 FPS)
    ↓
SkinRenderer.draw(Canvas)
    ↓
Display (720x720)
```

## Development Workflow

### Skin Format (.wsz)
Classic Winamp skins are ZIP archives containing:
- `main.bmp` - Main window (275x116px)
- `cbuttons.bmp` - Control buttons
- `titlebar.bmp` - Title bar
- `numbers.bmp` - Time display digits
- `pledit.txt` - Playlist editor config
- `region.txt` - Window shape definition

### Keyboard Shortcuts (Default Mapping)
- **Space** - Play/Pause
- **N** - Next track
- **B** - Previous track
- **S** - Stop
- **+/-** - Volume up/down
- **Arrow keys** - Navigate UI
- **Enter** - Activate focused element
- **L** - Load file
- **E** - Toggle equalizer

### Compatibility Constraints
- **Min SDK**: 16 (Android 4.1)
- **Target SDK**: 19 (Android 4.4)
- **No Google Play Services** - Pure Android SDK only
- **No AndroidX** - Support library compatible
- **Memory target**: < 30 MB resident
- **Performance**: 30 FPS minimum on BB Classic

### Testing on BlackBerry Classic
1. Build debug APK: `./gradlew assembleDebug`
2. Transfer APK to device or use ADB
3. Grant storage permissions when prompted
4. Place test MP3 files in `/sdcard/Music/`
5. Test keyboard shortcuts
6. Monitor memory usage: `adb shell dumpsys meminfo com.rockbox.winamp`

## Implementation Status

✅ **ALL 7 PHASES COMPLETE!** (100%)

**Phase 1: Project Setup & Basic UI** ✅ Complete
**Phase 2: Skin Loading System** ✅ Complete
**Phase 3: Audio Playback Engine** ✅ Complete
**Phase 4: Keyboard & Trackpad Input** ✅ Complete
**Phase 5: Playlist Editor** ✅ Complete
**Phase 6: Equalizer** ✅ Complete
**Phase 7: Polish & Optimization** ✅ Complete

**MVP Status**: 100% Complete
**Total Lines of Code**: 5,331 lines across 18 Java files
**Last Updated**: 2026-02-09

## Known Limitations

### Technical
- Gapless playback difficult on Android 4.x MediaPlayer
- Software rendering may not achieve 60 FPS
- AudioEffect API availability varies by device

### Legal
- No official Winamp branding/trademarks
- Users must provide their own .wsz skin files
- No bundled Winamp binaries

## Recent Fixes (2026-02-09 Build Session)

### Build Compatibility Fixes
All code now compiles successfully with Java 7 and Gradle 7.0.2:

1. **Method Visibility Fix** - `WinampView.java:386`
   - Renamed `draw()` to `drawFrame()` to avoid conflict with SurfaceView.draw()
   - Private helper method no longer mistaken for override

2. **Java 7 Final Variables** - Required for inner class access
   - `WinampView.java:96` - Made `wszFile` parameter final
   - `FilePicker.java:209` - Made `context` parameter final
   - `TrackBrowser.java:208` - Made `context` parameter final
   - `TrackBrowser.java:123` - Created `finalAudioFiles` for inner class usage

3. **UI Initialization Fix** - `SkinRenderer.java:58`
   - Initialize display size to 720x720 by default in constructor
   - Prevents rendering at 0x0 size before surfaceChanged() is called
   - Ensures UI is visible immediately on app start

4. **Error Handling Improvements**
   - Added detailed error dialogs when storage is inaccessible
   - Shows exact storage paths being checked
   - Provides troubleshooting guidance to users
   - Created comprehensive SETUP_GUIDE.md

5. **User Feedback**
   - Added startup toast: "Rockbox Winamp Started! Press A to add music, H for help"
   - Added prominent white instruction text at top of screen
   - Improved visibility of keyboard shortcuts

### Build Requirements
- **JDK**: 8 or 11 (NOT 21 - Gradle 7.0.2 doesn't support Java 21)
- **Android Studio**: Configure Gradle JDK to JDK 8 in Settings
- **Build Tools**: 29.0.3
- **Target**: API 16-19 (Android 4.x for BB10 compatibility)

### Known Issues & Solutions
- **Storage Access**: App requires READ_EXTERNAL_STORAGE permission
  - Grant via Settings > Apps > Rockbox Winamp > Permissions
  - Or via ADB: `adb shell pm grant com.rockbox.winamp android.permission.READ_EXTERNAL_STORAGE`
- **Music Not Loading**: Create `/sdcard/Music/` directory and add MP3 files
- **Skins Not Loading**: Create `/sdcard/Skins/` directory and add .wsz files

## Important Notes

- **Memory Management**: Recycle bitmaps promptly, use RGB_565 format
- **Performance**: Dirty rectangle tracking for rendering optimization
- **Input**: Keyboard-first design, touch is secondary
- **Skins**: Support core .wsz format, fail gracefully on errors
- **Java Version**: Must use JDK 8 for building (Java 7 target compatibility)
