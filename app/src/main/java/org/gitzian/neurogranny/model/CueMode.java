package org.gitzian.neurogranny.model;

/** Selects how a memory cue is rendered. */
public enum CueMode {
    GENERATED(0),
    PHOTO(1);

    private final int databaseValue;

    CueMode(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static CueMode fromDatabaseValue(int value) {
        for (CueMode mode : values()) {
            if (mode.databaseValue == value) {
                return mode;
            }
        }
        return GENERATED;
    }
}
