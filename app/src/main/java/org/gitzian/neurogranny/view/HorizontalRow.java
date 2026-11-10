package org.gitzian.neurogranny.view;

import android.graphics.RectF;

/** Splits an existing button/row rectangle into non-overlapping horizontal slots. */
public final class HorizontalRow {
    private final RectF bounds;
    private float nextLeft;

    public HorizontalRow(RectF bounds, float leadingPadding) {
        this.bounds = new RectF(bounds);
        nextLeft = bounds.left + Math.max(0.0f, leadingPadding);
    }

    public RectF fixed(float width, float trailingGap) {
        float end = Math.min(bounds.right, nextLeft + Math.max(0.0f, width));
        RectF slot = new RectF(nextLeft, bounds.top, end, bounds.bottom);
        nextLeft = Math.min(bounds.right, end + Math.max(0.0f, trailingGap));
        return slot;
    }

    public RectF remainder(float trailingPadding) {
        return new RectF(nextLeft, bounds.top,
                Math.max(nextLeft, bounds.right - Math.max(0.0f, trailingPadding)), bounds.bottom);
    }
}
