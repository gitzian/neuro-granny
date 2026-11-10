package org.gitzian.neurogranny.view;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.gitzian.neurogranny.model.ReviewEvent;

/**
 * Aggregates recall attempts into calendar-aligned chart buckets.
 *
 * <p>Each incorrect password submission is a separate event. Each completed
 * review contributes one correct event, regardless of earlier mistakes in
 * that review. The wrong-attempt count attached to a correct event is not
 * added again, because those attempts have already been recorded separately.
 *
 * <p>All bucket boundaries use the requested time zone. This makes daily
 * counts independent of daylight-saving changes.
 */
public final class ReviewChartData {
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1_000L;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;
    private static final int HOURS_PER_DAY = 24;
    private static final int DAYS_PER_WEEK = 7;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int MAX_DAILY_RANGE_DAYS = 31;
    private static final int MAX_WEEKLY_RANGE_WEEKS = 26;
    private static final int MAX_MONTHLY_BUCKETS = 24;

    public enum Resolution {
        HOUR,
        DAY,
        WEEK,
        MONTH
    }

    public static final class Bucket {
        public final long startMillis;
        public final long endMillis;
        public int correct;
        public int incorrect;

        private Bucket(long startMillis, long endMillis) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
        }
    }

    private final Resolution resolution;
    private final List<Bucket> buckets;
    private final int totalCorrect;
    private final int totalIncorrect;
    private final int maximumCount;
    private final int populatedBuckets;

    private ReviewChartData(Resolution resolution, List<Bucket> buckets,
            int totalCorrect, int totalIncorrect) {
        this.resolution = resolution;
        this.buckets = Collections.unmodifiableList(buckets);
        this.totalCorrect = totalCorrect;
        this.totalIncorrect = totalIncorrect;

        int highestCount = 0;
        int bucketsWithEvents = 0;
        for (Bucket bucket : buckets) {
            highestCount = Math.max(highestCount, Math.max(bucket.correct, bucket.incorrect));
            if (bucket.correct > 0 || bucket.incorrect > 0) {
                bucketsWithEvents++;
            }
        }
        maximumCount = highestCount;
        populatedBuckets = bucketsWithEvents;
    }

    public Resolution resolution() {
        return resolution;
    }

    public List<Bucket> buckets() {
        return buckets;
    }

    public int totalCorrect() {
        return totalCorrect;
    }

    public int totalIncorrect() {
        return totalIncorrect;
    }

    public int maximumCount() {
        return maximumCount;
    }

    public int populatedBuckets() {
        return populatedBuckets;
    }

    public boolean isEmpty() {
        return totalCorrect == 0 && totalIncorrect == 0;
    }

    public static ReviewChartData aggregate(List<ReviewEvent> events, long fromMillis,
            long toMillis, TimeZone timeZone, Locale locale) {
        if (fromMillis > toMillis) {
            throw new IllegalArgumentException("History start must not exceed end");
        }

        long duration = toMillis - fromMillis;
        Resolution resolution = selectResolution(duration);
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(fromMillis);
        alignToBucketStart(calendar, resolution);

        int monthsPerBucket = resolution == Resolution.MONTH
                ? monthsPerBucket(fromMillis, toMillis, timeZone, locale) : 1;
        List<Bucket> buckets = new ArrayList<>();
        do {
            long bucketStart = calendar.getTimeInMillis();
            advance(calendar, resolution, monthsPerBucket);
            long bucketEnd = calendar.getTimeInMillis();
            buckets.add(new Bucket(bucketStart, bucketEnd));
        } while (calendar.getTimeInMillis() <= toMillis);

        int correct = 0;
        int incorrect = 0;
        for (ReviewEvent event : events) {
            if (event.timestamp < fromMillis || event.timestamp > toMillis) {
                continue;
            }
            for (Bucket bucket : buckets) {
                if (event.timestamp >= bucket.startMillis && event.timestamp < bucket.endMillis) {
                    if (event.correct) {
                        bucket.correct++;
                        correct++;
                    } else {
                        bucket.incorrect++;
                        incorrect++;
                    }
                    break;
                }
            }
        }
        return new ReviewChartData(resolution, buckets, correct, incorrect);
    }

    private static Resolution selectResolution(long durationMillis) {
        if (durationMillis <= HOURS_PER_DAY * MILLIS_PER_HOUR) {
            return Resolution.HOUR;
        }
        if (durationMillis <= MAX_DAILY_RANGE_DAYS * MILLIS_PER_DAY) {
            return Resolution.DAY;
        }
        if (durationMillis <= MAX_WEEKLY_RANGE_WEEKS * DAYS_PER_WEEK * MILLIS_PER_DAY) {
            return Resolution.WEEK;
        }
        return Resolution.MONTH;
    }

    private static void alignToBucketStart(Calendar calendar, Resolution resolution) {
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        if (resolution != Resolution.HOUR) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }
        if (resolution == Resolution.WEEK) {
            int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
            int firstDay = calendar.getFirstDayOfWeek();
            calendar.add(Calendar.DAY_OF_YEAR, -((currentDay - firstDay + 7) % 7));
        } else if (resolution == Resolution.MONTH) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static void advance(Calendar calendar, Resolution resolution, int monthsPerBucket) {
        switch (resolution) {
            case HOUR:
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                break;
            case DAY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, monthsPerBucket);
                break;
            default:
                throw new IllegalStateException("Unsupported chart resolution");
        }
    }

    private static int monthsPerBucket(long fromMillis, long toMillis,
            TimeZone timeZone, Locale locale) {
        Calendar from = Calendar.getInstance(timeZone, locale);
        Calendar to = Calendar.getInstance(timeZone, locale);
        from.setTimeInMillis(fromMillis);
        to.setTimeInMillis(toMillis);

        int monthCount = (to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * MONTHS_PER_YEAR
                + to.get(Calendar.MONTH) - from.get(Calendar.MONTH) + 1;
        return Math.max(1, (monthCount + MAX_MONTHLY_BUCKETS - 1) / MAX_MONTHLY_BUCKETS);
    }
}
