# Development Guide: Rockbox Winamp

## Quick Reference for Contributors

### Prerequisites

```bash
# Required
- JDK 7 or 8 (for Android 4.x compatibility)
- Android SDK (API 16-19)
- Android Build Tools 29.0.3

# Optional
- Android Studio (for debugging)
- ADB (for device testing)
- BlackBerry Classic (target hardware)
```

### First-Time Setup

```bash
# Clone repository
git clone https://github.com/3pstxin/b.rockbox.git
cd b.rockbox

# Verify Gradle wrapper
./gradlew --version

# Build project
./gradlew assembleDebug
```

### Common Tasks

#### Building

```bash
# Debug build (faster, includes debug info)
./gradlew assembleDebug

# Release build (optimized, minified)
./gradlew assembleRelease

# Clean build
./gradlew clean build

# List all tasks
./gradlew tasks
```

#### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Test specific class
./gradlew test --tests="com.rockbox.winamp.SkinRendererTest"
```

#### Installing

```bash
# Install debug APK to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install via Gradle
./gradlew installDebug

# Uninstall
adb uninstall com.rockbox.winamp
```

#### Debugging

```bash
# View logs
adb logcat | grep "RockboxWinamp"

# Filter by tag
adb logcat -s WinampView:D

# Check memory usage
adb shell dumpsys meminfo com.rockbox.winamp

# Monitor CPU usage
adb shell top | grep winamp
```

### Code Style

#### Java Conventions

```java
// Class names: PascalCase
public class AudioEngine { }

// Methods: camelCase
public void playTrack() { }

// Constants: UPPER_SNAKE_CASE
private static final int MAX_VOLUME = 100;

// Member variables: camelCase
private boolean isPlaying;

// Package-private: no modifier (avoid public when possible)
class SkinCache { }
```

#### File Organization

```
com.rockbox.winamp/
├── MainActivity.java           # Keep minimal, delegate to views
├── audio/                      # Playback logic
│   ├── AudioEngine.java        # MediaPlayer wrapper
│   ├── PlaybackController.java # State management
│   └── Playlist.java           # Queue management
├── skin/                       # Rendering system
│   ├── SkinLoader.java         # ZIP extraction
│   ├── SkinParser.java         # BMP parsing
│   ├── SkinRenderer.java       # Canvas drawing
│   └── SkinAssets.java         # Bitmap cache
├── ui/                         # View layer
│   ├── WinampView.java         # Main view
│   └── KeyboardHandler.java    # Input mapping
└── util/                       # Shared utilities
    ├── FileScanner.java        # Media discovery
    └── PreferencesManager.java # Settings
```

### Performance Guidelines

#### Memory Management

```java
// ✅ Good: Recycle bitmaps
Bitmap bitmap = BitmapFactory.decode(...);
// ... use bitmap ...
bitmap.recycle();

// ✅ Good: Use RGB_565 for performance
BitmapFactory.Options options = new BitmapFactory.Options();
options.inPreferredConfig = Bitmap.Config.RGB_565;

// ❌ Bad: Loading large bitmaps without options
Bitmap large = BitmapFactory.decodeFile(path); // May OOM
```

#### Canvas Rendering

```java
// ✅ Good: Dirty rectangle tracking
if (needsRedraw) {
    Canvas canvas = holder.lockCanvas(dirtyRect);
    // ... draw only changed area ...
}

// ✅ Good: Pre-allocate Paint objects
private Paint textPaint = new Paint(); // Reuse

// ❌ Bad: Creating Paint in draw loop
public void draw(Canvas canvas) {
    Paint p = new Paint(); // Don't do this!
}
```

#### Threading

```java
// ✅ Good: Rendering thread (already implemented)
private class RenderThread extends Thread {
    public void run() {
        while (running) {
            Canvas canvas = holder.lockCanvas();
            draw(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
    }
}

// ❌ Bad: Drawing on UI thread
// Never block UI thread with Canvas drawing
```

### Android 4.x Compatibility

#### API Level 16-19 Constraints

```java
// ✅ Available in API 16+
MediaPlayer player = new MediaPlayer();
Canvas canvas = holder.lockCanvas();
Paint paint = new Paint();

// ❌ NOT available (API 21+)
// VectorDrawable (use PNG instead)
// RecyclerView (use ListView)
// Toolbar (use ActionBar or custom)

// ⚠️ Check availability
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
    // Use AudioEffect API
}
```

#### No AndroidX

```gradle
// ✅ Use support library (if needed)
implementation 'com.android.support:support-v4:19.1.0'

// ❌ Don't use AndroidX
// implementation 'androidx.appcompat:appcompat:1.x.x'
```

### Winamp Skin Format

#### .wsz File Structure

```
skin.wsz (ZIP archive)
├── main.bmp        # 275x116px, main window
├── cbuttons.bmp    # 276x113px, control buttons
├── titlebar.bmp    # 275x14px, title bar
├── numbers.bmp     # Time display digits
├── playpaus.bmp    # Play/pause indicator
├── posbar.bmp      # Position slider
├── volume.bmp      # Volume slider
├── balance.bmp     # Balance slider
├── monoster.bmp    # Mono/stereo indicator
├── shufrep.bmp     # Shuffle/repeat buttons
├── text.bmp        # Playlist font
├── nums_ex.bmp     # Extended numbers
├── pledit.txt      # Playlist editor config
└── region.txt      # Window shape definition
```

#### Bitmap Format

```
Format: BMP (Windows Bitmap)
Color depth: 24-bit RGB or 8-bit indexed
Compression: None (BI_RGB)
Origin: Bottom-left (standard BMP)

# Important: Some skins use transparency via:
# - Magic color (RGB 255, 0, 255) for transparency
# - region.txt for window shape
```

#### Loading Example (Phase 2)

```java
// Extract .wsz
ZipFile zip = new ZipFile("/sdcard/Skins/myskin.wsz");
Enumeration<? extends ZipEntry> entries = zip.entries();

while (entries.hasMoreElements()) {
    ZipEntry entry = entries.nextElement();
    if (entry.getName().endsWith(".bmp")) {
        InputStream in = zip.getInputStream(entry);
        Bitmap bitmap = BitmapFactory.decodeStream(in);
        // Cache bitmap
    }
}
```

### BlackBerry Classic Specifics

#### Device Constraints

```
CPU: Qualcomm Snapdragon S4 Plus (dual-core 1.5 GHz)
RAM: 2 GB
Storage: 16 GB
Display: 720x720 (294 PPI)
OS: BB10 3.1 (Android Runtime 4.3)
```

#### Keyboard Layout

```
QWERTY physical keyboard
Optical trackpad (center of keyboard)
Function keys: Call, Back, Menu

Key mappings to implement:
- Call button → Play/Pause (optional)
- Back button → Exit confirmation
- Menu button → Settings menu
```

#### Testing Without Hardware

```bash
# Use Android emulator with square display
android create avd -n BBClassic -t android-19 \
    -s 720x720 -c 512M

# Or use Android Studio AVD Manager:
# - Device: Custom (720x720)
# - API: 19 (Android 4.4 KitKat)
# - Keyboard: Hardware keyboard present
```

### Debugging Tips

#### Common Issues

**Issue: App crashes on startup**
```bash
# Check logcat
adb logcat | grep "AndroidRuntime"

# Common causes:
# - Missing permissions in manifest
# - Null pointer in onCreate
# - Invalid resource reference
```

**Issue: UI not rendering**
```bash
# Verify SurfaceView callbacks
adb logcat -s WinampView:D

# Check:
# - surfaceCreated called?
# - RenderThread started?
# - Canvas locking successful?
```

**Issue: Keyboard input not working**
```bash
# Test key events
adb logcat | grep "KeyEvent"

# Verify:
# - View is focusable
# - View has focus
# - onKeyDown implemented
```

#### Performance Profiling

```bash
# Method tracing
adb shell am start -n com.rockbox.winamp/.MainActivity --start-profiler /sdcard/trace.trace

# Stop profiling
adb shell am profile stop com.rockbox.winamp

# Pull trace file
adb pull /sdcard/trace.trace
# Open in Android Studio: Analyze > Analyze Trace File
```

### Git Workflow

```bash
# Feature development
git checkout -b feature/audio-engine
# ... make changes ...
git add .
git commit -m "Implement AudioEngine with MediaPlayer"
git push origin feature/audio-engine

# Create PR on GitHub
# After merge, delete branch
git checkout main
git pull
git branch -d feature/audio-engine
```

### Code Review Checklist

- [ ] Follows Java naming conventions
- [ ] No memory leaks (bitmaps recycled, listeners removed)
- [ ] No blocking operations on UI thread
- [ ] Android 4.x compatible (no API 21+ features)
- [ ] Handles null inputs gracefully
- [ ] Logs errors appropriately
- [ ] Comments explain "why", not "what"
- [ ] No hardcoded strings (use strings.xml)
- [ ] Performance considered (no O(n²) in draw loop)

### Resources

#### Documentation
- [Implementation Plan](C:\Users\Admin\.claude\plans\rustling-twirling-abelson.md)
- [Project Status](PROJECT_STATUS.md)
- [CLAUDE.md](CLAUDE.md) - AI agent guidance
- [README.md](README.md) - User documentation

#### External Resources
- [Android 4.4 API Docs](https://developer.android.com/sdk/api_diff/19/changes)
- [Winamp Skin Specs](http://wiki.winamp.com/wiki/Skinning)
- [BB10 Android Runtime](https://developer.blackberry.com/android/)

### Contact

- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Email**: [To be added]

---

Happy coding! 🎵
