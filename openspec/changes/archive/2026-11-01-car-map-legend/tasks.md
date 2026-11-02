# Tasks

## 1. Car legend overlay

- [x] 1.1 Add `CarLegendOverlay` in `:car` as an osmdroid `Overlay` that draws a dark translucent panel with one row per colour band (swatch + age label) in the top-left, reading colours from a `StrikeColorHandler` and the duration from `Parameters.intervalDuration`, with metrics scaled from the surface dpi; verify with unit tests that the rows match the number of colours and the `<`/`>` age labels for a given interval
- [x] 1.2 Add the `car_legend_*` string(s) to `car/src/main/res/values/strings.xml` (English only)
- [x] 1.3 Register the overlay in `OsmdroidCarMapRenderer` after the strike and own-location overlays so it draws above the dim scrim; verify with a renderer test that the legend overlay is present and ordered last and that dimming leaves it undimmed
- [x] 1.4 Anchor the legend to the host's unobscured map area: forward `SurfaceCallback.onVisibleAreaChanged` / `onStableAreaChanged` through `CarMapSurfaceCallback` and `CarMapRenderer` to `CarLegendOverlay`, which draws at the top-left of the stable area (falling back to the visible area, then the raw surface corner); verify with unit tests that the panel is placed inside the reported area, that an empty area is ignored, and that the callback forwards both areas to the renderer

## 2. Integration verification

- [x] 2.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 2.2 On the Android Automotive OS emulator verify the legend shows in the top-left of the map's visible area with the correct colours and age labels, stays legible when the map is dimmed, updates when the interval or colour scheme changes, and does not collide with the content list

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and confirm the `car-integration` spec reflects the car map legend.
