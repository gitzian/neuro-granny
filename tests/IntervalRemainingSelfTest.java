import org.gitzian.neurogranny.model.Interval;
import org.gitzian.neurogranny.model.TimeUnit;

import java.util.Locale;

/** Plain-Java smoke tests for the compact home-screen countdown formatter. */
public final class IntervalRemainingSelfTest {
    private IntervalRemainingSelfTest() {
    }

    public static void main(String[] args) {
        check(Interval.remaining(0), "Due now");
        check(Interval.remaining(18), "18 sec");
        check(Interval.remaining(3527), "59 min");
        check(Interval.remaining(3600), "1 h");
        check(Interval.remaining(4500), "1 h 15 min");
        check(Interval.remaining(86400), "1 day");
        check(Interval.remaining(90000), "1 day 1 h");
        check(Interval.remaining(1_209_600), "14 days");
        check(Interval.amount(5_400, TimeUnit.HOURS, Locale.GERMAN), "1,500");
        check(Interval.amount(5_400, TimeUnit.HOURS, Locale.US), "1.500");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMAN);
            check(Interval.amount(5_400, TimeUnit.HOURS), "1,500");
            Locale.setDefault(Locale.US);
            check(Interval.amount(5_400, TimeUnit.HOURS), "1.500");
        } finally {
            Locale.setDefault(previous);
        }
        System.out.println("IntervalRemainingSelfTest: all checks passed");
    }

    private static void check(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected '" + expected + "' but received '" + actual + "'");
        }
    }
}
