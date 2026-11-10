package org.gitzian.neurogranny.model;

/** Time units used by user-entered intervals; stable order matches the spinner. */
public enum TimeUnit {
    SECONDS(1L),
    MINUTES(60L),
    HOURS(3_600L),
    DAYS(86_400L);

    private final long seconds;

    TimeUnit(long seconds) {
        this.seconds = seconds;
    }

    public long seconds() {
        return seconds;
    }

    public static TimeUnit atIndex(int index) {
        TimeUnit[] units = values();
        if (index < 0 || index >= units.length) {
            throw new IllegalArgumentException("Unsupported time unit index: " + index);
        }
        return units[index];
    }

    public static TimeUnit bestFor(long seconds) {
        for (int index = values().length - 1; index >= 1; index--) {
            TimeUnit unit = values()[index];
            if (seconds % unit.seconds == 0) {
                return unit;
            }
        }
        return SECONDS;
    }
}
