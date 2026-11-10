package org.gitzian.neurogranny.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.TextView;

/** Uses Android's theme font and system SP sizing in the Canvas UI. */
public final class UiTypography {
    public static final float BODY_SP = 14.0f;
    public static final float TITLE_SP = 17.0f;

    private final DisplayMetrics metrics;
    private final Typeface normal;
    private final Typeface bold;

    public UiTypography(Context context) {
        metrics = context.getResources().getDisplayMetrics();
        normal = defaultTypeface(context);
        bold = Typeface.create(normal, Typeface.BOLD);
    }

    public static Typeface defaultTypeface(Context context) {
        Typeface platform = new TextView(context).getTypeface();
        return platform == null ? Typeface.DEFAULT : platform;
    }

    public Typeface normal() {
        return normal;
    }

    public Typeface bold() {
        return bold;
    }

    /** Canvas coordinates are dp; convert system-scaled sp into those coordinates. */
    public float spToDp(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
                / metrics.density;
    }
}
