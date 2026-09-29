## ADDED Requirements

### Requirement: Data events exposed as a Flow

The system SHALL provide a `StrikeDataRepository` that exposes `MainDataHandler` notifications as a cold Kotlin `Flow<DataEvent>`.

#### Scenario: Observer receives forwarded events

- **WHEN** a collector subscribes to `StrikeDataRepository.observeDataEvents()`
- **THEN** the repository SHALL register exactly one consumer with `MainDataHandler` and forward every `DataEvent` (`RequestStarted`, `DataReceived`, `StatusUpdate`, `NoData`) to the collector

#### Scenario: Registration is released on cancellation

- **WHEN** the collector's coroutine is cancelled or the flow is no longer collected
- **THEN** the repository SHALL remove the consumer from `MainDataHandler` so no reference is retained

### Requirement: Data commands delegated to the handler

The system SHALL expose the current `MainDataHandler` data operations through `StrikeDataRepository` without changing their semantics.

#### Scenario: Command forwarding

- **WHEN** a caller invokes `updateData()`, `start()`, `stop()`, `restart()`, `startAnimation()`, `goRealtime()`, `setPosition(position)`, `historySteps()`, or `toggleExtendedMode()` on the repository
- **THEN** the repository SHALL invoke the corresponding `MainDataHandler` operation and return its result where applicable

#### Scenario: Parameters and state reads

- **WHEN** a caller reads `getParameters()`, `getIntervalDuration()`, `isRealtime()`, or `calculateTotalCacheSize()`
- **THEN** the repository SHALL return the value obtained from `MainDataHandler`

### Requirement: Service data events exposed as a Flow

The system SHALL provide a `ServiceStrikeDataRepository` that wraps `ServiceDataHandler` for the background service path with the same Flow and command contract used by the foreground repository.

#### Scenario: Service observer receives events

- **WHEN** `AppService` collects `ServiceStrikeDataRepository.observeDataEvents()`
- **THEN** the repository SHALL register a single consumer with `ServiceDataHandler` and forward events until collection ends

### Requirement: Location events and commands exposed reactively

The system SHALL provide a `LocationRepository` that exposes `LocationHandler` notifications as a `Flow<LocationEvent>` and forwards background-mode commands.

#### Scenario: Location event forwarding

- **WHEN** a collector subscribes to `LocationRepository.observeLocationEvents()`
- **THEN** the repository SHALL register a consumer with `LocationHandler` and forward every `LocationEvent` (`LocationUpdate`, `NoLocation`) until collection ends

#### Scenario: Background location control

- **WHEN** `enableBackgroundMode()` or `disableBackgroundMode()` is invoked
- **THEN** the repository SHALL delegate to `LocationHandler.enableBackgroundMode()` / `disableBackgroundMode()`

#### Scenario: Current location read

- **WHEN** `getCurrentLocation()` is invoked
- **THEN** the repository SHALL return `LocationHandler.location`, which MAY be null

### Requirement: Alert events and parameters exposed reactively

The system SHALL provide an `AlertRepository` that exposes `AlertHandler` warnings as a `Flow<Warning>` and forwards alert parameter reads.

#### Scenario: Alert warning forwarding

- **WHEN** a collector subscribes to `AlertRepository.observeAlertEvents()`
- **THEN** the repository SHALL register a consumer with `AlertHandler` and forward every `Warning` until collection ends

#### Scenario: Alert parameters read

- **WHEN** `getAlertParameters()` is invoked
- **THEN** the repository SHALL return the current `AlertParameters` from `AlertHandler`
