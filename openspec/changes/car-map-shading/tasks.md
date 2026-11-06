# Tasks

## 1. Core colour-handler target override

- [x] 1.1 Add an optional target override to `ColorHandler` that is resolved as
  `override ?: preferenceTarget` inside `updateTarget()`, so a later
  `updateTarget()` call cannot discard it; verify with a core unit test that sets
  an override, calls `updateTarget()`, and still reads the overridden palette.
- [x] 1.2 Document on `ColorTarget.SATELLITE` / `STREETMAP` that they mean the
  dark- and bright-shading palettes; verify the existing colour-handler tests still
  pass with `./gradlew :core:testDebugUnitTest`.

## 2. Car shading model and preference

- [x] 2.1 Replace `MapDimMode` with `MapShadingMode { FOLLOW_CAR, DARK, BRIGHT }`
  plus a `MapShading { DARK, BRIGHT }` resolution for a host theme; verify with
  unit tests covering all three modes in both themes and asserting there is no
  unshaded result.
- [x] 2.2 Rename the `CarSettings` preference to `map_shading_mode` (property
  `mapShadingMode`) and delete the old key constant; verify with unit tests for the
  Follow car default, round-trip persistence, and that the phone's default
  preferences remain untouched.

## 3. Two-direction map scrim

- [x] 3.1 Rename `MapDimOverlay` to `MapShadeOverlay` and replace its black-only
  paint with a colour plus alpha (black 128 for dark, white 140 for bright); verify
  with unit tests that both directions draw with the expected colour and alpha and
  that alpha 0 still draws nothing.
- [x] 3.2 Update the renderer's overlay registration and comments so the scrim
  stays at `overlays[0]` beneath strikes, location, and the legend; verify the
  existing overlay-order renderer tests still pass.

## 4. Renderer shading and strike palette

- [x] 4.1 Replace `CarMapRenderer.setDimmed(Boolean)` with
  `setShading(MapShading)`; verify the interface and all call sites compile with
  `./gradlew :car:compileDebugKotlin`.
- [x] 4.2 Make the renderer apply dark shading with the bright palette
  (`ColorTarget.SATELLITE`) and bright shading with the darker palette
  (`ColorTarget.STREETMAP`) through the handler override before refreshing
  strikes; verify with renderer tests asserting the scrim colour, the active
  palette, and that a shading change recolours existing strikes.
- [x] 4.3 Re-apply the stored shading whenever strikes are updated, so the first
  data event after a cold start or theme flip uses the correct palette; verify with
  a renderer test that feeds strikes before and after a shading change and checks
  the resulting strike colours.

## 5. Car screens and strings

- [x] 5.1 Rename the map screen's shading state helper (`isMapDimmed` →
  `currentShading`) and pass the resolved shading to the renderer; verify with
  screen tests for Follow car in both themes and the two forced modes.
- [x] 5.2 Update the settings screens and string resources to the shading
  vocabulary (`car_settings_map_shading`, `car_shade_follow_car`, `car_shade_dark`,
  `car_shade_bright`), including the selected-row marker; verify with settings
  screen tests that the three modes are listed and selection persists.
- [x] 5.3 Remove the old dimming strings and any remaining `dim`/`off` references
  in `:car`; verify with `grep -ri "dimming\|dimmed\|car_dim" car/src` returning no
  hits.

## 6. Car dependency wiring

- [x] 6.1 Give the car its own `StrikeColorHandler` instance over the shared
  preferences in `AppCarDependencies` and `AutomotiveCarDependencies`, instead of
  reusing the phone singleton; verify with dependency-provider tests that the car
  handler is a distinct instance and that the phone handler's target is unaffected
  by car shading.

## 7. Integration verification

- [x] 7.1 Run `./gradlew testDebugUnitTest` for the affected modules and fix any
  regressions.
- [x] 7.2 Run `./gradlew lint` and resolve new findings in `:car`/`:core`.
- [x] 7.3 Verify the rendering on the Android Automotive OS emulator: dark theme
  shows a dark map with bright strikes, light theme shows a pale map with darker
  strikes, and a theme switch updates both without restarting the app.

## Workflow follow-up

- Archive the change after review and emulator verification.
