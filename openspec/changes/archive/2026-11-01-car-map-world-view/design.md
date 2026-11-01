# Design

## Context

See `proposal.md` — Why. Current state that shapes the approach:

- `OsmdroidCarMapRenderer` owns a plain `MapView(carContext)`:
  - `setMinZoomLevel`/`setMaxZoomLevel` are never called, so
    `MapView.getMinZoomLevel()` falls back to the tile overlay's minimum
    (MAPNIK ≈ 0).
  - No scrollable-area limit is set, so the map can pan past and wrap the world.
  - The camera is never persisted; only `Configuration` is loaded from prefs.
- Framing is stateful and in-memory: `initialLocationFramed` (frame once) and
  `userMovedMap` (suppress framing after any pan/zoom), both reset when the
  renderer is recreated. `onLocationEvent` ignores `NoLocation`.
- The surface dimensions arrive in `onSurfaceAvailable(SurfaceContainer)`
  (`width`/`height`/`dpi`) before the `Presentation` is shown — the only point
  where the viewport size is known before layout.
- `CarSettings` is the car-only preference file (`car_settings`), already used
  for `map_dim_mode`, and is the established place for car state that must stay
  independent of the phone app.
- Installed osmdroid is 6.1.20; verified against the AAR:
  `MapView.setMinZoomLevel(Double)` / `setScrollableAreaLimitDouble(BoundingBox)`
  / `getTilesScaleFactor()`; `TileSystem.getBoundingBoxZoom(...)`,
  `getLongitudeZoom(...)`, `getLatitudeZoom(...)`, `getTileSize(double)`.
  `BoundingBox` has **no** full-world constant, so the world box must be built
  explicitly.

## Goals / Non-Goals

**Goals:**

- A deterministic, bounded default view when the map has neither a fix nor a
  stored camera.
- A session-wide zoom floor and pan bound that never let the world shrink below
  the viewport or be scrolled off it.
- Camera persistence that restores the driver's last view and suppresses
  first-fix framing in that case.
- All of the above inside `:car`, using the car-only settings file.

**Non-Goals:**

- Any phone-map change (`OwnMapView.minZoomLevel = 1.5`,
  `MapViewModel.DEFAULT_ZOOM_LEVEL`) — explicitly out of scope.
- Persisting the *auto-framed* camera. Only driver navigation is persisted, so
  the "world view without a location" behaviour still applies on each fresh
  session.
- Changing the existing framing zoom (`DEFAULT_ZOOM_LEVEL = 8.0`), strike
  rendering, dimming, or the data layer.
- Reframing when a location arrives after a camera was restored.

## Decisions

### Decision: COVER world view, computed from the surface

Compute the world-view zoom as the level at which the projected world is at
least as large as the viewport in **both** dimensions (cover), i.e. the maximum
of the fit-width and fit-height zooms, using the tile size/scale osmdroid
actually uses. Centre on `0°`/`0°`.

- **Why**: the request is "full world, but not smaller". Fit-inside would leave
  empty margins and repeated tiles on a non-square viewport; cover guarantees no
  margins while keeping the world fully in range.
- **Why not a constant**: the fit zoom depends on surface pixels and the tile
  scale factor, so a hardcoded value (like the phone's `1.5`) is wrong on most
  head units.
- **Alternatives considered**: `TileSystem.getBoundingBoxZoom(worldBox, w, h)`
  alone — rejected, it computes a *fit* (min of the two axes); take the max via
  the public per-axis helpers instead. Manual `log2(max(w, h) / tileSize)` —
  workable but duplicates osmdroid's scale handling.

### Decision: Enforce the floor with `MapView.setMinZoomLevel`

Set `mapView.setMinZoomLevel(worldZoom)` per surface and leave it set for the
session.

- **Why**: osmdroid's `MapController` (pinch, action-strip zoom, `canZoomOut`)
  all honour the map minimum, so a single call covers every zoom path; a
  per-gesture clamp would not.
- **Alternative considered**: clamp inside `onScale`/`zoomOut` — rejected,
  duplicates logic and misses `zoomOutFixing` and animation.

### Decision: Bound panning with `setScrollableAreaLimitDouble`

Set the scrollable area to the world box
`BoundingBox(85.051129, 180.0, -85.051129, -180.0)`.

- **Why**: makes "block panning past the world" explicit for every aspect ratio
  and disables longitude wrap.
- **Alternative considered**: `setScrollableAreaLimitLatitude` only — rejected,
  it does not bound horizontally.

### Decision: Persist the camera in `CarSettings`

Add centre latitude/longitude and zoom keys to `CarSettings` (car-only file). A
camera is "stored" only when all three are present. Restore it in
`onSurfaceAvailable`; save it whenever the driver navigates. Clamp a restored
zoom up to the surface's world-view floor.

- **Why**: `CarSettings` already owns car-only state and existing specs require
  the car to stay independent of phone preferences.
- **Alternatives considered**: instance/static fields — rejected, they do not
  survive a session; reusing the phone's osmdroid prefs — rejected, violates
  car/phone independence.

### Decision: Restored camera suppresses framing

When a stored camera is restored, mark a `cameraRestored` flag; `onLocationEvent`
then skips first-fix framing.

- **Why**: otherwise a restored view would be immediately replaced by the
  current position, defeating persistence.
- **Alternative considered**: reuse `initialLocationFramed` — workable, but a
  named flag states the intent and keeps the two one-shot rules distinct.

### Decision: Apply the world view at surface time, not on `NoLocation`

Establish the world view in `onSurfaceAvailable` when no camera is stored, and
do not react to `NoLocation` events afterwards.

- **Why**: a `NoLocation` event may never arrive (permission prompt, passive
  mode), yet the map must be usable immediately; and the spec says a missing
  location must not move an already-established view.
- **Alternative considered**: drive the fallback from the location stream —
  rejected, it would leave the map undefined until the first event.

## Risks / Trade-offs

- **Surface dimensions vs. tile scale mismatch** → derive the zoom from the same
  tile-size/scale source osmdroid uses for rendering, and verify the world
  exactly covers the viewport on the emulator.
- **Surface size can change between sessions/head units** → recompute the world
  view and the floor on every `onSurfaceAvailable`, and clamp any restored zoom
  to the new floor.
- **Frequent writes while panning** → SharedPreferences `apply()` is
  asynchronous and non-blocking; if write volume proves noisy, save on surface
  teardown instead, but a driver's navigation must survive a kill, so
  per-navigation saving is the safer default.
- **Requirement wording** → the existing "No location available" scenario
  changes meaning, so it is delivered as a MODIFIED requirement (full block
  restated); the new fallback, bounds, and persistence are ADDED requirements.

## Migration Plan

New car-only preference keys with no stored default are read as "no stored
camera", so the world view applies until the driver first navigates. No data
migration; rollback is a straight revert, and the extra keys are inert if left
behind.
