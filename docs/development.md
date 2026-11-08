# Build and test Little Steps

For a user introduction and screenshots, start with the [README](../README.md).

## Requirements

- Android Studio and Android SDK 37.
- JDK 25, as specified in `gradle/gradle-daemon-jvm.properties`.
- Android 10 / API 29 or newer for the app.

Open the project folder in Android Studio and sync Gradle. Set the SDK location in Android Studio, or in your own untracked `local.properties` file.

On macOS, Android Studio's bundled JDK can be selected with:

```bash
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
```

## Build and check

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To run the device tests, start an emulator or connect an Android phone with USB debugging enabled:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Install your local build with Android Studio or:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Device tests clear the app's database and preferences to create repeatable test conditions. Use a dedicated emulator or test installation.

## Project overview

The app uses Kotlin, Jetpack Compose, Room for activities and date-keyed totals, DataStore for language preferences, and WorkManager for local reminders. Application ID: `com.example.littlesteps`. Compile/target SDK: 37. AGP: 9.5.0. Gradle: 9.6.0. Room uses AGP's legacy-kapt bridge with built-in Kotlin. Language splits are disabled so English and Russian both ship offline.

There is no login, backend, network permission, analytics, or cloud sync. Android backup is enabled. User-written names and units are not translated when changing the interface language.

## Reminders and historical settings

WorkManager reminders are approximate and can be delayed by Android battery policies. They skip completed, off-schedule, and archived activities. Android 13+ requires notification permission. After a time-zone or daylight-saving change, resave a reminder to align its 24-hour schedule with the new local time. After a force-stop, reopen the app to allow reminders to resume.

Goal, unit and schedule settings belong to the whole activity. Historical totals are retained, but past settings are not snapshotted separately.

## Included test APK

The [downloadable debug APK](../downloads/LittleSteps-MVP-debug.apk) was built on October 31, 2026. Its SHA-256 is:

```text
f93b474bddd8addb429e07df2498ec2379bacd243db53b9c61d5ae219fe9e9aa
```

It is signed with an Android debug certificate. Release signing and store publishing are not configured. See [verification notes](verification.md) for recorded checks and remaining limitations.
