import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.gitzian.neurogranny.model.ReviewEvent;
import org.gitzian.neurogranny.model.ReviewSettings;
import org.gitzian.neurogranny.view.ReviewChartData;

/** Standalone aggregation checks; no Android SDK or JUnit dependency required. */
public final class ReviewChartDataSelfTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final Locale LOCALE = Locale.ENGLISH;

    public static void main(String[] arguments) {
        dailyCountsDoNotDoubleCountFailedAttempts();
        hourlyGroupingUsesSeparateHours();
        weeklyGroupingHandlesLongerSelections();
        emptyHistoryHasNoSyntheticCounts();
        localCalendarDaySurvivesDaylightSavingTransition();
        longRangesRemainBounded();
        invalidTimeRangeIsRejected();
        System.out.println("Review chart aggregation: 7 tests passed");
    }

    private static void dailyCountsDoNotDoubleCountFailedAttempts() {
        long today = timestamp(2026, Calendar.NOVEMBER, 8, 14, 0, UTC);
        long from = today - 13L * 24L * 60L * 60L * 1_000L;
        List<ReviewEvent> events = new ArrayList<>();

        for (int attempt = 0; attempt < 6; attempt++) {
            events.add(event(today + attempt * 1_000L, false, attempt + 1));
        }
        for (int review = 0; review < 3; review++) {
            events.add(event(today + (review + 6) * 1_000L, true, 2));
        }

        ReviewChartData chart = ReviewChartData.aggregate(events, from,
                today + 60_000L, UTC, LOCALE);
        assertEquals(ReviewChartData.Resolution.DAY, chart.resolution());
        assertEquals(6, chart.totalIncorrect());
        assertEquals(3, chart.totalCorrect());
        assertEquals(1, chart.populatedBuckets());
        ReviewChartData.Bucket lastBucket = chart.buckets().get(chart.buckets().size() - 1);
        assertEquals(6, lastBucket.incorrect);
        assertEquals(3, lastBucket.correct);
    }

    private static void hourlyGroupingUsesSeparateHours() {
        long from = timestamp(2026, Calendar.NOVEMBER, 8, 9, 30, UTC);
        List<ReviewEvent> events = new ArrayList<>();
        events.add(event(from + 1_000L, false, 1));
        events.add(event(from + 2L * 60L * 60L * 1_000L, true, 0));
        events.add(event(from - 1_000L, true, 0));

        ReviewChartData chart = ReviewChartData.aggregate(events, from,
                from + 4L * 60L * 60L * 1_000L, UTC, LOCALE);
        assertEquals(ReviewChartData.Resolution.HOUR, chart.resolution());
        assertEquals(1, chart.totalIncorrect());
        assertEquals(1, chart.totalCorrect());
        assertEquals(2, chart.populatedBuckets());
    }

    private static void weeklyGroupingHandlesLongerSelections() {
        long from = timestamp(2026, Calendar.JULY, 1, 0, 0, UTC);
        long to = timestamp(2026, Calendar.NOVEMBER, 8, 12, 0, UTC);
        List<ReviewEvent> events = new ArrayList<>();
        events.add(event(timestamp(2026, Calendar.JULY, 15, 12, 0, UTC), true, 0));
        events.add(event(timestamp(2026, Calendar.OCTOBER, 8, 12, 0, UTC), false, 1));

        ReviewChartData chart = ReviewChartData.aggregate(events, from, to, UTC, LOCALE);
        assertEquals(ReviewChartData.Resolution.WEEK, chart.resolution());
        assertEquals(1, chart.totalCorrect());
        assertEquals(1, chart.totalIncorrect());
        assertTrue(chart.buckets().size() <= 22);
    }

    private static void emptyHistoryHasNoSyntheticCounts() {
        long from = timestamp(2026, Calendar.NOVEMBER, 8, 0, 0, UTC);
        ReviewChartData chart = ReviewChartData.aggregate(new ArrayList<>(),
                from, from + 20_000L, UTC, LOCALE);
        assertTrue(chart.isEmpty());
        assertEquals(0, chart.maximumCount());
        assertEquals(0, chart.populatedBuckets());
        assertEquals(1, chart.buckets().size());
    }

    private static void localCalendarDaySurvivesDaylightSavingTransition() {
        TimeZone vienna = TimeZone.getTimeZone("Europe/Vienna");
        long today = timestamp(2026, Calendar.MARCH, 29, 15, 0, vienna);
        long yesterday = timestamp(2026, Calendar.MARCH, 28, 15, 0, vienna);
        long from = timestamp(2026, Calendar.MARCH, 27, 0, 0, vienna);

        List<ReviewEvent> events = new ArrayList<>();
        events.add(event(today, true, 0));
        events.add(event(yesterday, false, 1));
        ReviewChartData chart = ReviewChartData.aggregate(events, from,
                timestamp(2026, Calendar.APRIL, 1, 12, 0, vienna), vienna, Locale.GERMAN);
        assertEquals(ReviewChartData.Resolution.DAY, chart.resolution());
        assertEquals(1, chart.totalIncorrect());
        assertEquals(1, chart.totalCorrect());
        assertEquals(2, chart.populatedBuckets());
    }

    private static void longRangesRemainBounded() {
        long from = timestamp(2020, Calendar.JANUARY, 1, 0, 0, UTC);
        long to = timestamp(2026, Calendar.NOVEMBER, 8, 12, 0, UTC);
        ReviewChartData chart = ReviewChartData.aggregate(new ArrayList<>(),
                from, to, UTC, LOCALE);
        assertEquals(ReviewChartData.Resolution.MONTH, chart.resolution());
        assertTrue(chart.buckets().size() <= 26);
        assertEquals(0, chart.totalCorrect());
    }

    private static void invalidTimeRangeIsRejected() {
        boolean rejected = false;
        try {
            ReviewChartData.aggregate(new ArrayList<>(), 20L, 10L, UTC, LOCALE);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    private static ReviewEvent event(long timestamp, boolean correct, int wrongAttempts) {
        ReviewEvent event = new ReviewEvent();
        event.timestamp = timestamp;
        event.correct = correct;
        event.wrongAttempts = wrongAttempts;
        return event;
    }

    private static long timestamp(int year, int month, int day, int hour,
            int minute, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone, LOCALE);
        calendar.clear();
        calendar.set(year, month, day, hour, minute);
        return calendar.getTimeInMillis();
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
