package org.gitzian.neurogranny;

import org.junit.Test;
import static org.junit.Assert.*;
import org.gitzian.neurogranny.crypto.Argon2Verifier;
import org.gitzian.neurogranny.model.Interval;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.SecretMode;
import org.gitzian.neurogranny.model.TimeUnit;
import org.gitzian.neurogranny.review.ReviewEngine;

public final class CorePolicyTest {
    @Test
    public void timeUnitsConvertExactly() {
        assertEquals(4200, Interval.from(70, TimeUnit.MINUTES));
        assertEquals(3600, Interval.from(1, TimeUnit.HOURS));
        assertEquals(86400, Interval.from(1, TimeUnit.DAYS));
    }
    @Test
    public void persistedEnumCodesRemainStable() {
        assertEquals(0, SecretMode.RECOVERABLE.databaseValue());
        assertEquals(1, SecretMode.VERIFY_ONLY.databaseValue());
        assertEquals(0, ScheduleMode.FIXED.databaseValue());
        assertEquals(1, ScheduleMode.AUTO_LEARNER.databaseValue());
        assertEquals(2, ScheduleMode.ASK_NEXT_TIME.databaseValue());
        assertEquals(86_400L, TimeUnit.DAYS.seconds());
        assertEquals(TimeUnit.HOURS, TimeUnit.bestFor(3_600L));
    }

    @Test
    public void autoLearnerClampsToBounds() {
        Memory m = new Memory();
        m.scheduleMode = ScheduleMode.AUTO_LEARNER;
        m.currentSeconds = 3600;
        m.minSeconds = 3600;
        m.maxSeconds = 14 * 86400;
        assertEquals(7200, ReviewEngine.afterSuccess(m, 0, true, 0));
        assertEquals(3600, ReviewEngine.afterSuccess(m, 2, true, 0));
        m.currentSeconds = 14 * 86400;
        assertEquals(14 * 86400, ReviewEngine.afterSuccess(m, 0, true, 0));
    }
    @Test
    public void twoWrongAttemptsSkipConfidence() {
        Memory m = new Memory();
        assertTrue(ReviewEngine.askConfidence(m, 0));
        assertTrue(ReviewEngine.askConfidence(m, 1));
        assertFalse(ReviewEngine.askConfidence(m, 2));
    }
    @Test
    public void everyFifthSuccessOffersOptionalGraduation() {
        Memory m = new Memory();
        m.mode = SecretMode.RECOVERABLE;
        m.successes = 5;
        assertTrue(ReviewEngine.offerGraduation(m, 0));
        assertFalse(ReviewEngine.offerGraduation(m, 2));
        m.successes = 6;
        assertFalse(ReviewEngine.offerGraduation(m, 0));
        m.successes = 10;
        assertTrue(ReviewEngine.offerGraduation(m, 0));
        m.mode = SecretMode.VERIFY_ONLY;
        assertFalse(ReviewEngine.offerGraduation(m, 0));
    }
    @Test
    public void saltedArgon2WorksAndSaltsDiffer() {
        char[] password = "never use real passwords in unit tests".toCharArray();
        String h1 = Argon2Verifier.hash(password), h2 = Argon2Verifier.hash(password);
        assertNotEquals(h1, h2);
        assertTrue(Argon2Verifier.verify(password, h1));
        assertFalse(Argon2Verifier.verify("wrong".toCharArray(), h1));
    }
}
