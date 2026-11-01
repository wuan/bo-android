# Tasks

## 1. Compact header countdown

- [x] 1.1 Keep the reported period alongside the remaining seconds in `CarAlertListState`/`reduce`, and verify with reducer tests that a `StatusUpdate("40/60")` yields 40 remaining of a 60-second period and that a malformed status yields no countdown
- [x] 1.2 Compose the map header title as the app name alone, or `<app name> · <remaining> / <period>s` when a countdown is known, using a new `car_status_next_refresh` format; verify with a screen test that a reported status produces the compact one-line title and that no status produces just the app name
- [x] 1.3 Remove the countdown row from `CarAlertListPresenter`/`CarAlertStrings` and update the presenter tests so the first row is always an alert row (no countdown row between the header and the alerts)

## 2. Rename the car display name

- [x] 2.1 Set `car_app_name` and the Android Automotive OS `app_name` to "Blitzortung Lightning"; verify the header uses the new name and the phone-label string is unchanged

## 3. Integration verification

- [x] 3.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 3.2 On the Android Automotive OS emulator verify the header shows "Blitzortung Lightning · x / ys" on one line, the alert rows immediately follow without an empty status line, and the AAOS launcher label reads "Blitzortung Lightning"

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and confirm the `car-integration` spec reflects the header countdown.
