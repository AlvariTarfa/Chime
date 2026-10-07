# Architecture

## Package layout

The application package is `com.savatech.chimelauncher`.

```text
core/
  di/          Hilt modules and dependency bindings
  theme/       Compose Material 3 colors, typography, and app theme
  navigation/  Route identifiers and the single-activity NavHost
  util/        Small shared utilities
data/
  apps/        Installed-app data sources and models
  usage/       Usage-statistics data sources and models
  goals/       Goal persistence and repositories
  settings/    Preference persistence and repositories
  backup/      Versioned local JSON backups and privacy data deletion
domain/        Domain models, repository contracts, and use cases
  backup/      Serializable backup schema, codec, and import errors
feature/
  home/        Launcher home UI and its state holders
  drawer/      App drawer UI and its state holders
  goals/       Goal UI and its state holders
  intercept/   Mindful-intercept UI and its state holders
  focus/       Focus-mode UI and its state holders
  insights/    Usage insights UI and its state holders
  checkin/     Daily check-in UI and its state holders
  onboarding/  First-run UI and its state holders
  settings/    Settings UI and its state holders
service/       Android services and their platform integration
```

## MVVM rules

- A feature owns its screens and ViewModels. ViewModels expose immutable `StateFlow` UI state; composables collect flows with `collectAsStateWithLifecycle`.
- Keep UI rendering and user interaction in Compose. Put business decisions in domain use cases and persistence/platform access behind data repositories.
- Do not perform disk, database, or `PackageManager` work on the main thread. Inject dispatchers and use coroutines for blocking or asynchronous data work.
- Put user-visible text in Android string resources.

## Navigation

`MainActivity` is the only activity. It installs the app theme and hosts `AppNavHost`; `Routes` owns the route identifiers. Add screen destinations to `AppNavHost` and keep navigation calls at the UI boundary. The home route consumes Back so the launcher does not exit.

## Dependency injection

Use Hilt. Put application-wide bindings in `core/di`; bind data implementations to domain contracts there. Annotate the application and entry-point Android components with Hilt integration. Use KSP for Hilt and Room processors.

## Where things go

- Android or Compose styling: `core/theme`; activity-wide destinations: `core/navigation`.
- Reusable, platform-independent behavior: `core/util` only when it is genuinely shared.
- Database, preferences, usage access, or app-list implementation: matching package under `data/`.
- Local backup/export, import validation, and privacy deletion: `data/backup`; the Room snapshot and restore each use a database transaction. JSON export is user-selected through SAF only; there is no cloud sync.
- Backup schema and versioned JSON handling: `domain/backup`. Notification digest content and temporary app grants are intentionally excluded from backup files.
- Reusable business rules: `domain/`.
- Screen-specific UI state and composables: the matching `feature/<name>/`.
- Lifecycle-managed Android service integration: `service/`. The foreground accessibility watcher is configured only for window-state changes, does not retrieve window content or filter key events, and bases interception only on event package names. Its Compose pause surface uses a `TYPE_ACCESSIBILITY_OVERLAY` with explicit view-tree lifecycle, ViewModel-store, and saved-state owners.

## Room schema and migrations

The committed schema at `app/schemas/com.savatech.chimelauncher.data.db.AppDatabase/1.json` is the frozen version-1 schema. Every future entity or schema change must include a Room `Migration` and a migration test that verifies existing data is preserved; do not use destructive migration fallbacks.
