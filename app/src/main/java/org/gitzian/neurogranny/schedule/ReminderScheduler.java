package org.gitzian.neurogranny.schedule;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.android.AppLanguage;
import org.gitzian.neurogranny.android.MainActivity;
import org.gitzian.neurogranny.android.ReminderReceiver;
import org.gitzian.neurogranny.data.MemoryRepository;
import org.gitzian.neurogranny.data.NeuroDatabase;
import org.gitzian.neurogranny.model.Memory;

/**
 * Single OS alarm for the earliest future review, individual ongoing due notifications.
 * Reminder state lives in SQLite; dismissing a notification never completes a review.
 * Inexact alarms intentionally avoid special exact-alarm permission.
 */
public final class ReminderScheduler {
    public static final String CHANNEL = "recall";
    public static final String EXTRA_MEMORY_ID = "memory_id";
    private final Context context;
    private final MemoryRepository repo;
    public ReminderScheduler(Context context, MemoryRepository repo) {
        this.context = AppLanguage.localizedContext(context.getApplicationContext());
        this.repo = repo;
    }

    public static void reconcile(Context context) {
        NeuroDatabase db = new NeuroDatabase(context);
        try {
            new ReminderScheduler(context, new MemoryRepository(db)).refresh();
        } finally {
            db.close();
        }
    }

    public void refresh() {
        createChannel();
        long now = System.currentTimeMillis(), earliest = Long.MAX_VALUE;
        for (Memory m : repo.all()) {
            if (m.due || m.nextDue <= now) {
                if (!m.due) repo.setDue(m.id);
                show(m);
            } else if (m.nextDue < earliest) earliest = m.nextDue;
        }
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent alarm = alarmIntent();
        alarms.cancel(alarm);
        if (earliest != Long.MAX_VALUE) {
            // setAndAllowWhileIdle is available on minSdk 23; delivery may be delayed.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Math.max(now + 1000, earliest), alarm);
        }
    }

    public void cancelMemory(long id) {
        NotificationManager n = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (n != null) n.cancel(notificationId(id));
    }

    private PendingIntent alarmIntent() {
        Intent i = new Intent(context, ReminderReceiver.class).setAction("org.gitzian.neurogranny.CHECK_DUE");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 101, i, flags);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL, context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(context.getString(R.string.notification_channel_description));
                nm.createNotificationChannel(channel);
            }
        }
    }

    private static int notificationId(long id) {
        // SQLite local ID is normally small; collision-free for the supported signed int range.
        if (id > Integer.MAX_VALUE - 1000) throw new IllegalStateException("Memory ID exceeds notification range");
        return (int) id + 1000;
    }

    private void show(Memory m) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent open = new Intent(context, MainActivity.class);
        open.putExtra(EXTRA_MEMORY_ID, m.id);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent tap = PendingIntent.getActivity(context, notificationId(m.id), open, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context,
                CHANNEL) : new Notification.Builder(context);
        Notification n = builder.setSmallIcon(R.drawable.notification_cane).setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_recall, m.title)).setContentIntent(tap).setOngoing(true).setAutoCancel(false)
                .setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_REMINDER).build();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) try {
            nm.notify(notificationId(m.id), n);
        } catch (SecurityException ignored) {
            /* due state survives denied notifications */
        }
    }
}
