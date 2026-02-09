package com.rockbox.winamp.ui;

import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages focus navigation for UI elements.
 * Supports trackpad/D-pad navigation between controls.
 */
public class FocusManager {

    private static final String TAG = "FocusManager";

    // Focusable element types
    public static final int ELEMENT_PREV_BUTTON = 0;
    public static final int ELEMENT_PLAY_BUTTON = 1;
    public static final int ELEMENT_PAUSE_BUTTON = 2;
    public static final int ELEMENT_STOP_BUTTON = 3;
    public static final int ELEMENT_NEXT_BUTTON = 4;
    public static final int ELEMENT_VOLUME_SLIDER = 5;
    public static final int ELEMENT_POSITION_SLIDER = 6;
    public static final int ELEMENT_SHUFFLE_BUTTON = 7;
    public static final int ELEMENT_REPEAT_BUTTON = 8;

    private List<FocusableElement> elements;
    private int currentFocusIndex;

    public static class FocusableElement {
        public int type;
        public Rect bounds;
        public String label;

        public FocusableElement(int type, Rect bounds, String label) {
            this.type = type;
            this.bounds = bounds;
            this.label = label;
        }
    }

    public FocusManager() {
        this.elements = new ArrayList<FocusableElement>();
        this.currentFocusIndex = -1;
    }

    /**
     * Add focusable element
     */
    public void addElement(int type, Rect bounds, String label) {
        elements.add(new FocusableElement(type, bounds, label));
        if (currentFocusIndex < 0 && !elements.isEmpty()) {
            currentFocusIndex = 0;
        }
    }

    /**
     * Clear all elements
     */
    public void clear() {
        elements.clear();
        currentFocusIndex = -1;
    }

    /**
     * Get currently focused element
     */
    public FocusableElement getCurrentElement() {
        if (currentFocusIndex >= 0 && currentFocusIndex < elements.size()) {
            return elements.get(currentFocusIndex);
        }
        return null;
    }

    /**
     * Get current focus index
     */
    public int getCurrentFocusIndex() {
        return currentFocusIndex;
    }

    /**
     * Move focus up
     */
    public boolean focusUp() {
        if (elements.isEmpty()) return false;

        FocusableElement current = getCurrentElement();
        if (current == null) {
            currentFocusIndex = 0;
            return true;
        }

        // Find element above current
        FocusableElement best = null;
        int bestIndex = -1;
        int currentCenterX = current.bounds.centerX();
        int currentTop = current.bounds.top;

        for (int i = 0; i < elements.size(); i++) {
            if (i == currentFocusIndex) continue;

            FocusableElement elem = elements.get(i);
            int elemCenterX = elem.bounds.centerX();
            int elemBottom = elem.bounds.bottom;

            // Must be above current
            if (elemBottom <= currentTop) {
                // Prefer element with closer X alignment
                int xDist = Math.abs(elemCenterX - currentCenterX);
                int yDist = currentTop - elemBottom;

                if (best == null || (yDist < 200 && xDist < Math.abs(best.bounds.centerX() - currentCenterX))) {
                    best = elem;
                    bestIndex = i;
                }
            }
        }

        if (best != null) {
            currentFocusIndex = bestIndex;
            Log.d(TAG, "Focus up to: " + best.label);
            return true;
        }

        return false;
    }

    /**
     * Move focus down
     */
    public boolean focusDown() {
        if (elements.isEmpty()) return false;

        FocusableElement current = getCurrentElement();
        if (current == null) {
            currentFocusIndex = 0;
            return true;
        }

        // Find element below current
        FocusableElement best = null;
        int bestIndex = -1;
        int currentCenterX = current.bounds.centerX();
        int currentBottom = current.bounds.bottom;

        for (int i = 0; i < elements.size(); i++) {
            if (i == currentFocusIndex) continue;

            FocusableElement elem = elements.get(i);
            int elemCenterX = elem.bounds.centerX();
            int elemTop = elem.bounds.top;

            // Must be below current
            if (elemTop >= currentBottom) {
                int xDist = Math.abs(elemCenterX - currentCenterX);
                int yDist = elemTop - currentBottom;

                if (best == null || (yDist < 200 && xDist < Math.abs(best.bounds.centerX() - currentCenterX))) {
                    best = elem;
                    bestIndex = i;
                }
            }
        }

        if (best != null) {
            currentFocusIndex = bestIndex;
            Log.d(TAG, "Focus down to: " + best.label);
            return true;
        }

        return false;
    }

    /**
     * Move focus left
     */
    public boolean focusLeft() {
        if (elements.isEmpty()) return false;

        FocusableElement current = getCurrentElement();
        if (current == null) {
            currentFocusIndex = 0;
            return true;
        }

        // Find element to the left
        FocusableElement best = null;
        int bestIndex = -1;
        int currentCenterY = current.bounds.centerY();
        int currentLeft = current.bounds.left;

        for (int i = 0; i < elements.size(); i++) {
            if (i == currentFocusIndex) continue;

            FocusableElement elem = elements.get(i);
            int elemCenterY = elem.bounds.centerY();
            int elemRight = elem.bounds.right;

            // Must be to the left
            if (elemRight <= currentLeft) {
                int xDist = currentLeft - elemRight;
                int yDist = Math.abs(elemCenterY - currentCenterY);

                if (best == null || (yDist < 100 && xDist < (currentLeft - best.bounds.right))) {
                    best = elem;
                    bestIndex = i;
                }
            }
        }

        if (best != null) {
            currentFocusIndex = bestIndex;
            Log.d(TAG, "Focus left to: " + best.label);
            return true;
        }

        return false;
    }

    /**
     * Move focus right
     */
    public boolean focusRight() {
        if (elements.isEmpty()) return false;

        FocusableElement current = getCurrentElement();
        if (current == null) {
            currentFocusIndex = 0;
            return true;
        }

        // Find element to the right
        FocusableElement best = null;
        int bestIndex = -1;
        int currentCenterY = current.bounds.centerY();
        int currentRight = current.bounds.right;

        for (int i = 0; i < elements.size(); i++) {
            if (i == currentFocusIndex) continue;

            FocusableElement elem = elements.get(i);
            int elemCenterY = elem.bounds.centerY();
            int elemLeft = elem.bounds.left;

            // Must be to the right
            if (elemLeft >= currentRight) {
                int xDist = elemLeft - currentRight;
                int yDist = Math.abs(elemCenterY - currentCenterY);

                if (best == null || (yDist < 100 && xDist < (best.bounds.left - currentRight))) {
                    best = elem;
                    bestIndex = i;
                }
            }
        }

        if (best != null) {
            currentFocusIndex = bestIndex;
            Log.d(TAG, "Focus right to: " + best.label);
            return true;
        }

        return false;
    }

    /**
     * Move to next element (tab order)
     */
    public boolean focusNext() {
        if (elements.isEmpty()) return false;

        currentFocusIndex = (currentFocusIndex + 1) % elements.size();
        FocusableElement elem = getCurrentElement();
        if (elem != null) {
            Log.d(TAG, "Focus next to: " + elem.label);
        }
        return true;
    }

    /**
     * Move to previous element
     */
    public boolean focusPrevious() {
        if (elements.isEmpty()) return false;

        currentFocusIndex = (currentFocusIndex - 1 + elements.size()) % elements.size();
        FocusableElement elem = getCurrentElement();
        if (elem != null) {
            Log.d(TAG, "Focus previous to: " + elem.label);
        }
        return true;
    }

    /**
     * Set focus by element type
     */
    public boolean setFocus(int elementType) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).type == elementType) {
                currentFocusIndex = i;
                Log.d(TAG, "Focus set to: " + elements.get(i).label);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if element is focused
     */
    public boolean isFocused(int elementType) {
        FocusableElement current = getCurrentElement();
        return current != null && current.type == elementType;
    }
}
