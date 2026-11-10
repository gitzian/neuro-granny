package org.gitzian.neurogranny.view;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/** Reads the installed package version. Build numbers remain Gradle's responsibility. */
public final class AppVersion {
    private AppVersion() {
    }

    public static String display(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long buildNumber = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode() : info.versionCode;
            return "[" + buildNumber + "] " + info.versionName;
        } catch (PackageManager.NameNotFoundException exception) {
            return context.getString(org.gitzian.neurogranny.R.string.version_unknown);
        }
    }
}
