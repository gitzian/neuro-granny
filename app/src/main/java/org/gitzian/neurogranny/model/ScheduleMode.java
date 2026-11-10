package org.gitzian.neurogranny.model;

/** Semantic review mode; code matches the existing SQLite representation. */
public enum ScheduleMode {
    FIXED(0),
    AUTO_LEARNER(1),
    ASK_NEXT_TIME(2);

    private final int databaseValue;

    ScheduleMode(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static ScheduleMode fromDatabaseValue(int value) {
        for (ScheduleMode mode : values()) {
            if (mode.databaseValue == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown scheduling mode: " + value);
    }
}
