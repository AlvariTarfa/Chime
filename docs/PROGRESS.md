# Progress

## Prompt checklist

- [ ] Prompt 0
- [x] Prompt 1 — project skeleton
- [x] Prompt 2 — live app list, app search, launch flow, and alphabet index
- [x] Prompt 3 — Room persistence, settings store, Hilt wiring, and tests
- [ ] Prompt 4 — implementation and JVM/build/lint validated; manual device checks remain
- [x] Prompt 5 — goals domain models, Room repository, streak/progress logic, and use cases
- [x] Prompt 6 — goals UI, goal/task editing and tracking, daily priorities, app-to-goal linking, and JVM/build/lint validated
- [x] Prompt 7 — goal-first home, date-aware priorities/tasks, dock gestures, and JVM/build/lint validated; manual device checks remain
- [x] Prompt 8 — single AppLauncher interception path, mindful pause UI/countdown, grants, escalation logging, and timed-session notifications implemented; build, JVM tests, and lint pass, manual device checks remain
- [x] Prompt 9 — usage-access permission flow, event-paired daily per-app foreground time, pickup/first pickup/longest session data, 30-second current-day cache, and pure pairing tests implemented; debug/release builds, JVM tests, and lint pass; manual Digital Wellbeing comparison pending
- [x] Prompt 10 — per-app/category daily limits, strict limit-reached pause and reasoned overrides, app/category limit editors, near-limit snackbars, and build/unit-test/lint validated; manual device check remains
- [x] Prompt 11 — scheduled/manual focus modes, app filtering, Sleep appearance, focus-session countdown/persistence/crediting, and inexact completion alarms implemented; build, 95 JVM tests, and lint pass; manual device checks remain
- [ ] Prompt 12
- [ ] Prompt 13
- [ ] Prompt 14
- [ ] Prompt 15
- [ ] Prompt 16
- [ ] Prompt 17
- [ ] Prompt 18
- [ ] Prompt 19

## What exists

- Android app skeleton using Kotlin, Jetpack Compose, Material 3, and Hilt.
- Single launcher activity with home, drawer, goals, and settings destinations; the goals flow is implemented through list, edit, detail, and priorities screens.
- Transparent activity window and transparent root Compose surface for wallpaper visibility.
- Home-screen icon, Material 3 light/dark themes, and an API 31+ dynamic-color path with fallback palettes.
- Version catalog with dependencies verified against AGP 8.13.2 and API 36; Room and Preferences DataStore persistence are implemented, with WorkManager still catalog-only.
- Architecture notes and a JUnit4 route-identifier test.
- Android API 36 compile/target SDK with min SDK 26; generated Gradle 8.13 wrapper.
- Debug APK builds and the JUnit4 suite passes; lint completes with 0 errors. Remaining lint warnings are dependency-version advisories.
- LauncherApps-backed app repository enumerates personal/work profiles, updates its StateFlow after package callbacks, supports launch, and caches sized icons in memory.
- Home screen displays a live HH:mm clock and opens a searchable, alphabet-indexed text-only app drawer; search and alphabet-bucket helpers have JUnit4 coverage.
- Verification for Prompt 2: `./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1g :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeds.
- Gradle wrapper is version 8.13; Prompt 2 validation passes all 11 unit tests and lint reports 0 errors with 16 dependency-version advisories.
- The latest installed SDK platform reported by `sdkmanager --list_installed` is Android 36.
- Prompt 3 persistence includes all eleven Room entities and basic DAOs, version-1 schema export, debug-only destructive migration fallback, string-backed domain enums, Preferences DataStore settings, and Hilt database/settings/dispatcher providers.
- JVM tests cover safe enum storage parsing and temporary-file DataStore defaults and round trips; instrumented DAO tests cover cascade deletion, read/observe, and task-log uniqueness.
- Prompt 3 verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug` succeeds. Android tests were compiled only and were not run in Codespaces.
- Room schema `app/schemas/com.savatech.chimelauncher.data.db.AppDatabase/1.json` exists and is tracked in commit `131d49f`.
- Prompt 4 implementation adds a transaction-backed app configuration repository, visible/hidden app flows, the four-app pin cap and order updates, app action sheets, a home dock, drawer text/icon/grid modes, and settings for drawer mode and hidden apps.
- Prompt 4 JVM tests cover hidden-app filtering, missing configuration defaults, uninstalled-app configurations, pin-limit rejection, reorder count, and freeing a slot by unpinning.
- Prompt 5 adds typed goal/task/log/priority domain models and entity mappers; a Room-backed goals repository with goal/task CRUD, date-range logs, and ordered priorities; a pure streak calculator with freeze credits; pure progress calculations; and overview, priority-validation, and completion-toggle use cases.
- Prompt 5 includes individual tests for all 12 required streak scenarios, progress edge cases, priority validation, overview scheduling/completion, idempotent task-log upserts, and entity mapping round trips.
- Current verification: `./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1g :app:testDebugUnitTest` passes all 48 tests; `./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1g :app:assembleDebug :app:lintDebug` succeeds with 0 lint errors and 18 dependency/toolchain advisories. A plain daemon-backed test rerun exited unexpectedly, then the no-daemon retry passed.
- Prompt 6 adds Active/Paused/Completed/Archived goal filtering with daily progress and task streak summaries; goal editing with title/why/target/category validation and a date picker; goal details with task recurrence, reminder-time storage, measurable daily values, completion logs, streak/freeze display, and goal/task actions; and a priority editor capped at three active goals with explicit reordering and typed errors.
- Prompt 6 wires string-argument routes from the drawer to the goals list, edit, detail, and priorities screens. The existing app action sheet can now link/unlink an app configuration to an active goal. No reminder scheduling or home-screen feature was added.
- Prompt 6 adds Turbine ViewModel tests for goal validation/create, today's task completion/progress updates, priority error handling, and status filtering; goal route tests also cover argument builders.
- Prompt 6 verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeds.
- Prompt 7 replaces the placeholder home with a minute-aligned clock and date, up to three ordered daily priorities with overview progress and inline unfinished-task checkboxes, the next-task completion action, and the pinned app dock. Empty states open goal creation or the existing Prompt 6 priority editor.
- Prompt 7 observes date changes using an injectable system `Clock`, so priorities and overview are reloaded for the new date. Wallpaper-aware surface scrims, swipe-up drawer access, and the empty-space long-press Settings/Goals menu are wired.
- Prompt 7 adds fake-backed HomeViewModel tests for the empty state, three priorities/task completion progress, and midnight rollover. Verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeds; all 55 JVM tests pass and lint reports 0 errors.
- Prompt 8 adds a pure interception policy with 5s/8s/15s friction defaults, opened-today delay escalation capped at 30s, Room-backed local-day event counting and expiring grants, and a singleton AppLauncher used by the drawer, search auto-launch, and dock. The full-screen intercept route persists its real-time countdown deadline across recreation, displays the day's top priority, logs cancel/open choices, creates 5/10/15-minute grants, and schedules inexact expiry alarms plus permission-gated notifications.
- Prompt 8 tests cover policy friction levels/escalation/cap/bypass, daylight-saving-aware local-day counts and expired grants, and AppLauncher pause/grant/neutral paths. Verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeds; all 65 JVM tests pass.
- Prompt 9 adds an AppOps-backed usage permission state refreshed on Settings lifecycle `ON_RESUME`, an intent to usage-access settings, and a settings row explaining the grant. Usage events (not buckets) are paired by package/component over a local-day window, with per-day/per-app time, pickup count/first pickup on API 28+, longest session, launcher exclusions, and past-day/today cache policies. In debug builds, granting access then returning to Settings writes today's per-app milliseconds and pickup count to Logcat under `ChimeUsageDebug`.
- Prompt 9 pure event-pairing tests cover foreground/background pairing, interleaved apps, window edges/orphan pauses, component switches, duplicate resumes, empty data, pickup counting, and longest session selection. Device validation comparing granted-access totals with Digital Wellbeing remains pending.
- Prompt 10 adds pure daily-limit evaluation and app-over-category precedence, category default limits in Preferences DataStore, and 0-720 minute editors for per-app and category limits. App launch checks today's package usage only when usage access is granted; reached limits override grants and pause productive/neutral apps too, with doubled delays capped at 45 seconds.
- Prompt 10's strict pause displays usage and limit, requires a trimmed reason of at least ten characters before opening, offers 5/10-minute grants, and logs `LIMIT_OVERRIDE` with the entered reason. Home and drawer foreground checks show each app's 80%-limit Snackbar once per app/date in memory. Settings explains that usage access is required for enforcement.
- Prompt 10 verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeds. Manual check of a two-minute limit, reasoned override/event logging, and clearing the limit remains pending.

## What's next

- Continue with Prompt 11.
- Manual phone check that personal/work apps enumerate, package changes refresh the list, search/launch work, and the alphabet index jumps correctly.

## Known issues

- Manual real-device installation, Home-app selection, and wallpaper confirmation have not been performed.
- Instrumented Room DAO tests have not been run; execute them on a phone or CI with an Android device/emulator.
- Lint reports 18 newer-version advisories for toolchain and dependency versions in the version catalog; no lint errors remain.
- Prompt 4 manual pin/hide/mode/category checks have not been performed.
- Prompt 6 manual checks remain: create a goal with two tasks, complete one and inspect progress/streak changes, force-stop/reopen to verify persistence, confirm the fourth daily priority is rejected and paused goals cannot be selected, and rotate the device during form editing.
- Prompt 7 manual checks remain: verify the home flow over bright/dark wallpapers, goal creation and prioritization, swipe-up drawer, dock launches, and date rollover on a real device.
- Prompt 8 manual checks remain: mark an app Distracting and confirm drawer, dock, and search launches show the pause screen; verify Go back, non-skippable countdown, grant bypass and increasing delay, and expiry notification with notification permission granted.
- Prompt 9 manual check remains: grant usage access, then compare today's per-app foreground totals and pickup count with Digital Wellbeing (target is roughly 10% agreement); pick-ups should be unavailable on API 26–27.
- Prompt 10 manual check remains: set a two-minute app limit, use it for two minutes, confirm relaunch invokes the strict pause and requires a reason, confirm the override logs and opens, then clear the app limit and confirm enforcement is removed.
- Prompt 11 adds JSON-serialized weekday schedules with overnight start-day matching, manual override expiry, one-time Work/Study/Sleep/Deep Work seeding, default dialer/SMS/system Settings allowlisting, CRUD screens with app search and weekday/time editors, a home mode chip, drawer pause banner, and mode-aware filtering shared by drawer, dock, and search. Hidden apps remain hidden.
- Prompt 11 Sleep mode applies a zero-saturation render effect on API 31+ and a muted dark scheme below API 31; the home wind-down panel shows the current clock, tomorrow's first prioritized goal when available, and an allowed-app drawer button.
- Prompt 11 focus sessions store start time and planned focus minutes in the existing Room table; the countdown derives from start plus duration. The 25/5 and 50/10 options schedule only the 25- or 50-minute focus phase; break lengths are shown as preset guidance, not persisted or timed as a second phase. Sessions activate Deep Work, survive process death, schedule inexact completion alarms, and are reconciled on launcher resume inside a Room transaction.
- Session credits mark a linked task's TaskLog complete for the local date of reconciliation. If that goal's unit is minute/minutes/min/mins, the planned focus minutes are added to that TaskLog value; hour units add fractional hours and second units add seconds. Existing non-time TaskLog values are preserved. Without a linked task, the current schema has no goal-level progress-log row, so completion is recorded on the session only and no goal value is changed. The goal's targetValue is never mutated.
- Prompt 11 unit tests cover overnight/day-boundary/override/overlap/no-mode resolution, persisted-session remaining-time arithmetic, session crediting, and allowed/always-allowed/hidden visibility. `./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1g :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeds: 95 tests, 0 failures/errors; lint reports 0 errors, 33 warnings, and 1 hint. Manual device checks remain: allowed-app behavior, scheduled activation, process-kill countdown/alarm/reopen completion, and Sleep rendering.
