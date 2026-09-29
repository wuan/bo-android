## Why

The app currently wires its UI directly to stateful handlers (`MainDataHandler`, `LocationHandler`, `AlertHandler`) through callback-based `ConsumerContainer` registrations that must be manually paired in `onResume`/`onPause` (Main) and `onCreate`/`onDestroy` (AppService). This makes state lost on configuration changes, spreads registration/cleanup logic across lifecycle methods (with at least one known remove/re-add bug), and makes the UI hard to unit test. An earlier `viewmodel_migration` branch prepared ViewModel/repository infrastructure but has since fallen 134 commits behind `main`, so its intent is being re-established on the current codebase as a tracked change.

## What Changes

- Add a **repository layer** that wraps the existing handlers and exposes their callback events as Kotlin `Flow`s (`StrikeDataRepository`, `LocationRepository`, `AlertRepository`), plus a `ServiceStrikeDataRepository` for the background service path.
- Add **presentation ViewModels** (`MainViewModel`, `MapViewModel`, `SettingsViewModel`) exposing UI state and commands as `StateFlow`, with a Dagger-backed `ViewModelFactory` and `ViewModelModule`.
- Add the required runtime dependencies (`androidx.lifecycle:lifecycle-viewmodel-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`, `kotlinx-coroutines-android`).
- **Migrate** `Main`, `AppService`, `MapFragment`, `SettingsFragment`, and `HistoryController` from direct handler injection/callback registration to lifecycle-aware Flow collection and ViewModel commands.
- Fix the stale `disableDataUpdates()` removal bug in `Main` as part of the migration.
- Keep handler classes and their existing consumer contracts intact; repositories only adapt them.

## Capabilities

### New Capabilities
- `reactive-data-repositories`: Repository wrappers that convert handler `ConsumerContainer` callbacks into cold `Flow`s and forward data/location/alert commands, without changing handler behavior.
- `presentation-viewmodels`: Lifecycle-aware ViewModels exposing UI state (`isLoading`, `hasError`, current result, location/alert events, map state, preference changes) as `StateFlow`, wired through Dagger.
- `viewmodel-screen-integration`: Screens (`Main`, `AppService`, `MapFragment`, `SettingsFragment`, `HistoryController`) consume ViewModels via `repeatOnLifecycle` and no longer register handler consumers directly.

### Modified Capabilities
<!-- No existing specs in openspec/specs/; all capabilities are newly introduced. -->

## Impact

- **New packages**: `org.blitzortung.android.data.repository`, `org.blitzortung.android.location` (repository), `org.blitzortung.android.alert` (repository), `org.blitzortung.android.app.viewmodel`.
- **DI**: new `dagger/module/ViewModelModule.kt`; `AppComponent` gains the module.
- **Modified screens/controllers**: `app/Main.kt`, `app/AppService.kt`, `map/MapFragment.kt`, `settings/SettingsFragment.kt`, `app/controller/HistoryController.kt`, and call sites of `MainDataHandler`/`AlertHandler` (`app/MainPopupMenu.kt`, `dialogs/AlarmDialog.kt`).
- **Build**: `app/build.gradle.kts` adds lifecycle/coroutines dependencies.
- **Event types are not renamed**: current `main` uses `DataReceived`/`RequestStarted`/`StatusUpdate`/`NoData` and `Warning`, unlike the older branch's `ResultEvent`/`AlertEvent`.
- **Tests**: new unit tests for repositories and ViewModels; existing tests must keep passing.
- Handler public APIs (`requestUpdates`, `removeUpdates`, `updateData`, `start`/`stop`, etc.) remain and continue to back the alerts/background service.
