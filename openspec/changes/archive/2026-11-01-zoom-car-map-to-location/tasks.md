# Tasks

## 1. Initial framing in the car map renderer

- [x] 1.1 Add one-shot initial framing to `OsmdroidCarMapRenderer.onLocationEvent`: on a `LocationUpdate`, when no framing has been applied yet, centre the map on the fix and set the car default zoom; verify with unit tests that the resulting centre matches the fix and the zoom is the default
- [x] 1.2 Track user interaction from the gesture and zoom entry points (`onScroll`, `onFling`, `onScale`, `zoomIn`, `zoomOut`) and suppress framing once the user has moved the map; verify with unit tests for "interaction before the first fix" and "later fixes do not reframe"
- [x] 1.3 Keep the marker path unchanged: verify `NoLocation` and subsequent `LocationUpdate`s update the own-location marker only and leave the camera centre and zoom untouched, with unit tests for both cases
- [x] 1.4 Name and document the car default zoom constant and its rationale in the renderer, and verify `./gradlew :car:testDebugUnitTest` passes

## 2. Integration verification

- [x] 2.1 Run `./gradlew testDebugUnitTest` across the modules and verify all new and existing unit tests pass
- [x] 2.2 Verify on the Desktop Head Unit (Android Auto) and/or the Android Automotive OS emulator that the map opens centred and zoomed on the current position, that later location updates only move the marker, and that a manual pan or zoom is preserved

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and confirm the `car-integration` spec reflects the initial-framing behaviour.
