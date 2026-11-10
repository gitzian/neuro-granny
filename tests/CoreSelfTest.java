import org.gitzian.neurogranny.crypto.Argon2Verifier;
import org.gitzian.neurogranny.model.Interval;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.TimeUnit;
import org.gitzian.neurogranny.review.ReviewEngine;

/** Runs core policy and verifier checks with a JDK and Bouncy Castle. */
public final class CoreSelfTest {

    private static int passed;

    private CoreSelfTest() {
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        passed++;
    }

    public static void main(String[] args) {
        check(Interval.from(70, TimeUnit.MINUTES) == 4_200, "70 minutes");
        check(Interval.from(1, TimeUnit.HOURS) == 3_600, "1 hour");
        check(Interval.from(1, TimeUnit.DAYS) == 86_400, "1 day");

        Memory memory = new Memory();
        memory.scheduleMode = ScheduleMode.AUTO_LEARNER;
        memory.minSeconds = 3_600;
        memory.maxSeconds = 14 * 86_400;
        memory.currentSeconds = 3_600;

        check(ReviewEngine.afterSuccess(memory, 0, true, 0) == 7_200, "easy doubles");
        check(ReviewEngine.afterSuccess(memory, 2, true, 0) == 3_600,
                "two errors clamp to minimum");
        check(ReviewEngine.askConfidence(memory, 1), "one error permits confidence feedback");
        check(!ReviewEngine.askConfidence(memory, 2), "two errors skip confidence feedback");

        memory.successes = 5;
        check(ReviewEngine.offerGraduation(memory, 0), "fifth success offers graduation");
        check(!ReviewEngine.offerGraduation(memory, 2),
                "two errors suppress graduation");

        char[] password = "test-only example, not a real password".toCharArray();
        String firstVerifier = Argon2Verifier.hash(password);
        String secondVerifier = Argon2Verifier.hash(password);

        check(!firstVerifier.equals(secondVerifier), "each verifier has a random salt");
        check(Argon2Verifier.verify(password, firstVerifier), "correct verifier");
        check(!Argon2Verifier.verify("incorrect".toCharArray(), firstVerifier),
                "incorrect password rejected");
        check(!Argon2Verifier.verify(password, "bad-verifier"), "invalid verifier rejected");

        System.out.println("CoreSelfTest: " + passed + " checks passed");
    }
}
