# Tasks

## 1. Drive the car alert computation from received data

- [x] 1.1 Add `onDataEvent(event: DataEvent)` to `AlertRepository` that delegates to `AlertHandler.dataEventConsumer`, and verify with a unit test that the handler receives the event
- [x] 1.2 Extract `BlitzortungMapScreen`'s data-event handling into a `handleDataEvent(event)` method that forwards each event to `alertRepository.onDataEvent`, reduces the alert-list state, updates the renderer for a successful `DataReceived`, and invalidates, then call it from the collector; verify with a screen test using fake car dependencies that a `DataReceived` is forwarded to the alert repository and sets the state to loaded
- [x] 1.3 Show a "no current alert" label (instead of the waiting label) for a local activity with no closest strike, so a loaded-but-quiet list does not look like loading; verify with a presenter test

## 2. Refresh countdown

- [x] 2.1 Add `refreshSecondsRemaining` to `CarAlertListState` with a reducer that maps `StatusUpdate("<remaining>/<period>")` to the remaining seconds (and to null when absent or unparsable), and verify with reducer unit tests for a valid, a malformed, and a non-status event
- [x] 2.2 Render the countdown as the first alert-list row using a new `CarAlertStrings.nextRefreshFormat` (for example "Next update in %d s"), shown when a status has been reported, and verify with presenter tests for the present and absent countdown

## 3. Integration verification

- [x] 3.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 3.2 On the Android Automotive OS emulator, verify the alert list leaves its waiting state once data has arrived (shows the closest strike, "no current alert", "no strikes in range", or "location unknown") and shows a live countdown that decreases each second

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and confirm the `car-integration` spec reflects the alert-list data wiring and the refresh countdown.
