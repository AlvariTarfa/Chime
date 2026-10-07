# Google Play release checklist

## Build and app content

- [ ] Set the next `versionCode` and `versionName` in `gradle.properties`.
- [ ] Run the documented release quality-gate command and inspect the generated mapping file and lint report.
- [ ] Build a signed AAB using protected CI secrets or local environment variables: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Confirm the uploaded bundle's signing identity; never upload or commit the keystore or passwords.
- [ ] Confirm application ID, launcher icon, display name, supported locales, and target device behavior.
- [ ] Publish the privacy policy at a stable public URL and enter that URL in Play Console. The repository copy is [PRIVACY_POLICY.md](./PRIVACY_POLICY.md).
- [ ] Complete the app access, ads, content rating, target audience, and data safety sections based on the actual submitted build.

## Data safety answers: derive from current app behavior

The current app has no account, backend, analytics, advertising SDK, or `INTERNET` permission. It processes the following information locally; it does not transmit it to the developer:

- Goal/task/check-in text, preferences, focus settings, and pause/limit history.
- Installed-app/package metadata and, only with Usage Access, Android usage events used for local screen-time metrics and limit decisions.
- If the user enables the notification digest, notification metadata is inspected for filtering and title/text are stored locally only for accepted Distracting-app items.
- If the user enables accessibility pauses, window-state event package names are used locally; window text and key events are not collected.
- A user-selected JSON or CSV export is written to the destination selected through Android's document picker. Export files are not uploaded by Chime.

For the current build, the expected Play Console answers are **no data collected by the developer** and **no data shared with third parties**, because the app does not transmit the listed data off-device. Treat this as a behavior-derived draft, not a substitute for reviewing Play's current definitions and each question in the console. If a future library, endpoint, or feature transmits data, reassess the answers before publishing. Do not claim that all on-device processing is encrypted; the app does not implement its own database/export encryption.

## Sensitive access and declarations

- [ ] **Usage Access:** describe that it is optional and used on-device for screen-time insights, pickup metrics where supported, and app/category limit enforcement. Ensure store listing and in-app explanation are consistent.
- [ ] **AccessibilityService:** complete the Play Console declaration and any required review/recording. Explain that the service is optional, listens for window-state changes, reads only each event's package name, and applies the app's pause/limit logic. State that it does not retrieve window content, inspect screen text, or filter key events. Do not position the service as an unrelated accessibility tool.
- [ ] **Notification listener:** explain the explicit opt-in, filtering, local title/text storage for accepted notifications, the digest controls, and how to revoke access. Confirm the console's current policy requirements for notification access.
- [ ] **Notifications:** explain the Android 13+ runtime permission and which optional reminders/alerts/digests use it.

## Package visibility and target API

- [ ] `QUERY_ALL_PACKAGES` is **not used**. The manifest's `<queries>` block declares the launcher, home, dialer, SMS, Settings, and supported icon-pack intents needed by current flows; verify those flows on a current Android device and do not add broad package visibility without a new documented need and policy review.
- [ ] Current build values: `minSdk 26`, `targetSdk 36`, `compileSdk 36`. Check Play Console's currently required target API at submission time and update the target SDK if policy changes.
- [ ] Test install/upgrade, launcher selection, usage-access status, notification permission denial/grant, notification-listener revocation, accessibility enable/disable, backup/restore, and data deletion on physical devices, including the oldest supported Android version available.
- [ ] Inspect Play pre-launch reports and crashes. Resolve blockers and review warnings before rollout.

## Store assets and screenshots

Capture current production UI on representative phone sizes. Prepare and review at least:

- [ ] Home screen with daily priorities and app dock.
- [ ] Searchable app drawer (include grid or icon view if it is part of the listing story).
- [ ] Goal detail with tasks and progress.
- [ ] Mindful pause/intercept screen.
- [ ] Focus mode/session screen.
- [ ] Insights chart/report screen.
- [ ] Check-in or weekly review screen.
- [ ] Settings showing optional permission explanations and data-management controls.

Use real app screens, remove personal data from captures, and confirm the assets meet current Play image dimensions and content rules.

## Closed testing and rollout

1. Create the Play Console app entry and complete app content, privacy policy, data safety, accessibility declaration, and store listing.
2. Upload the signed AAB to an internal test first. Install from Play and verify signing, update behavior, and the launch path.
3. Promote the verified build to a closed testing track. Add tester email addresses or a Google Group, select countries/regions, and share the opt-in URL.
4. Ask testers to exercise onboarding, Home selection, goals/tasks, interception/limits, focus, check-ins, insights, notification/accessibility opt-ins, backup/restore, and deletion. Collect crash reports and device/Android-version details.
5. Check the current Play Console eligibility and closed-testing duration requirements for this developer account. Keep the test active for the required period and retain tester feedback.
6. Review pre-launch reports and policy status, fix release blockers, then request production access or promote to production as the console permits. Start with a staged rollout and monitor crashes and reviews.
