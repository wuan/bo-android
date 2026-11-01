# Tasks

## 1. Shared data and overlay API (`:core`)

- [x] 1.1 Add `StrikeDataRepository.updateViewport(viewport: MapViewport)` that forwards to `MainDataHandler.updateGrid` with auto grid sizing enabled and the region pinned to global; verify with a unit test that an update after a high-zoom viewport yields an auto grid size and never a fixed region
- [x] 1.2 Add a `showDataArea` option to `StrikeListOverlay` (default `true`, keeping the phone behaviour) that suppresses the local data-area outline; verify with a unit test that the outline is skipped when the option is off and still drawn when it is on

## 2. Car map viewport wiring (`:car`)

- [x] 2.1 Add a car-side `MapViewport` adapter over the car `MapView` reporting the zoom level and bounding box, with `viewportIsAnimating = false` and `animator() = null`; verify with a unit test that the adapter reflects the map's zoom and bounding box
- [x] 2.2 Push the viewport from `OsmdroidCarMapRenderer` after every camera change (`onScroll`, `onFling`, `onScale`, `zoomIn`, `zoomOut`, first-fix framing, restored/world camera) and from an `addOnLayoutChangeListener`; verify with a unit test that a programmatic scroll and a zoom each invoke the listener
- [x] 2.3 Create the renderer's `StrikeListOverlay` with the data-area outline disabled; verify with a renderer unit test that the car overlay does not draw the data-area outline

## 3. Car screen wiring (`:car`)

- [x] 3.1 Wire `BlitzortungMapScreen` so the renderer's viewport listener reports to `strikeDataRepository.updateViewport`; verify with a screen unit test that a renderer camera change reaches the repository
- [x] 3.2 Defer the initial `updateData()` in `BlitzortungMapScreen` until the first viewport notification; verify with a screen unit test that no data request is issued before the first viewport update and that one is issued after it

## 4. Integration verification

- [x] 4.1 Run `./gradlew :car:testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 4.2 On the Android Automotive OS emulator verify that a wide view requests global grid data with ≥25 km squares, zooming in switches to local `<25 km` data, panning retargets it, no data-area outline is drawn, and no strike request is issued before the map is laid out
- [x] 4.3 Confirm the phone experience is unchanged: run `./gradlew :app:testDebugUnitTest` and verify the phone's data-area outline and region/grid-size behaviour still pass

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Confirm the `car-integration` spec reflects viewport-derived grid and global/local data.
