# Tasks

## 1. Data timestamp status line

- [x] 1.1 Add the `Data %1$s` status-line string and the two age-label strings
  (`0 min` and `-N min`) to the car resources, leaving the countdown string in
  place until its last use is removed; verify with
  `./gradlew :car:processDebugResources`.
- [x] 1.2 Rename the renderer's countdown API to `setDataTime`, rename the legend
  field to `dataTime`, and draw the timestamp as the panel's status line in the
  legend font;
  verify with `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.OsmdroidCarMapRendererTest" --tests "org.blitzortung.android.car.CarLegendOverlayTest"`.
- [x] 1.3 In `BlitzortungMapScreen`, format `DataReceived.referenceTime` as fixed
  24-hour `HH:mm:ss` on each successful result and push it to the renderer; add
  screen tests for the formatted timestamp, a newer result replacing it, a failed
  result keeping it, and no data hiding the line; verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.BlitzortungMapScreenTest"`.

## 2. Remove the countdown path

- [x] 2.1 Delete `CarRefreshCountdown`, the `refreshCountdown` field, the
  `StatusUpdate` branch of `CarAlertListState.reduce()`, and the screen's
  `buildCountdownText`/`applyCountdown`/`countdownText` members; remove the
  `car_status_next_refresh` string and update the reducer, presenter, and screen
  tests accordingly; verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.CarAlertListReducerTest" --tests "org.blitzortung.android.car.CarAlertListPresenterTest" --tests "org.blitzortung.android.car.BlitzortungMapScreenTest"`.
- [x] 2.2 Confirm a `StatusUpdate` no longer changes car alert-list state or
  invalidates the host template; verify by testing that reducing a
  `StatusUpdate` leaves the state equal and that the map-screen template is
  unchanged when only status events arrive.

## 3. Continuous legend band

- [x] 3.1 Replace the per-colour `LegendEntry` rows with a band model derived
  from the active palette and interval (band colours, three label values); add
  tests for 60- and 30-minute intervals, the `0 min` top label, the
  half-interval middle label, and the full-interval bottom label; verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.CarLegendOverlayTest"`.
- [x] 3.2 Draw the vertical band with a `LinearGradient` from the active palette,
  newest at the top, and reserve half a text line of overhang so the top and
  bottom labels stay inside the panel; add tests that the band height follows
  the colour count and that the panel grows/shrinks around the three labels;
  verify with the same legend test class.
- [x] 3.3 Keep the band one unbroken gradient with no separator lines and cache
  the shader until the palette, band bounds, or density changes; add tests for
  shader reuse and a density-driven rebuild; verify with the same legend test
  class.

## 4. Integration verification

- [x] 4.1 Run the car unit suite and lint —
  `./gradlew :car:testDebugUnitTest :car:lintDebug` — and confirm both pass.
- [x] 4.2 Run the phone and core unit suites to confirm the shared palette and
  `DataReceived` contract were not disturbed —
  `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest`.
- [x] 4.3 On the Desktop Head Unit and an AAOS emulator, verify the legend shows
  `Data HH:MM:SS`, a smooth band with three labels and no separator lines,
  follows a colour-scheme and interval change, stays legible with dimming on, and
  does not redraw once per second without new data.

## Workflow follow-up

- Archive the change after review and device verification are complete.
