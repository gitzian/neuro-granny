package org.gitzian.neurogranny.data;

/**
 * Persisted SQLite identifiers. These values are part of the database format.
 * Changing one requires a versioned migration in NeuroDatabase.
 */
public final class DatabaseContract {
    public static final String MEMORIES = "memories";
    public static final String REVIEW_EVENTS = "review_events";
    public static final String ID = "_id";
    public static final String TITLE = "title";
    public static final String SECRET_MODE = "mode";
    public static final String PLAINTEXT = "plain";
    public static final String VERIFIER = "verifier";
    public static final String SCHEDULE_MODE = "schedule_mode";
    public static final String FIXED_SECONDS = "fixed_seconds";
    public static final String MIN_SECONDS = "min_seconds";
    public static final String MAX_SECONDS = "max_seconds";
    public static final String CURRENT_SECONDS = "current_seconds";
    public static final String NEXT_DUE = "next_due";
    public static final String DUE = "is_due";
    public static final String PATTERN_SEED = "pattern_seed";
    public static final String PATTERN_VERSION = "pattern_version";
    public static final String CUE_MODE = "cue_mode";
    public static final String CUE_IMAGE = "cue_image";
    public static final String SUCCESSES = "successes";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String MEMORY_ID = "memory_id";
    public static final String TIMESTAMP = "timestamp";
    public static final String CORRECT = "correct";
    public static final String WRONG_ATTEMPTS = "wrong_attempts";
    public static final String CONFIDENCE = "confidence";
    public static final String INTERVAL_SECONDS = "interval_seconds";

    private DatabaseContract() {
    }
}
