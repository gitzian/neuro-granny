package org.gitzian.neurogranny.view;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.model.TimeUnit;

/** Localized UI formatting; scheduling arithmetic stays independent of resources. */
public final class TimeFormatter {
    private static final long SECONDS_PER_MINUTE = TimeUnit.MINUTES.seconds();
    private static final long SECONDS_PER_HOUR = TimeUnit.HOURS.seconds();
    private static final long SECONDS_PER_DAY = TimeUnit.DAYS.seconds();

    private final Context context;

    public TimeFormatter(Context context) {
        this.context = context;
    }

    public String remaining(long seconds) {
        if (seconds <= 0L) {
            return context.getString(R.string.due_now);
        }
        if (seconds < SECONDS_PER_MINUTE) {
            return quantity(R.plurals.short_seconds, seconds);
        }
        long totalMinutes = (seconds + 30L) / SECONDS_PER_MINUTE;
        if (totalMinutes < 60L) {
            return quantity(R.plurals.short_minutes, totalMinutes);
        }
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours < 24L) {
            String text = quantity(R.plurals.short_hours, hours);
            return minutes == 0L ? text : text + " " + quantity(R.plurals.short_minutes, minutes);
        }
        long days = hours / 24L;
        long remainingHours = hours % 24L;
        String text = quantity(R.plurals.short_days, days);
        return remainingHours == 0L ? text : text + " " + quantity(R.plurals.short_hours, remainingHours);
    }

    public String interval(long seconds) {
        if (seconds <= 0L) {
            return context.getString(R.string.due_now);
        }
        if (seconds % SECONDS_PER_DAY == 0L) {
            return quantity(R.plurals.long_days, seconds / SECONDS_PER_DAY);
        }
        if (seconds % SECONDS_PER_HOUR == 0L) {
            return quantity(R.plurals.long_hours, seconds / SECONDS_PER_HOUR);
        }
        if (seconds % SECONDS_PER_MINUTE == 0L) {
            return quantity(R.plurals.long_minutes, seconds / SECONDS_PER_MINUTE);
        }
        return quantity(R.plurals.long_seconds, seconds);
    }

    public String dateTime(long timestamp) {
        DateFormat format = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, locale());
        return format.format(new Date(timestamp));
    }

    public Locale locale() {
        Configuration configuration = context.getResources().getConfiguration();
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? configuration.getLocales().get(0) : configuration.locale;
    }

    private String quantity(int pluralId, long count) {
        int safe = (int) Math.min(Integer.MAX_VALUE, count);
        return context.getResources().getQuantityString(pluralId, safe, count);
    }
}
