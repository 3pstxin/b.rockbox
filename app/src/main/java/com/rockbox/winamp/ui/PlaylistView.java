package com.rockbox.winamp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.rockbox.winamp.audio.Playlist;
import com.rockbox.winamp.audio.Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Visual playlist editor window.
 * Displays track list with scrolling, selection, and editing.
 */
public class PlaylistView extends View {

    private static final String TAG = "PlaylistView";

    // Winamp playlist colors
    private static final int COLOR_BG = Color.rgb(0, 0, 0);
    private static final int COLOR_TEXT = Color.rgb(0, 255, 0);
    private static final int COLOR_SELECTED = Color.rgb(0, 85, 0);
    private static final int COLOR_CURRENT = Color.rgb(0, 170, 0);
    private static final int COLOR_SCROLLBAR = Color.rgb(85, 85, 85);

    private Playlist playlist;
    private Paint bgPaint;
    private Paint textPaint;
    private Paint selectedPaint;
    private Paint currentPaint;
    private Paint scrollbarPaint;

    // Track display
    private int lineHeight = 20;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int visibleLines = 0;

    // Selection
    private Set<Integer> selectedIndices;
    private int lastSelectedIndex = -1;

    // Scrolling
    private int touchStartY = 0;
    private int scrollStartOffset = 0;

    public PlaylistView(Context context, Playlist playlist) {
        super(context);
        this.playlist = playlist;
        this.selectedIndices = new HashSet<Integer>();

        setFocusable(true);
        setFocusableInTouchMode(true);

        initPaints();

        // Listen for playlist changes
        playlist.addListener(new Playlist.PlaylistListener() {
            public void onPlaylistChanged() {
                updateScrollLimits();
                invalidate();
            }

            public void onCurrentTrackChanged(Track track) {
                scrollToCurrentTrack();
                invalidate();
            }
        });
    }

    private void initPaints() {
        bgPaint = new Paint();
        bgPaint.setColor(COLOR_BG);
        bgPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(14);
        textPaint.setAntiAlias(true);

        selectedPaint = new Paint();
        selectedPaint.setColor(COLOR_SELECTED);
        selectedPaint.setStyle(Paint.Style.FILL);

        currentPaint = new Paint();
        currentPaint.setColor(COLOR_CURRENT);
        currentPaint.setStyle(Paint.Style.FILL);

        scrollbarPaint = new Paint();
        scrollbarPaint.setColor(COLOR_SCROLLBAR);
        scrollbarPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateScrollLimits();
    }

    private void updateScrollLimits() {
        visibleLines = getHeight() / lineHeight;
        int totalLines = playlist.size();
        maxScroll = Math.max(0, (totalLines - visibleLines) * lineHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background
        canvas.drawColor(COLOR_BG);

        if (playlist.isEmpty()) {
            drawEmptyMessage(canvas);
            return;
        }

        // Draw tracks
        drawTracks(canvas);

        // Draw scrollbar
        if (maxScroll > 0) {
            drawScrollbar(canvas);
        }
    }

    private void drawEmptyMessage(Canvas canvas) {
        String message = "Playlist is empty. Press A to add tracks.";
        textPaint.setColor(Color.GRAY);
        float textWidth = textPaint.measureText(message);
        canvas.drawText(message,
            (getWidth() - textWidth) / 2,
            getHeight() / 2,
            textPaint);
        textPaint.setColor(COLOR_TEXT);
    }

    private void drawTracks(Canvas canvas) {
        int y = -scrollOffset;
        int currentIndex = playlist.getCurrentIndex();
        List<Track> tracks = playlist.getTracks();

        for (int i = 0; i < tracks.size(); i++) {
            // Skip if not visible
            if (y + lineHeight < 0) {
                y += lineHeight;
                continue;
            }
            if (y > getHeight()) {
                break;
            }

            Track track = tracks.get(i);

            // Draw selection/current highlight
            if (i == currentIndex) {
                canvas.drawRect(0, y, getWidth() - 15, y + lineHeight, currentPaint);
            } else if (selectedIndices.contains(i)) {
                canvas.drawRect(0, y, getWidth() - 15, y + lineHeight, selectedPaint);
            }

            // Draw track info
            String trackText = String.format("%d. %s [%s]",
                i + 1,
                track.getDisplayString(),
                track.getFormattedDuration());

            // Truncate if too long
            float maxWidth = getWidth() - 30;
            while (textPaint.measureText(trackText) > maxWidth && trackText.length() > 10) {
                trackText = trackText.substring(0, trackText.length() - 4) + "...";
            }

            canvas.drawText(trackText, 5, y + lineHeight - 5, textPaint);

            y += lineHeight;
        }
    }

    private void drawScrollbar(Canvas canvas) {
        int scrollbarWidth = 10;
        int scrollbarX = getWidth() - scrollbarWidth - 2;

        // Scrollbar track
        canvas.drawRect(scrollbarX, 0, scrollbarX + scrollbarWidth, getHeight(), bgPaint);

        // Scrollbar thumb
        int totalHeight = playlist.size() * lineHeight;
        int thumbHeight = Math.max(20, (getHeight() * getHeight()) / totalHeight);
        int thumbY = (scrollOffset * (getHeight() - thumbHeight)) / maxScroll;

        canvas.drawRect(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, scrollbarPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                requestFocus();
                touchStartY = (int) event.getY();
                scrollStartOffset = scrollOffset;

                // Check if clicked on track
                int clickedIndex = (scrollOffset + touchStartY) / lineHeight;
                if (clickedIndex >= 0 && clickedIndex < playlist.size()) {
                    handleTrackClick(clickedIndex, event);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                int dy = touchStartY - (int) event.getY();
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollStartOffset + dy));
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTrackClick(int index, MotionEvent event) {
        // Double-click to play
        if (event.getEventTime() - event.getDownTime() < 300) {
            playlist.setCurrentIndex(index);
            return;
        }

        // Single click to select
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index);
        } else {
            selectedIndices.clear();
            selectedIndices.add(index);
            lastSelectedIndex = index;
        }
        invalidate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                scrollUp();
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                scrollDown();
                return true;

            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
                removeSelected();
                return true;

            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                playSelected();
                return true;

            case KeyEvent.KEYCODE_A:
                if (event.isCtrlPressed()) {
                    selectAll();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Scroll up one line
     */
    public void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - lineHeight);
        invalidate();
    }

    /**
     * Scroll down one line
     */
    public void scrollDown() {
        scrollOffset = Math.min(maxScroll, scrollOffset + lineHeight);
        invalidate();
    }

    /**
     * Scroll to show current track
     */
    public void scrollToCurrentTrack() {
        int currentIndex = playlist.getCurrentIndex();
        int trackY = currentIndex * lineHeight;

        // Scroll if current track is not visible
        if (trackY < scrollOffset) {
            scrollOffset = trackY;
        } else if (trackY + lineHeight > scrollOffset + getHeight()) {
            scrollOffset = trackY + lineHeight - getHeight();
        }

        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    /**
     * Remove selected tracks
     */
    public void removeSelected() {
        if (selectedIndices.isEmpty()) return;

        // Sort in reverse order to maintain indices
        List<Integer> sorted = new ArrayList<Integer>(selectedIndices);
        java.util.Collections.sort(sorted);
        java.util.Collections.reverse(sorted);

        for (int index : sorted) {
            playlist.removeTrack(index);
        }

        selectedIndices.clear();
        updateScrollLimits();
        invalidate();

        Log.d(TAG, "Removed " + sorted.size() + " tracks");
    }

    /**
     * Play selected track
     */
    public void playSelected() {
        if (selectedIndices.isEmpty()) return;

        int index = selectedIndices.iterator().next();
        playlist.setCurrentIndex(index);
        Log.d(TAG, "Playing track: " + index);
    }

    /**
     * Select all tracks
     */
    public void selectAll() {
        selectedIndices.clear();
        for (int i = 0; i < playlist.size(); i++) {
            selectedIndices.add(i);
        }
        invalidate();
        Log.d(TAG, "Selected all " + playlist.size() + " tracks");
    }

    /**
     * Clear selection
     */
    public void clearSelection() {
        selectedIndices.clear();
        invalidate();
    }

    /**
     * Get selected track count
     */
    public int getSelectedCount() {
        return selectedIndices.size();
    }
}
