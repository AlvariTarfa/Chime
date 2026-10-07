# Chime Privacy Policy

**Last reviewed: October 7, 2026**

Chime is an Android launcher for goals, focus, and mindful app use. Chime does not require an account and does not send app data to a Chime server. The app does not include analytics, advertising, or crash-reporting services. Information described below is stored or processed on your device unless you choose to export a file using Android's document picker.

## Information stored on this device

Chime stores goals, tasks, daily priorities, task logs, check-ins, app classifications and limits, focus modes and sessions, and records of pause/limit decisions in its private app database. Settings such as appearance, schedules, allow-lists, and notification preferences are stored in local app preferences.

If you grant Usage Access, Chime reads Android usage events to calculate app foreground time, trends, and (on supported Android versions) pickup-related metrics. It uses those results for insights and app/category limit decisions. Usage events are queried on-device; Chime does not send them to a server or retain a separate raw usage-event history.

If you enable the notification digest, Android grants Chime's notification listener access to notification objects so it can decide which notifications qualify. Chime excludes calls, alarms, media and other ineligible notifications, allow-listed apps, and notifications not accepted by the configured digest rules. For accepted notifications from apps classified as Distracting, Chime stores the originating package, title, text, and time in the local database. Delivered digest items are pruned after seven days. A notification removed from the Android shade cannot be restored by Chime.

If you enable the accessibility pause service, Chime observes only window-state-change events and uses the event's package name to evaluate whether a pause is needed. The service does not request window-content access or key-event filtering. Package names and outcomes can be recorded in Chime's local pause history. The service can be disabled in Android Accessibility settings.

Chime also reads installed launcher-app and icon-pack metadata to show your app drawer. This metadata is used on-device.

## Permissions and optional access

- **Notifications:** Android 13 and later ask permission before Chime can post reminders, session/grant alerts, and digest summaries. You can deny or later revoke it; scheduled work may still run without a visible notification.
- **Usage Access:** enable this special access in Android Settings only if you want usage insights and app/category limit enforcement.
- **Notification listener access:** enable this special access only if you want the notification digest. The Android system warns that a listener can inspect notifications; Chime's filtering and local storage behavior are described above. Revoke access in Android Settings or turn off batching in Chime.
- **Accessibility service:** enable this special access only if you want pauses when opening apps outside Chime. Chime's disclosure explains its package-name-only use. Disable the service in Android Accessibility settings.

The access above is optional. Chime can still be used as a launcher with goals and local settings without enabling Usage Access, notification listening, or accessibility pauses.

## Backup, export, and sharing

Chime has no cloud sync. Android cloud backup and device-transfer rules exclude Chime's Room database and DataStore preferences. You can explicitly export a JSON backup to a location you choose; the file contains goals, tasks/logs, priorities, app settings, focus records, check-ins, intercept records, and supported typed settings. It deliberately excludes notification digest content and temporary app grants. The backup is a user-managed file; protect it as you would other personal files and delete any copies you no longer want.

Insights can be exported as CSV using Android's document picker. Chime writes the file to the destination you select. Chime does not upload either export.

## Deleting information

Use **Settings → Data and privacy → Delete all data** and confirm the requested text to delete Chime's local database and preferences, cancel scheduled work and alarms, and clear Chime's posted notifications. This does not delete JSON or CSV files you previously exported, copies stored by a cloud/document provider, or Android's own usage history. Delete those files through the provider or device if you no longer want them.

Uninstalling Chime removes its private app data, but does not remove user-managed exports or Android's usage history.

## Changes and contact

This policy must be updated if Chime begins transmitting data, changes how special access is used, or adds a new data category. For privacy questions, contact the Chime publisher through the contact details shown on the app's Play Store listing.
