package org.gitzian.neurogranny.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/** Immutable, measured multiline text for Canvas-backed UI. Requires API 23+. */
public final class TextBlock {
    private final StaticLayout layout;
    private final int width;

    public TextBlock(String text, int width, float textSize, int color,
            Typeface typeface, Layout.Alignment alignment) {
        this.width = Math.max(1, width);
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(color);
        paint.setTypeface(typeface);
        paint.setTextSize(textSize);
        String content = text == null ? "" : text;
        layout = StaticLayout.Builder.obtain(content, 0, content.length(), paint, this.width)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .build();
    }

    /**
     * Measures at the preferred size and reduces it only when the text is too tall.
     * If the minimum size still does not fit, the caller receives that measured
     * block and can decide whether to increase the available height.
     * All sizes use the same Canvas coordinate units as the caller.
     */
    public static TextBlock fit(String text, int width, float availableHeight,
            float preferredTextSize, float minimumTextSize, float sizeStep,
            int color, Typeface typeface, Layout.Alignment alignment) {
        if (minimumTextSize <= 0.0f || preferredTextSize < minimumTextSize || sizeStep <= 0.0f) {
            throw new IllegalArgumentException("Invalid text size range");
        }

        float textSize = preferredTextSize;
        while (true) {
            TextBlock candidate = new TextBlock(text, width, textSize, color, typeface, alignment);
            if (candidate.height() <= availableHeight || textSize <= minimumTextSize) {
                return candidate;
            }
            float nextSize = Math.max(minimumTextSize, textSize - sizeStep);
            if (nextSize >= textSize) {
                return candidate;
            }
            textSize = nextSize;
        }
    }

    public float height() {
        return layout.getHeight();
    }

    public void draw(Canvas canvas, float x, float top) {
        canvas.save();
        canvas.translate(x, top);
        canvas.clipRect(0, 0, width, layout.getHeight());
        layout.draw(canvas);
        canvas.restore();
    }
}
