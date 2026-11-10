package org.gitzian.neurogranny.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.gitzian.neurogranny.schedule.ReminderScheduler;

/** Database is credential-encrypted storage; BOOT_COMPLETED runs after user unlock. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) ReminderScheduler.reconcile(context);
    }
}
