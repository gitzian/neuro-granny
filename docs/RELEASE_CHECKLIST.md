# Release checklist

Use a fresh public source checkout for the release build. The application version is updated deliberately in `app/build.gradle`.

## Source and build

- [ ] Review the public diff for private data, generated files, credentials and incompatible licenses.
- [ ] Confirm `versionName`, increasing `versionCode`, application ID and minimum supported SDK.
- [ ] Confirm third-party licenses and required notices.
- [ ] Run `./gradlew clean testDebugUnitTest assembleRelease` and the checks documented in [Test plan](TEST_PLAN.md).
- [ ] Build and sign the release APK using credentials kept outside Git.

## Device testing

- [ ] Check both secret storage modes, correct and incorrect answers, and review scheduling.
- [ ] Test database export and import, including conflicts and cancellation.
- [ ] Test reminders after reboot and under notification restrictions.
- [ ] Test keyboard, rotation and large-font layouts on API 23 and a recent Android version.
- [ ] Verify that no secrets appear in logs, screenshots or unintended exports.

## Publication

- [ ] Tag the exact reviewed source commit using `vX.Y.Z`.
- [ ] Publish source, license, changelog and release notes.
- [ ] If distributing an APK, attach its SHA-256 checksum and identify the signing source.
- [ ] Keep F-Droid submissions and signing independent from GitHub APK releases.
