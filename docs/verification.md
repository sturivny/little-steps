# MVP verification

The APK and original source code were built and tested on October 31, 2026. These are the recorded results from that run, not a new November 7 test run.

- Debug APK: assembled and installed successfully on an Android 17 / API 37 emulator.
- Unit tests: 6 passed (5 habit-logic checks and the original template test).
- Device tests: 7 passed, covering app context, Room disk persistence, manual drag order and persistence, creation and completion toggling, activity recreation, immediate EN/RU switching and persistence, advanced goals/units, measured totals, and calendar history.
- Android Lint: no errors. Non-blocking warnings include available dependency updates and two external lifecycle checks skipped because of library/Lint API compatibility.
- Debug APK signature verified. The downloadable file matches the APK tested in the original run.

## Reproduce

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The device tests need a running emulator or a phone with USB debugging enabled. See the README for JDK/SDK requirements. Recorded JUnit XML reports are in [test-results](test-results/).

## Remaining limitations

Physical haptics and timed notification delivery still need a real-device check. Reminders use WorkManager, so Android battery policies may delay them. Resave reminders after changing time zones or daylight-saving transitions to realign their 24-hour schedule. Notification permission is required on Android 13+.

Changing an activity's goal, unit or schedule also changes how older entries are displayed/evaluated; historical quantities remain stored, but settings are not versioned by date. Archived activities disappear from today onward; an unarchive screen is not included.

Release signing and store publishing are not configured. No account, cloud sync, social features, or advanced analytics are included in this MVP.
