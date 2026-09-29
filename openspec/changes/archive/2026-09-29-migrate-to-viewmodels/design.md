## Context

The app follows a component-based architecture coordinated by Dagger 2. UI classes (`Main`, `AppService`) inject stateful handlers directly (`MainDataHandler`, `ServiceDataHandler`, `LocationHandler`, `AlertHandler`) and register callbacks against `ConsumerContainer`-based publishers. Registration and removal are manual and lifecycle-sensitive, which:

- loses UI state on configuration changes,
- duplicates wiring across `onResume`/`onPause` and `onCreate`/`onDestroy`,
- contains at least one defect where `Main.disableDataUpdates()` re-adds an alert consumer instead of removing it,
- resists unit testing.

An earlier `viewmodel_migration` branch (`dcc5a055`, `d78d50f0`) introduced a ViewModel/repository skeleton, but it targets an older codebase (`ResultEvent`/`AlertEvent`, `MainDataHandler` in a different package) and is 134 commits behind `origin/main`. The current `main` uses `DataReceived`/`RequestStarted`/`StatusUpdate`/`NoData` and `Warning`, and has a distinct `ServiceDataHandler`. This design re-establishes that intent against the current code.

Constraints:
- No new external services; keep handler contracts and the alerts/background behavior intact.
- Must fit the existing Dagger-Android injection setup.
- Must remain testable with JUnit4/AssertJ/MockK + `kotlinx-coroutines-test`/Turbine (already in test scope).

## Goals / Non-Goals

**Goals:**
- Introduce a repository layer that adapts existing handlers to cold `Flow`s without altering handler behavior.
- Introduce lifecycle-aware ViewModels exposing UI state (`StateFlow`) and commands for Main, Map, Settings, and the background service.
- Migrate `Main`, `AppService`, `MapFragment`, `SettingsFragment`, and `HistoryController` to consume ViewModels.
- Preserve, not change, observable app behavior (strike display, history, location, alerts).

**Non-Goals:**
- Rewriting handler internals, `ConsumerContainer`, or the data/alert algorithms.
- Renaming existing event types (`DataReceived`, `Warning`, etc.).
- Replacing Dagger with Hilt or another DI framework.
- Redesigning UI/UX or adding new user-facing features.
- Migrating `WidgetUpdateWorker` / `WidgetProvider` (handlers resolved directly) in this change.

## Decisions

### Decision: Repositories as thin adapters, not logic owners

`StrikeDataRepository`, `ServiceStrikeDataRepository`, `LocationRepository`, and `AlertRepository` wrap the existing handlers and only convert `ConsumerContainer` registrations into `callbackFlow` and forward commands. Business logic stays in handlers.

- **Why**: Smallest blast radius; handlers are shared with `AlertHandler`, `WidgetUpdateWorker`, and the notification path, which must keep working.
- **Alternative considered**: Move data-fetch/alert logic into repositories. Rejected: large, risky rewrite with no direct user benefit in this change.

### Decision: `callbackFlow` with `awaitClose` for event streams

Each repository registers exactly one consumer when a collector subscribes and removes it in `awaitClose`. Flows are cold; lifecycle-bound collectors drive registration.

- **Why**: Maps naturally onto `ConsumerContainer.requestUpdates`/`removeUpdates`, avoids leaking consumers, and makes cancellation explicit.
- **Consequence**: `ConsumerContainer` caches the last event, so a new collector receives the current event immediately — preserving existing behavior.

### Decision: ViewModels expose `StateFlow`, collectors use `repeatOnLifecycle`

`MainViewModel` holds `isLoading`/`hasError`/`currentResult`/`clearDataRequested` as `MutableStateFlow`, and re-exposes repository flows via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(...))`. Screens collect inside `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }`.

- **Why**: Standard lifecycle-aware pattern; state survives configuration changes; multiple UI consumers share one subscription.
- **Alternative considered**: Expose raw `Flow` directly to the UI. Rejected: loses the cached "current value" needed for late subscribers and status reconstruction.

### Decision: Dagger multibinding ViewModel factory

Add `ViewModelModule` with `@Binds @IntoMap @ViewModelKey(X::class)` bindings and a `ViewModelFactory` injecting `Map<Class<out ViewModel>, Provider<ViewModel>>`. Register the module on `AppComponent`.

- **Why**: Consistent with the existing Dagger setup; no Hilt migration.
- **Consequence**: Screens inject `ViewModelProvider.Factory` and create ViewModels via `by viewModels { factory }`.

### Decision: Target the current event model and service handler

The ported ViewModels/repositories use `DataReceived`, `RequestStarted`, `StatusUpdate`, `NoData`, and `Warning`, and add `ServiceStrikeDataRepository` over `ServiceDataHandler`. Names from the old branch are not reintroduced.

- **Why**: Compiles and matches `main`; avoids resurrecting removed types.

### Decision: No ViewModel layer existed; `MapFragment` gains injection

`MapFragment` currently has no Dagger injection and reads `SharedPreferences` directly. It will get `AndroidSupportInjection` plus `MapViewModel` while keeping its existing preference keys for map type/scale.

- **Why**: Required to centralize map state; also makes map state independent of `onPause` save timing.

### Decision: Migration order Settings → Map → HistoryController → Main → AppService

Each step is independently compilable; handlers remain until the last consumer is migrated.

- **Why**: Enables incremental verification and a clean rollback boundary.

## Risks / Trade-offs

- **Dual registration during transitional period** (handler consumer and repository consumer both active) → Migrate one screen at a time and never collect a repository flow from a screen that still registers its own consumers.
- **`MainViewModel` scoped per-Activity vs. per-Fragment** → Create it at the Activity and share via `activityViewModels` where a fragment needs the same instance (e.g., history); document scope in code.
- **`AppService` has no Activity lifecycle** → Use a `ServiceStrikeDataRepository` plus explicit `onDestroy` cleanup; do not rely on `repeatOnLifecycle`.
- **Flow backpressure/lifecycle race** → Use `SharingStarted.WhileSubscribed(5000)` and re-register consumers on each subscription; `ConsumerContainer` cache guarantees initial state.
- **Map state restore fighting user gestures** → Only apply restored zoom/center when they differ from the current map values (as in the reference docs).
- **New dependencies (lifecycle/coroutines)** → Pin to versions compatible with the existing Kotlin 2.2.10 / AGP setup and verify with Gradle dependency verification.
- **Behavior regression in strike overlay processing** → Port `Main`'s `DataReceived` handling verbatim into the observer, including `clearDataIfRequested()` and incremental vs. full update logic; cover with the existing tests plus new ViewModel tests.

## Migration Plan

1. Add lifecycle/coroutines dependencies and the `ViewModelModule`/`ViewModelFactory` scaffolding; wire the module into `AppComponent`.
2. Add repository classes and their unit tests (no UI changes yet).
3. Add `SettingsViewModel`, migrate `SettingsFragment`; run tests.
4. Add `MapViewModel`, migrate `MapFragment` (add injection); run tests.
5. Add `MainViewModel`; migrate `HistoryController` to depend on it; migrate `Main` to lifecycle-aware collection and remove manual consumer registration (fixing the `disableDataUpdates` defect by deletion).
6. Add `ServiceStrikeDataRepository` and migrate `AppService`.
7. Update tests/docs; run `./gradlew testDebugUnitTest` and `./gradlew lint`.

**Rollback**: Each step is a separate commit; revert the step to restore the previous direct-handler path. Handlers and their public APIs are untouched, so reverting a screen requires no handler changes.

## Open Questions

- Should `AppService` use a dedicated `ServiceViewModel` or call `ServiceStrikeDataRepository` directly from the service?
- Should `MainPopupMenu`/`AlarmDialog` (which take `MainDataHandler`/`AlertHandler`) be migrated in this change or left as a follow-up?
- Should the older `VIEWMODEL_MIGRATION_*.md` documents be deleted or updated to match the current event model?
- Is gaining precise map-state restoration worth adding `AndroidSupportInjection` to `MapFragment` now, or should map state stay on `SharedPreferences` initially?
