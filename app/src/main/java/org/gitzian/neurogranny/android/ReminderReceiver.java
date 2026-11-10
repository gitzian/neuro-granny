package org.gitzian.neurogranny.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.gitzian.neurogranny.schedule.ReminderScheduler;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.reconcile(context);
    }
}
