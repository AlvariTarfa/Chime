# Chime
Chime: a goal-first Android launcher that cuts screen time with mindful pauses, focus modes, and daily goal tracking. Built with Kotlin and Jetpack Compose.

## Features

- Use Chime as the Home launcher and browse installed personal/work-profile apps.
- Set goals, tasks, and up to three daily priorities; review check-ins and progress.
- Add mindful pauses, optional app/category limits, focus modes, and timed focus sessions.
- Review on-device usage insights and export a CSV through Android's document picker.
- Optionally enable local notification digests, reminders, and accessibility-based pauses.
- Export/import a versioned JSON backup or delete Chime's local data in Settings.

## Build environment

Open this repository in GitHub Codespaces with **Code → Codespaces → Create codespace**. The dev container installs JDK 17, the Android command-line SDK, and Gradle through SDKMAN. It does not include an emulator.

Build and validate the debug app from the repository root:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Download it from the Codespace to your computer, connect your Android phone with USB debugging enabled, then sideload it with Android Platform Tools:

```bash
adb install -r app-debug.apk
```

### Release artifacts and signing

Build the release APK and Android App Bundle (AAB):

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

Release R8 minification and resource shrinking are enabled. Signing is configured only from these environment variables: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Provide all four through a protected CI secret store or your local shell environment. Never commit a keystore or signing credentials. If any variable is missing, Gradle prints a warning and creates unsigned release artifacts; an unsigned APK cannot be installed until it is signed.

Release output paths are `app/build/outputs/apk/release/app-release.apk` (when signed) or `app/build/outputs/apk/release/app-release-unsigned.apk` (when unsigned), and `app/build/outputs/bundle/release/app-release.aab`.

To sideload a signed release APK, download it to a computer with Android Platform Tools and run:

```bash
adb install -r app-release.apk
```

To update an installed build, the APK must have the same application ID and signing key. Otherwise uninstall the existing copy first; uninstalling removes its local app data.

The app's `versionCode` and `versionName` are set in `gradle.properties`. Update them for each Play release.

## Permissions and special access

- **Notification permission (Android 13+):** optional; allows Chime to post reminders, focus/grant-expiry alerts, and digests. Without it, scheduled work can run without displaying notifications.
- **Usage access:** optional; Android's Usage Access setting permits Chime to read usage events for screen-time insights, pickup metrics on supported Android versions, and app/category limit enforcement. Usage values are computed on-device.
- **Notification listener access:** optional and separately enabled in Android Settings after Chime's disclosure. The listener must inspect incoming notification metadata to exclude calls, alarms, media, allowed apps, and other ineligible notifications. It stores title/text only for notifications accepted into the digest; access can be revoked in Android Settings or batching can be turned off in Chime.
- **Accessibility service:** optional and separately enabled after Chime's disclosure. It observes window-state changes and reads the event's package name to apply the same pause/limit decision used by Chime's launcher. It does not request window content or key-event access. Turn it off in Android Accessibility settings at any time.
- **Installed-app visibility:** Chime uses Android's launcher/profile APIs and a limited `<queries>` block for launcher, home, dialer, SMS, Settings, and supported icon-pack intents. It does not request `QUERY_ALL_PACKAGES`.

## Notification digest

The optional notification digest requires explicit consent and Android notification-listener access. Android gives the listener technical access to full notification objects from all apps. Chime checks the originating package, ongoing/group-summary flags, category, media-session presence, default dialer/SMS identity, your allow-list, app classification, and focus/digest settings; it reads and stores title/text only for eligible notifications from apps classified as Distracting. Eligible content remains in the on-device digest and is not sent over the network.

Cancelled notifications cannot be restored to the system shade; they live only in Chime's in-app digest. Turn batching off in Chime's digest settings or revoke notification-listener access in Android Settings to stop interception.

See [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) for the plain-language data policy and [docs/PLAY_STORE_CHECKLIST.md](docs/PLAY_STORE_CHECKLIST.md) before publishing.
