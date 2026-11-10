package org.gitzian.neurogranny.view;

import android.graphics.RectF;

/** Positions variable-height Canvas content without hardcoded subsequent Y coordinates. */
public final class VerticalStack {
    private final float left;
    private final float width;
    private float top;

    public VerticalStack(float left, float top, float width) {
        this.left = left;
        this.top = top;
        this.width = Math.max(1.0f, width);
    }

    public RectF place(float height, float gapAfter) {
        RectF bounds = new RectF(left, top, left + width, top + Math.max(0.0f, height));
        top = bounds.bottom + Math.max(0.0f, gapAfter);
        return bounds;
    }

    public void gap(float distance) {
        top += Math.max(0.0f, distance);
    }

    public float bottom() {
        return top;
    }
}
