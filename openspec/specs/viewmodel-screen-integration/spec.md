### Requirement: Main screen collects ViewModel state lifecycle-aware

`Main` SHALL obtain its data, location, and alert state from `MainViewModel` and SHALL NOT register handler consumers directly.

#### Scenario: Collectors follow lifecycle

- **WHEN** `Main` reaches the `STARTED` state
- **THEN** it SHALL collect `MainViewModel` flows within `repeatOnLifecycle(Lifecycle.State.STARTED)`
- **WHEN** `Main` moves below `STARTED`
- **THEN** collection SHALL stop and underlying repository consumers SHALL be released

#### Scenario: UI reacts to ViewModel state

- **WHEN** `isLoading`, `hasError`, `currentResult`, `locationEvents`, or `alertEvents` emits
- **THEN** `Main` SHALL update the corresponding UI (status progress, error indication, strike overlay, location and alert views)

#### Scenario: No manual handler subscriptions remain

- **WHEN** `Main` is inspected
- **THEN** it SHALL NOT contain manual `requestUpdates`/`removeUpdates` calls against `MainDataHandler`, `LocationHandler`, or `AlertHandler`

#### Scenario: Data result processing unchanged

- **WHEN** a `DataReceived` result is observed with a changed parameter set
- **THEN** the strike overlay SHALL be reinitialized and data cleared as before
- **WHEN** a `DataReceived` result is observed with an incremental update
- **THEN** only new strikes SHALL be appended and the time slider/history updated

### Requirement: Background service uses the service repository

`AppService` SHALL obtain background data and alert wiring from a ViewModel/repository layer and SHALL release resources when destroyed.

#### Scenario: Service observes through repository

- **WHEN** `AppService` is created
- **THEN** it SHALL subscribe to service data events through `ServiceStrikeDataRepository` and forward alerts through the alert layer without directly registering `MainDataHandler`/`ServiceDataHandler` consumers in lifecycle methods

#### Scenario: Service cleanup

- **WHEN** `AppService` is destroyed
- **THEN** it SHALL unregister its preference listener and release location/data subscriptions

### Requirement: Map screen uses MapViewModel

`MapFragment` SHALL persist and restore map state through `MapViewModel` instead of ad-hoc `SharedPreferences` access alone.

#### Scenario: Restore map state

- **WHEN** `MapFragment` is created with available ViewModel state
- **THEN** it SHALL restore zoom level and center position from `MapViewModel`

#### Scenario: Save map state

- **WHEN** map zoom or scroll occurs or the fragment pauses
- **THEN** `MapFragment` SHALL update `MapViewModel` with the current zoom level and center position

### Requirement: Settings screen uses SettingsViewModel

`SettingsFragment` SHALL use `SettingsViewModel` for typed preference reads/writes and preference-change handling.

#### Scenario: Preference change handled via ViewModel

- **WHEN** a preference changes while `SettingsFragment` is started
- **THEN** it SHALL receive the mapped `PreferenceKey` from `SettingsViewModel` and update dependent UI/preferences

### Requirement: History controller depends on ViewModel

`HistoryController` SHALL consume data events from `MainViewModel` rather than a raw `MainDataHandler`.

#### Scenario: History reacts to data and commands

- **WHEN** a data event is observed
- **THEN** `HistoryController` SHALL update history controls
- **WHEN** the user changes history position or returns to realtime
- **THEN** `HistoryController` SHALL invoke the corresponding `MainViewModel` command

### Requirement: Migration preserves existing behavior

The migration SHALL NOT change the observable behavior of strike display, history playback, location tracking, or alerts.

#### Scenario: Existing tests pass

- **WHEN** the unit test suite is executed
- **THEN** all pre-existing tests SHALL pass, and new repository/ViewModel tests SHALL cover the added behavior
