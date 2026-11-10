package org.gitzian.neurogranny.model;

import java.util.Locale;

/** Converts between editable time units and seconds used by the scheduling model. */
public final class Interval {
    public static final long MAX_INTERVAL_SECONDS = 20L * 365 * 86_400;

    private Interval() {
    }

    public static long from(double amount, TimeUnit unit) {
        if (unit == null || Double.isNaN(amount) || Double.isInfinite(amount)
                || amount <= 0.0d) {
            throw new IllegalArgumentException("Positive interval required");
        }

        double seconds = amount * unit.seconds();
        if (seconds > MAX_INTERVAL_SECONDS) {
            throw new IllegalArgumentException("Interval too large");
        }
        return Math.max(1, Math.round(seconds));
    }

    /** Compatibility overload for existing integer unit positions. New code uses TimeUnit. */
    @Deprecated
    public static long from(double amount, int unit) {
        return from(amount, TimeUnit.atIndex(unit));
    }

    public static String amount(long seconds, TimeUnit unit) {
        return amount(seconds, unit, Locale.getDefault());
    }

    /** Formats the editor value with the supplied number-format locale. */
    public static String amount(long seconds, TimeUnit unit, Locale locale) {
        return String.format(locale, "%.4g", seconds / (double) unit.seconds());
    }

    @Deprecated
    public static String amount(long seconds, int unit) {
        return amount(seconds, TimeUnit.atIndex(unit));
    }
}
