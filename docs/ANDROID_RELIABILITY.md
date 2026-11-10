# Reminders and Android limitations

Neuro Granny stores review due dates in its local database. Notifications and alarms are reminders of that state, not the source of truth.

- Android can delay inexact alarms during Doze, battery optimization or manufacturer-specific power management. Reminder delivery is **not guaranteed at an exact time**.
- A device reboot requires reminder scheduling to be restored after the first unlock.
- A force-stopped app cannot receive broadcasts until the user opens it again.
- Android 13 and later require notification permission. If permission is denied, due reviews remain accessible in the app.
- On Android 14 and later, users can dismiss many ongoing notifications. Dismissing one does not complete a review.
- The app does not request exact-alarm permission.

If reminders are late or missing, check notification permission and the device's battery restrictions. Opening the app refreshes its due state.
