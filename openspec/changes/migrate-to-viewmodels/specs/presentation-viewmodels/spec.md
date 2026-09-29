## ADDED Requirements

### Requirement: Main ViewModel exposes UI state

The system SHALL provide a `MainViewModel` that derives UI state from repository event flows and exposes it as `StateFlow`.

#### Scenario: Loading and error state derived from data events

- **WHEN** a `RequestStarted` event is received
- **THEN** `isLoading` SHALL become `true`
- **WHEN** a `DataReceived` event is received
- **THEN** `isLoading` SHALL become `false` and `hasError` SHALL reflect `DataReceived.failed`

#### Scenario: Current result retained on success

- **WHEN** a `DataReceived` event with `failed == true` is received
- **THEN** `currentResult` SHALL NOT be replaced
- **WHEN** a `DataReceived` event with `failed == false` is received
- **THEN** `currentResult` SHALL be updated to that event

#### Scenario: Event flows exposed

- **WHEN** a collector observes `dataEvents`, `locationEvents`, or `alertEvents`
- **THEN** each SHALL emit the latest event and subsequent events from the corresponding repository

### Requirement: Main ViewModel commands

The system SHALL expose `MainViewModel` commands that delegate to the data and location repositories.

#### Scenario: Data command delegation

- **WHEN** `updateData()`, `start()`, `stop()`, `restart()`, `startAnimation()`, `goRealtime()`, `setPosition()`, `toggleExtendedMode()`, `historySteps()`, `getParameters()`, `getIntervalDuration()`, or `isRealtime()` is invoked
- **THEN** the ViewModel SHALL delegate to `StrikeDataRepository`

#### Scenario: Background location delegation

- **WHEN** `enableBackgroundLocation()` or `disableBackgroundLocation()` is invoked
- **THEN** the ViewModel SHALL delegate to `LocationRepository`

#### Scenario: Data updates stopped on clear

- **WHEN** the ViewModel is cleared
- **THEN** it SHALL stop periodic data updates through the repository

### Requirement: Map ViewModel holds map state

The system SHALL provide a `MapViewModel` that holds map state as `StateFlow` and survives configuration changes.

#### Scenario: Map state updates

- **WHEN** `updateZoomLevel()`, `updateCenterPosition()`, `updateMapType()`, or `setMapReady()` is invoked
- **THEN** the corresponding `StateFlow` SHALL emit the new value

#### Scenario: Map state save and restore

- **WHEN** `saveMapState(zoom, center)` is invoked
- **THEN** `zoomLevel` and `centerPosition` SHALL be updated so a recreated screen can restore them

### Requirement: Settings ViewModel exposes preference access

The system SHALL provide a `SettingsViewModel` that observes `SharedPreferences` changes and exposes typed preference access.

#### Scenario: Preference change mapped to key

- **WHEN** a registered `SharedPreferences` key changes and matches a `PreferenceKey` entry
- **THEN** `preferenceChanged` SHALL emit the corresponding `PreferenceKey`
- **WHEN** the changed key does not match any `PreferenceKey`
- **THEN** the change SHALL be ignored without throwing

#### Scenario: Typed preference read and write

- **WHEN** `getStringPreference()`, `getIntPreference()`, `getBooleanPreference()`, or `getFloatPreference()` is invoked
- **THEN** the value SHALL be read from `SharedPreferences`, falling back to the provided default
- **WHEN** `setStringPreference()`, `setIntPreference()`, or `setBooleanPreference()` is invoked
- **THEN** the value SHALL be persisted to `SharedPreferences`

#### Scenario: Listener released on clear

- **WHEN** the ViewModel is cleared
- **THEN** it SHALL unregister its `SharedPreferences` change listener

### Requirement: Dagger-provided ViewModel factory

The system SHALL provide a `ViewModelFactory` that creates ViewModels using Dagger-injected providers registered in a `ViewModelModule`.

#### Scenario: Factory resolves known ViewModel

- **WHEN** `ViewModelFactory.create(MainViewModel::class.java)` is called
- **THEN** Dagger SHALL provide a `MainViewModel` with its repository dependencies injected

#### Scenario: Unknown ViewModel rejected

- **WHEN** `create()` is called with a class not registered in the map
- **THEN** the factory SHALL throw an `IllegalArgumentException` identifying the unknown class
