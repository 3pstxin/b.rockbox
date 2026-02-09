# Add project specific ProGuard rules here.
# Keep our application class
-keep class com.rockbox.winamp.** { *; }

# Keep MediaPlayer and Audio APIs
-keep class android.media.** { *; }

# Keep skin-related classes to preserve reflection-based loading
-keepclassmembers class * {
    public <init>(...);
}
