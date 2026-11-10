package org.gitzian.neurogranny.model;

/** Semantic secret mode; code matches the existing SQLite representation. */
public enum SecretMode {
    RECOVERABLE(0),
    VERIFY_ONLY(1);

    private final int databaseValue;

    SecretMode(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static SecretMode fromDatabaseValue(int value) {
        for (SecretMode mode : values()) {
            if (mode.databaseValue == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown secret mode: " + value);
    }
}
