package com.rockbox.winamp.skin;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Renders Winamp-style UI using Canvas.
 * Phase 2: Renders loaded .wsz skin bitmaps when available.
 * Falls back to primitive drawing when no skin is loaded.
 */
public class SkinRenderer {

    // Winamp classic main window dimensions
    private static final int WINAMP_WIDTH = 275;
    private static final int WINAMP_HEIGHT = 116;

    // Skin assets (loaded bitmaps)
    private SkinAssets skinAssets;
    private SkinParser skinParser;

    // Colors (Winamp classic default skin palette)
    private static final int COLOR_BG = Color.rgb(0, 0, 0);
    private static final int COLOR_TITLE_BG = Color.rgb(36, 52, 92);
    private static final int COLOR_DISPLAY_BG = Color.rgb(0, 0, 0);
    private static final int COLOR_DISPLAY_TEXT = Color.rgb(0, 255, 0); // Classic green LED
    private static final int COLOR_BUTTON = Color.rgb(85, 85, 85);
    private static final int COLOR_BUTTON_PRESSED = Color.rgb(170, 170, 170);
    private static final int COLOR_BORDER = Color.rgb(128, 128, 128);

    private Paint bgPaint;
    private Paint titlePaint;
    private Paint displayPaint;
    private Paint textPaint;
    private Paint buttonPaint;
    private Paint borderPaint;
    private Paint focusPaint;

    private int scaleX;
    private int scaleY;
    private int offsetX;
    private int offsetY;

    // UI state
    private String trackTitle = "No track loaded";
    private int currentTime = 0;
    private int totalTime = 0;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private int volume = 50;
    private boolean shuffle = false;
    private boolean repeat = false;
    private int focusedButton = -1; // -1 = none, 0-4 = button index

    public SkinRenderer() {
        this.skinAssets = null;
        this.skinParser = new SkinParser();
        initPaints();
        // Initialize with default size (will be updated by setDisplaySize)
        setDisplaySize(720, 720); // Default for BB Classic
    }

    /**
     * Set skin assets to use for rendering
     */
    public void setSkinAssets(SkinAssets assets) {
        this.skinAssets = assets;
    }

    /**
     * Get current skin assets
     */
    public SkinAssets getSkinAssets() {
        return skinAssets;
    }

    /**
     * Check if a skin is loaded
     */
    public boolean hasSkin() {
        return skinAssets != null && skinAssets.isLoaded();
    }

    private void initPaints() {
        bgPaint = new Paint();
        bgPaint.setColor(COLOR_BG);
        bgPaint.setStyle(Paint.Style.FILL);

        titlePaint = new Paint();
        titlePaint.setColor(COLOR_TITLE_BG);
        titlePaint.setStyle(Paint.Style.FILL);

        displayPaint = new Paint();
        displayPaint.setColor(COLOR_DISPLAY_BG);
        displayPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(COLOR_DISPLAY_TEXT);
        textPaint.setTextSize(12);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);

        buttonPaint = new Paint();
        buttonPaint.setColor(COLOR_BUTTON);
        buttonPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(COLOR_BORDER);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);

        focusPaint = new Paint();
        focusPaint.setColor(Color.rgb(255, 165, 0)); // Orange focus
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(3);
        focusPaint.setAntiAlias(true);
    }

    /**
     * Calculate scaling and offset to center the Winamp UI on the screen
     */
    public void setDisplaySize(int width, int height) {
        // Scale to fit screen while maintaining aspect ratio
        float scaleXf = (float) width / WINAMP_WIDTH;
        float scaleYf = (float) height / WINAMP_HEIGHT;
        float scale = Math.min(scaleXf, scaleYf) * 0.8f; // 80% of screen

        scaleX = (int) (WINAMP_WIDTH * scale);
        scaleY = (int) (WINAMP_HEIGHT * scale);

        // Center on screen
        offsetX = (width - scaleX) / 2;
        offsetY = (height - scaleY) / 2;
    }

    /**
     * Main rendering method - draws entire Winamp UI
     */
    public void draw(Canvas canvas) {
        if (canvas == null) return;

        // Clear background
        canvas.drawColor(Color.BLACK);

        // Use loaded skin bitmaps if available, otherwise use primitives
        if (hasSkin() && skinAssets.hasBitmap(SkinAssets.MAIN)) {
            drawWithSkinBitmaps(canvas);
        } else {
            drawWithPrimitives(canvas);
        }
    }

    /**
     * Draw UI using loaded skin bitmaps
     */
    private void drawWithSkinBitmaps(Canvas canvas) {
        // Draw main window bitmap
        Bitmap mainBitmap = skinAssets.getMainWindow();
        if (mainBitmap != null) {
            Rect src = new Rect(0, 0, mainBitmap.getWidth(), mainBitmap.getHeight());
            Rect dst = new Rect(offsetX, offsetY, offsetX + scaleX, offsetY + scaleY);
            canvas.drawBitmap(mainBitmap, src, dst, null);
        }

        // Overlay dynamic elements (time display, visualizer)
        drawDynamicElements(canvas);

        // Draw usage hint
        drawHint(canvas);
    }

    /**
     * Draw UI using primitives (fallback when no skin loaded)
     */
    private void drawWithPrimitives(Canvas canvas) {
        // Draw main window background
        Rect mainWindow = new Rect(offsetX, offsetY, offsetX + scaleX, offsetY + scaleY);
        canvas.drawRect(mainWindow, bgPaint);
        canvas.drawRect(mainWindow, borderPaint);

        // Draw title bar
        drawTitleBar(canvas);

        // Draw display area (track info + time)
        drawDisplayArea(canvas);

        // Draw control buttons
        drawControlButtons(canvas);

        // Draw volume/balance sliders
        drawSliders(canvas);

        // Draw shuffle/repeat indicators
        drawModeIndicators(canvas);

        // Draw usage hint
        drawHint(canvas);
    }

    /**
     * Draw dynamic elements that overlay the skin
     * (time display, track title, visualizer)
     */
    private void drawDynamicElements(Canvas canvas) {
        // Get time display area from parser
        Rect timeRect = skinParser.getTimeDisplayRect();
        if (timeRect != null) {
            drawTimeWithDigits(canvas, timeRect);
        }

        // Get info display area
        Rect infoRect = skinParser.getInfoDisplayRect();
        if (infoRect != null) {
            drawTrackTitle(canvas, infoRect);
        }

        // Draw status indicator
        drawPlaybackStatus(canvas);
    }

    /**
     * Draw time using digit bitmaps from numbers.bmp
     */
    private void drawTimeWithDigits(Canvas canvas, Rect timeRect) {
        if (!skinAssets.hasBitmap(SkinAssets.NUMBERS)) {
            // Fallback to text
            textPaint.setColor(COLOR_DISPLAY_TEXT);
            textPaint.setTextSize(timeRect.height() * 0.6f);
            String timeStr = formatTime(currentTime) + " / " + formatTime(totalTime);
            canvas.drawText(timeStr, offsetX + timeRect.left, offsetY + timeRect.bottom, textPaint);
            return;
        }

        // Draw time with digit bitmaps
        String timeStr = formatTime(currentTime);
        int[] digitDims = skinParser.getNumberDimensions();
        int digitWidth = digitDims[0];
        int digitHeight = digitDims[1];

        int x = offsetX + timeRect.left;
        int y = offsetY + timeRect.top;

        for (int i = 0; i < timeStr.length(); i++) {
            char c = timeStr.charAt(i);
            Bitmap digit = null;

            if (c >= '0' && c <= '9') {
                digit = skinAssets.getDigit(c - '0');
            } else if (c == ':') {
                // Colon is often digit 10 or separate bitmap
                digit = skinAssets.getDigit(10);
            }

            if (digit != null) {
                canvas.drawBitmap(digit, x, y, null);
                x += digitWidth;
            }
        }
    }

    /**
     * Draw track title text
     */
    private void drawTrackTitle(Canvas canvas, Rect infoRect) {
        textPaint.setColor(COLOR_DISPLAY_TEXT);
        textPaint.setTextSize(infoRect.height() * 0.6f);
        String title = truncateText(trackTitle, infoRect.width());
        canvas.drawText(title, offsetX + infoRect.left, offsetY + infoRect.bottom - 2, textPaint);
    }

    /**
     * Draw playback status indicator
     */
    private void drawPlaybackStatus(Canvas canvas) {
        // Could use playpaus.bmp from skin
        // For now, just draw status text
        String status = isPlaying ? (isPaused ? "|| " : "▶ ") : "□ ";
        textPaint.setColor(COLOR_DISPLAY_TEXT);
        textPaint.setTextSize(16);
        canvas.drawText(status, offsetX + 10, offsetY + scaleY - 40, textPaint);
    }

    private void drawTitleBar(Canvas canvas) {
        int barHeight = scaleY / 8;
        Rect titleBar = new Rect(
            offsetX,
            offsetY,
            offsetX + scaleX,
            offsetY + barHeight
        );
        canvas.drawRect(titleBar, titlePaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(barHeight * 0.5f);
        canvas.drawText("Rockbox Winamp",
                       offsetX + 10,
                       offsetY + barHeight * 0.65f,
                       textPaint);
    }

    private void drawDisplayArea(Canvas canvas) {
        int barHeight = scaleY / 8;
        int displayTop = offsetY + barHeight + 5;
        int displayHeight = scaleY / 4;

        // Display background
        Rect displayBg = new Rect(
            offsetX + 10,
            displayTop,
            offsetX + scaleX - 10,
            displayTop + displayHeight
        );
        canvas.drawRect(displayBg, displayPaint);
        canvas.drawRect(displayBg, borderPaint);

        // Track title (scrolling marquee in future)
        textPaint.setColor(COLOR_DISPLAY_TEXT);
        textPaint.setTextSize(displayHeight * 0.3f);
        String title = truncateText(trackTitle, scaleX - 40);
        canvas.drawText(title,
                       offsetX + 15,
                       displayTop + displayHeight * 0.35f,
                       textPaint);

        // Time display
        String timeStr = formatTime(currentTime) + " / " + formatTime(totalTime);
        canvas.drawText(timeStr,
                       offsetX + 15,
                       displayTop + displayHeight * 0.70f,
                       textPaint);

        // Status text
        String status = isPlaying ? (isPaused ? "Paused" : "Playing") : "Stopped";
        canvas.drawText(status,
                       offsetX + scaleX - 80,
                       displayTop + displayHeight * 0.70f,
                       textPaint);
    }

    private void drawControlButtons(Canvas canvas) {
        int buttonY = offsetY + scaleY / 2;
        int buttonWidth = scaleX / 8;
        int buttonHeight = scaleY / 6;
        int spacing = 5;

        String[] labels = {"PREV", "PLAY", "PAUSE", "STOP", "NEXT"};
        int startX = offsetX + (scaleX - (buttonWidth * 5 + spacing * 4)) / 2;

        for (int i = 0; i < labels.length; i++) {
            int x = startX + i * (buttonWidth + spacing);

            // Button background
            Rect button = new Rect(x, buttonY, x + buttonWidth, buttonY + buttonHeight);
            canvas.drawRect(button, buttonPaint);
            canvas.drawRect(button, borderPaint);

            // Draw focus indicator if focused
            if (i == focusedButton) {
                canvas.drawRect(button, focusPaint);
            }

            // Button label
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(buttonHeight * 0.3f);
            float textWidth = textPaint.measureText(labels[i]);
            canvas.drawText(labels[i],
                           x + (buttonWidth - textWidth) / 2,
                           buttonY + buttonHeight * 0.6f,
                           textPaint);
        }
    }

    private void drawSliders(Canvas canvas) {
        int sliderY = offsetY + (int)(scaleY * 0.75f);
        int sliderWidth = scaleX / 3;
        int sliderHeight = 15;
        int labelOffset = 20;

        // Volume slider
        drawSlider(canvas, offsetX + 20, sliderY, sliderWidth, sliderHeight, volume, "VOL");

        // Balance slider (placeholder)
        drawSlider(canvas, offsetX + scaleX - sliderWidth - 20, sliderY, sliderWidth, sliderHeight, 50, "BAL");
    }

    private void drawSlider(Canvas canvas, int x, int y, int width, int height, int value, String label) {
        // Slider background
        Rect sliderBg = new Rect(x, y, x + width, y + height);
        canvas.drawRect(sliderBg, displayPaint);
        canvas.drawRect(sliderBg, borderPaint);

        // Slider fill
        int fillWidth = (width * value) / 100;
        Rect sliderFill = new Rect(x, y, x + fillWidth, y + height);
        buttonPaint.setColor(COLOR_DISPLAY_TEXT);
        canvas.drawRect(sliderFill, buttonPaint);
        buttonPaint.setColor(COLOR_BUTTON);

        // Label
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12);
        canvas.drawText(label, x, y - 5, textPaint);
    }

    private void drawModeIndicators(Canvas canvas) {
        int indicatorY = offsetY + scaleY - 25;
        int indicatorSize = 15;

        textPaint.setColor(shuffle ? COLOR_DISPLAY_TEXT : Color.GRAY);
        canvas.drawText("SHUFFLE", offsetX + 20, indicatorY, textPaint);

        textPaint.setColor(repeat ? COLOR_DISPLAY_TEXT : Color.GRAY);
        canvas.drawText("REPEAT", offsetX + 100, indicatorY, textPaint);
    }

    private void drawHint(Canvas canvas) {
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(14);

        // Bottom hint
        String hint = "Space: Play/Pause | N: Next | B: Prev | +/-: Volume";
        canvas.drawText(hint, offsetX + 10, offsetY + scaleY + 30, textPaint);

        // Top instruction (more visible)
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20);
        String topHint = "Press A to add music | L to load skin | H for help";
        canvas.drawText(topHint, 20, 40, textPaint);
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private String truncateText(String text, int maxWidth) {
        float width = textPaint.measureText(text);
        if (width <= maxWidth) {
            return text;
        }

        // Truncate with ellipsis
        int chars = (int) ((maxWidth / width) * text.length());
        return text.substring(0, Math.max(1, chars - 3)) + "...";
    }

    // Setters for UI state (called from WinampView)
    public void setTrackTitle(String title) {
        this.trackTitle = title;
    }

    public void setCurrentTime(int seconds) {
        this.currentTime = seconds;
    }

    public void setTotalTime(int seconds) {
        this.totalTime = seconds;
    }

    public void setPlaybackState(boolean playing, boolean paused) {
        this.isPlaying = playing;
        this.isPaused = paused;
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public void setFocusedButton(int buttonIndex) {
        this.focusedButton = buttonIndex;
    }

    public int getFocusedButton() {
        return focusedButton;
    }

    /**
     * Hit test for button clicks (touch/trackpad)
     * Returns button ID: 0=PREV, 1=PLAY, 2=PAUSE, 3=STOP, 4=NEXT, -1=none
     */
    public int hitTest(int x, int y) {
        int buttonY = offsetY + scaleY / 2;
        int buttonWidth = scaleX / 8;
        int buttonHeight = scaleY / 6;
        int spacing = 5;
        int startX = offsetX + (scaleX - (buttonWidth * 5 + spacing * 4)) / 2;

        for (int i = 0; i < 5; i++) {
            int btnX = startX + i * (buttonWidth + spacing);
            if (x >= btnX && x <= btnX + buttonWidth &&
                y >= buttonY && y <= buttonY + buttonHeight) {
                return i;
            }
        }
        return -1;
    }
}
