# Tasks

## 1. Shared framing math and units

- [x] 1.1 Add `MeasurementSystem.toMeters(value)` as the inverse of
  `calculateDistance`, and extend `MeasurementSystemTest` with metric and
  imperial round-trips; verify with
  `./gradlew :core:testDebugUnitTest --tests "org.blitzortung.android.util.MeasurementSystemTest"`.
- [x] 1.2 Add the exact Web-Mercator fit in `:core` — input span in metres,
  viewport pixels on the shorter side, and target latitude; return a fractional
  zoom and clamp the latitude to the Mercator limit. Cover known values,
  fractional results, higher-latitude framing, and the latitude clamp in
  `MapZoomTest`; verify with
  `./gradlew :core:testDebugUnitTest --tests "org.blitzortung.android.map.MapZoomTest"`.
- [x] 1.3 Add `AlertZoomTarget.framingMeters(warning, parameters)` implementing
  the outer range step, the closest-strike `*2.0` margin, the 50-unit minimum,
  the outer-step maximum, and the measurement-system conversion. Test no activity,
  closest strike, both bounds, an activity without a finite closest sector, and
  metric/imperial values in `AlertZoomTargetTest`; verify with
  `./gradlew :core:testDebugUnitTest --tests "org.blitzortung.android.alert.AlertZoomTargetTest"`.
- [x] 1.4 Move the `MapController.mCurrentAnimator` reflection accessor into one
  shared `MapCameraAnimator` helper in `:core`, make it return null instead of
  throwing when the field is unavailable, and make `OwnMapView.animator()`
  delegate to it. Test that the helper returns null while idle and a live
  animator between `animateTo` and its end on a laid-out `MapView`; verify with
  `./gradlew :core:testDebugUnitTest --tests "org.blitzortung.android.map.MapCameraAnimatorTest" :app:compileDebugKotlin`.

## 2. Phone adopts the shared framing

- [x] 2.1 Change `MapFragment.calculateTargetZoomLevel` to take a span in metres
  and the target latitude and delegate to `MapZoom`, and change
  `Main.animateToLocationAndVisibleSize` to accept metres and pass the framed
  latitude. Verify with `./gradlew :app:testDebugUnitTest`; manually check on a
  phone that alert-view and histogram framing use the tighter exact fit.
- [x] 2.2 Replace `Main.determineTargetZoomRadius` with
  `AlertZoomTarget.framingMeters(alertHandler.alertEvent, alertHandler.alertParameters)`
  (keeping the keep-zoom-on-goto-own-location bypass) and express the histogram
  spans in metres. Verify with `./gradlew :app:testDebugUnitTest` and manually
  check: no nearby strike frames the range, a nearby strike frames it with the
  doubled margin, and the keep-zoom preference leaves the zoom unchanged.

## 3. Car target span and viewport contract

- [x] 3.1 Change `CarMapRenderer.goToCurrentLocation` to take the target span in
  metres and update its KDoc; verify the interface and all call sites compile
  with `./gradlew :car:compileDebugKotlin`.
- [x] 3.2 In `BlitzortungMapScreen`, compute the span from
  `alertListState.warning` and `alertRepository.getAlertParameters()` at action
  press and pass it to the renderer; add screen tests for the alert-range and
  closest-strike spans and verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.BlitzortungMapScreenTest"`.
- [x] 3.3 Make `CarMapViewport` report `mapView.isAnimating` and the shared
  current animator instead of the hard-coded idle values; replace the
  "never animates" test with an idle → animating → idle test and verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.CarMapViewportTest"`.

## 4. Car camera transition protocol

- [x] 4.1 Add the camera-move start path: bump the generation, call
  `controller.stopAnimation(false)`, then
  `animateTo(GeoPoint(...), targetZoom, 500L)`; if the shared animator accessor
  returns no animator, apply the target camera immediately and settle instead of
  leaving the move unfinished. Test that the camera is still at the start
  immediately after the call, between start and target at half the duration, and
  at the target after it; verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.OsmdroidCarMapRendererTest"`.
- [x] 4.2 Add `settle()` (store the camera and notify the viewport listener) and
  invoke it once from the posted animation end, guarded by the generation. Test
  that nothing is stored or notified at the tap and exactly one notification with
  the final camera arrives after the end; verify with the same renderer test
  class.
- [x] 4.3 Interrupt the transition from `onScroll`, `onFling`, `onScale`,
  `zoomIn`, and `zoomOut`: bump the generation, `stopAnimation(false)`, apply the
  driver input, then settle. Test panning and the zoom action mid-flight leave
  the camera where the input left it (no snap to target) and store that camera;
  verify with the same renderer test class.
- [x] 4.4 Retarget on a repeated location action: cancel without settling, start a
  new move from the current camera, and settle only the new move. Test two
  actions without idling produce the second target with one store/notification;
  verify with the same renderer test class.
- [x] 4.5 Cancel on `release()` without settling, and no-op when the map has no
  size. Test that advancing the clock after a release during a move produces no
  store or viewport notification; verify with the same renderer test class.

## 5. Integration verification

- [x] 5.1 Run the affected unit suites together —
  `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest :car:testDebugUnitTest`
  — and confirm they pass.
- [ ] 5.2 On the Desktop Head Unit and an AAOS emulator, verify the location
  action visibly animates on the map surface, pan/zoom interrupts it, data
  refreshes after it settles, and the settled camera is restored after a restart.
- [ ] 5.3 On a phone, verify the alert-view and histogram zooms frame the
  requested distances in both metric and imperial, and that the keep-zoom
  preference still bypasses the own-location target.

## 6. Car manual location precedence

- [x] 6.1 Make `CarLocationProvider` return the manual provider when the stored
  mode is manual and both coordinates are valid; otherwise keep the GPS → network
  → passive selection.
- [x] 6.2 Cover manual-over-GPS, manual-without-coordinates, and the existing
  provider fallbacks in `CarLocationProviderTest`; verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.CarLocationProviderTest"`.

## 7. Car alert status line

- [x] 7.1 Make `CarAlertListPresenter` return only the closest activity as one
  status row, removing the per-sector rows and the strike-count string.
- [x] 7.2 Update the presenter and screen tests; verify with
  `./gradlew :car:testDebugUnitTest --tests "org.blitzortung.android.car.CarAlertListPresenterTest" --tests "org.blitzortung.android.car.BlitzortungMapScreenTest"`.

## 8. Histogram zoom-out

- [x] 8.1 Make the histogram tap frame the data area first, then the global view
  once the data-area framing is already active, and cover global results.
- [x] 8.2 Add `HistogramZoomTest` for the data-area, global, fixed-region, and
  zoomed-in cases; verify with
  `./gradlew :app:testDebugUnitTest --tests "org.blitzortung.android.map.HistogramZoomTest"`.

## Workflow follow-up

- Archive the change after review and verification.
- Re-check the archived `map-zoom-framing` and `car-integration` specs for
  coherent requirement text after archive.
