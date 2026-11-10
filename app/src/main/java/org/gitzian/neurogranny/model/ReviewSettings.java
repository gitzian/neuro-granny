package org.gitzian.neurogranny.model;

import java.util.Calendar;

/** Defaults shared between storage, scheduling and presentation. */
public final class ReviewSettings {
    public static final int HISTORY_WINDOW_DAYS = 14;
    public static final long MILLIS_PER_DAY = TimeUnit.DAYS.seconds() * 1_000L;
    public static final long DEFAULT_FIXED_SECONDS = TimeUnit.DAYS.seconds();
    public static final long DEFAULT_MIN_SECONDS = TimeUnit.HOURS.seconds();
    public static final long DEFAULT_MAX_SECONDS = 14L * TimeUnit.DAYS.seconds();

    /** Includes today and the preceding 13 local calendar days. */
    public static long historyWindowStart(long nowMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -(HISTORY_WINDOW_DAYS - 1));
        return calendar.getTimeInMillis();
    }

    private ReviewSettings() {
    }
}
