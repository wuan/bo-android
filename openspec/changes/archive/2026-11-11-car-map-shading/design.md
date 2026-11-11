# Design

## Context

See `proposal.md` — Why. The current state that shapes the approach:

- `OsmdroidCarMapRenderer` hosts an osmdroid `MapView` with
  `TileSourceFactory.MAPNIK`, and adds `MapDimOverlay` at `overlays[0]` so the
  scrim covers tiles only, beneath strikes, location, and the legend.
- `MapDimMode` is `FOLLOW_CAR` / `ON` / `OFF`. The scrim paint is black with a
  single `DIM_ALPHA` of 128; `OFF` (and dark-mode `FOLLOW_CAR` in light theme)
  leaves the map untouched.
- The phone also always uses MAPNIK: `MapFragment` maps both `MAP_TYPE` values to
  `TileSourceFactory.DEFAULT_TILE_SOURCE` (which *is* `MAPNIK`) and
  `TileSourceFactory.MAPNIK`. `SATELLITE` / `STREETMAP` are legacy names. The
  phone's real distinction is the shading direction: dark map = black fade at 55%
  with the raw bright palette; bright map = white fade at 55% with the palette
  darkened to 80%.
- The car's `StrikeColorHandler` comes from the shared graph
  (`CarDependencies.strikeColorHandler`) and resolves its target from the phone's
  `map_mode` preference. It therefore almost always uses the bright `SATELLITE`
  palette, regardless of any car shading. `StrikeListOverlay.refresh()` calls
  `colorHandler.updateTarget()`, which re-reads that preference.
- `CarLegendOverlay` reads colours from the same handler, so whatever target the
  handler resolves is what both strikes and legend swatches show.
- `BlitzortungCarSession.onCarConfigurationChanged` invalidates the top screen, so
  `onGetTemplate()` already runs on a theme flip.

## Goals / Non-Goals

**Goals:**

- One always-on shading concept with a direction (dark / bright) and a mode that
  decides which direction applies.
- Background scrim and strike palette derived from the same shading state.
- Car-owned colour state that cannot leak into the phone experience.
- Consistent vocabulary in code, strings, and stored preference.

**Non-Goals:**

- A continuous shading strength in the car (the Car App Library has no slider).
- Changing the phone map modes, palettes, or preferences.
- Recolouring or inverting the map tiles.
- A dark/bright tile-source choice for the car.

## Decisions

### Decision: Model shading as a direction, not a dimmer

Introduce `MapShadingMode { FOLLOW_CAR, DARK, BRIGHT }` and resolve it to a
`MapShading { DARK, BRIGHT }` for the current host theme:

| mode | dark host theme | light host theme |
| --- | --- | --- |
| `FOLLOW_CAR` | `DARK` | `BRIGHT` |
| `DARK` | `DARK` | `DARK` |
| `BRIGHT` | `BRIGHT` | `BRIGHT` |

Remove `OFF` entirely.

- **Why**: The phone never shows an unshaded MAPNIK map; both of its map modes
  differ only in shading direction. Removing `OFF` makes the car match that
  invariant, and it removes the "dimming" framing that only fit one direction.
- **Alternative considered**: Keep `OFF` as an escape hatch. Rejected: it
  reintroduces the busy unshaded map the change exists to remove, and it makes the
  setting a mix of "how much" and "which direction".
- **Alternative considered**: Keep `FOLLOW_CAR` / `ON` and redefine `ON`. Rejected:
  "On" cannot express a direction, and with `OFF` gone it degenerates into a
  synonym for one of the forced modes.

### Decision: Two-direction scrim with per-direction alpha

Rename `MapDimOverlay` to `MapShadeOverlay` and give it a colour plus alpha instead
of a single black paint. Use black at 128 (50%) for dark shading and white at 140
(55%) for bright shading.

- **Why**: Black at 128 preserves today's night behaviour. White at 140 matches the
  phone's bright-map fade (55%) and washes the roads and labels back so the data
  reads first. Separate constants allow the white wash to be tuned down later
  without touching the night scrim.
- **Alternative considered**: A single shared alpha for both directions. Rejected:
  the perceptual weight of black over MAPNIK and white over MAPNIK differs enough
  that they need independent tuning.
- **Alternative considered**: A colour matrix that brightens/desaturates tiles.
  Rejected: more code for a result that reads as a different map style rather than
  shading, and the phone already established the translucent-scrim idiom.

### Decision: The strike palette follows the shading direction

Dark shading keeps the raw bright palette. Bright shading applies the existing
`ColorTarget.STREETMAP` treatment (brightness × 0.8, black label colour) to
strikes.

- **Why**: Strike shapes are colour-only strokes with no outline. The newest strike
  colours are `#ffffff` and near-white; on a white-washed background they would be
  invisible. The phone already solves this on its bright map by darkening the
  palette to 80%; reusing that rule keeps the two experiences aligned.
- **Alternative considered**: Keep the bright palette and only lower the white
  alpha. Rejected: it trades away the lightening the change is for and still leaves
  white strikes weak.
- **Alternative considered**: A car-only palette. Rejected: duplicates colour logic
  and diverges from the phone without a demonstrated need.

### Decision: Car-owned handler plus a target override seam

Add an optional target override to the core colour handler, resolved as
`override ?: preferenceTarget` inside `updateTarget()`. The car receives its own
`StrikeColorHandler` instance over the same preferences and sets the override from
the current shading before refreshing.

- **Why**: Two independent hazards rule out mutating the shared instance:
  1. In the Android Auto artifact, `AppCarDependencies` hands the car the same
     `StrikeColorHandler` singleton the phone UI uses; a car override would recolour
     the phone map.
  2. `StrikeListOverlay.refresh()` calls `updateTarget()`, which re-reads the phone
     preference, so a transient target set from outside would be silently undone.
  Resolving the override inside `updateTarget()` fixes both, and a dedicated car
  instance keeps the phone untouched.
- **Alternative considered**: Set the phone's `map_mode` preference from the car.
  Rejected: couples the car theme to a phone setting and mutates phone behaviour.
- **Alternative considered**: Override the target only on `StrikeListOverlay`.
  Rejected: `CarLegendOverlay` reads colours directly from the handler, so strikes
  and legend swatches would disagree.
- **Alternative considered**: Pass a target into `StrikeListOverlay`. Rejected for
  the same legend-consistency reason, and it spreads the policy across two objects.

### Decision: The renderer owns the shading state and re-applies it on every refresh

`CarMapRenderer.setDimmed(Boolean)` becomes `setShading(MapShading)`. The renderer
stores the latest shading and re-applies both scrim and palette whenever strikes
are updated, not only when `setShading` is called.

- **Why**: Shading arrives via `onGetTemplate()`, while strikes arrive on data
  events. Without re-applying on `updateStrikes`, the first data event after a cold
  start or a theme flip could paint strikes with the previous palette.
- **Alternative considered**: Force a template rebuild after each data update.
  Rejected: that is the flicker the countdown was moved onto the surface to avoid.

### Decision: Rename the feature end to end, with no migration

Rename `MapDimMode` → `MapShadingMode`, `MapDimOverlay` → `MapShadeOverlay`,
`setDimmed` → `setShading`, `isMapDimmed` → `currentShading`, the preference key
`map_dim_mode` → `map_shading_mode`, and the strings to shading vocabulary
(`car_settings_map_shading`, `car_shade_follow_car`, `car_shade_dark`,
`car_shade_bright`).

- **Why**: The car shading feature has not shipped, so no stored value can exist in
  the field. Keeping "dim" names would describe a feature that no longer exists and
  would force future readers to translate between two vocabularies.
- **Alternative considered**: Keep the old key and map `ON` → Dark, `OFF` → Bright.
  Rejected by the explicit decision that no compatibility is needed.

## Risks / Trade-offs

- **A 55% white wash may be too strong for a driving map** → Keep the bright alpha
  as a standalone constant and verify on the AAOS emulator; lowering it does not
  affect the dark scrim.
- **The newest strikes may still be weak on bright shading** → The palette
  treatment darkens them to 80%; if that is insufficient, deepen the palette factor
  or lighten the scrim alpha, both local to `:core`/`:car`.
- **The legend panel stays dark with white text on a pale map** → Accepted: it
  keeps high contrast and is drawn above the scrim, so it is never washed out.
- **A theme flip while the map is visible could leave stale strike colours** →
  Re-applying shading on `updateStrikes` plus the existing
  `onCarConfigurationChanged` invalidation covers both the flip and the next data
  event; a renderer test asserts palette changes on shading change.
- **Mutating the shared handler would regress the phone** → Dedicated car handler
  plus an override that cannot be re-read away; a test asserts the override
  survives `updateTarget()`.
- **`ColorTarget.SATELLITE` / `STREETMAP` remain misleading names** → Document them
  as dark/bright shading targets in code; renaming them would change the phone's
  stored preference values, which this change does not touch.

## Migration Plan

Code-only change in `:core`, `:car`, and the car dependency wiring of `:app` /
`:automotive`, plus strings and tests. The stored key is new and the old one is
ignored, which is safe because the feature is unreleased. Rollback is a straight
revert.
