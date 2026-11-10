package org.gitzian.neurogranny.review;

import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.SecretMode;

/** Pure deterministic learning policy. No Android APIs and no clock reads. */
public final class ReviewEngine {
    public static final int GRADUATION_EVERY = 5;

    private static final double MULTIPLIER_EASY = 2.0;
    private static final double MULTIPLIER_HARD = 1.2;
    private static final double MULTIPLIER_MULTIPLE_FAILURES = 0.5;

    private ReviewEngine() {
    }

    public static boolean askConfidence(Memory memory, int wrongAttempts) {
        return memory.scheduleMode == ScheduleMode.AUTO_LEARNER && wrongAttempts < 2;
    }

    public static boolean offerGraduation(Memory memory, int wrongAttempts) {
        return memory.mode == SecretMode.RECOVERABLE
                && wrongAttempts < 2
                && memory.successes > 0
                && memory.successes % GRADUATION_EVERY == 0;
    }

    /**
     * Returns the next repetition interval in seconds.
     *
     * <p>Two or more failed attempts override the confidence answer in auto mode.
     * Fixed and ask-next-time modes retain their explicitly selected intervals.
     */
    public static long afterSuccess(
            Memory memory, int wrongAttempts, boolean easy, long nextIntervalSeconds) {
        switch (memory.scheduleMode) {
            case FIXED:
                return Math.max(1, memory.fixedSeconds);
            case ASK_NEXT_TIME:
                return Math.max(1, nextIntervalSeconds);
            case AUTO_LEARNER:
                double multiplier;
                if (wrongAttempts >= 2) {
                    multiplier = MULTIPLIER_MULTIPLE_FAILURES;
                } else {
                    multiplier = easy ? MULTIPLIER_EASY : MULTIPLIER_HARD;
                }

                long proposedSeconds = (long) Math.ceil(memory.currentSeconds * multiplier);
                return Math.max(
                        memory.minSeconds,
                        Math.min(memory.maxSeconds, proposedSeconds));
            default:
                throw new IllegalArgumentException("Unknown schedule mode");
        }
    }
}
