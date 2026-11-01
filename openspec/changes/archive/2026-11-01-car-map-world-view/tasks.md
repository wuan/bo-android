# Tasks

## 1. Camera persistence in car settings

- [x] 1.1 Add centre latitude, centre longitude, and zoom keys plus read/write (and a "camera stored" check) to `CarSettings`, keeping them in the car-only `car_settings` file; verify with unit tests that absent values read as "no stored camera" and that written values round-trip
- [x] 1.2 Verify the new keys are not written to or read from the phone app's default preferences; assert with a test that only `CarSettings`' file is touched

## 2. World view and visible-world bounds

- [x] 2.1 Compute the cover world-view zoom from the surface dimensions (maximum of the fit-width and fit-height zooms using osmdroid's tile size/scale) and apply it with centre `0°/0°` in `onSurfaceAvailable`; verify with a test that the resulting zoom makes the world at least as large as the viewport and that the centre is `0°/0°`
- [x] 2.2 Set the map minimum zoom to the world-view zoom for the session; verify with tests that zooming out below it is clamped and that `canZoomOut()` is false at the floor
- [x] 2.3 Set the scrollable area limit to the world box (`±85.051129°`, `±180°`); verify with a test that panning beyond an edge is clamped back to the bounds
- [x] 2.4 Recompute the world view and floor on each `onSurfaceAvailable` with different dimensions; verify with a test that a wider surface yields a higher world-view zoom

## 3. Camera restore and framing interaction

- [x] 3.1 Restore a stored camera in `onSurfaceAvailable` in preference to the world view and clamp the restored zoom up to the current floor; verify with tests that a stored camera is restored, and that a below-floor stored zoom is raised to the floor
- [x] 3.2 Treat a restored camera as an established view and suppress first-fix framing; verify with a test that a location fix arriving after a restore keeps the restored centre and zoom
- [x] 3.3 Save the camera whenever the driver navigates (scroll, fling, scale, action-strip zoom in/out); verify with tests that each navigation updates the stored centre/zoom

## 4. Integration verification

- [x] 4.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 4.2 On the Android Automotive OS emulator verify a fresh run with no stored camera and no location shows the whole world covering the screen (no empty margins, centre 0°/0°), that zoom-out and panning stop at the world bounds, and that after panning/zooming and restarting the app the last view is restored

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and confirm the `car-integration` spec reflects the world-view fallback, the visible-world bounds, and camera persistence.
