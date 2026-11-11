# Testing

## Automated checks

Run from the repository root:

```bash
./gradlew clean testDebugUnitTest assembleDebug
bash tests/run_core_selftest.sh
bash tests/verify_source.sh
```

## Device tests

Before release, verify at least the following on Android 6 (API 23) and a current Android version:

1. Recoverable and Verify-only creation, verification, editing and review scheduling.
2. Correct/incorrect review history, repeated failures and secret changes.
3. Database export/import, duplicate-title conflicts, corrupted input and cancellation.
4. Due reminders, notification permission, reboot, force-stop and battery restrictions.
5. Keyboard behavior, rotation, large fonts and smaller displays.
6. Absence of secrets in application logs and unexpected network activity.

Use test secrets, not real credentials. Record any untested combinations or device-specific failures in release notes.
