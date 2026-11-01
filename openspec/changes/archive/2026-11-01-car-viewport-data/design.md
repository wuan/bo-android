# Design

## Context

See `proposal.md` — Why. The relevant current state:

- The car map is a plain `osmdroid.views.MapView` hosted in a `Presentation` on a
  `VirtualDisplay`, created and driven by `OsmdroidCarMapRenderer`. The host
  surface callback drives camera changes (`onScroll`, `onFling`, `onScale`) and
  the renderer also changes the camera itself (action-strip zoom, first-fix
  framing, restored/world camera on surface available).
- `MainDataHandler.updateGrid(viewport: MapViewport, autoGridSize: Boolean)` is
  the single entry point that updates both the auto grid size (from zoom) and
  the local data area (from the viewport bounding box) and then triggers a
  fetch. `LocalData.updateParameters` turns that into either a global request
  (squares ≥ 25 km) or a local one (squares < 25 km). No changes are needed in
  `LocalData` or `JsonRpcData`.
- On the phone, `OwnMapView : MapView, MapViewport` is registered with
  `mapView.addMapListener(dataHandler)`, plus one explicit `updateGrid` after the
  first layout. `OwnMapView` lives in `:app` and carries phone-only gestures and
  dialogs.
- `:car` reaches the data layer only through `CarDependencies.strikeDataRepository`;
  `MainDataHandler` is not exposed. Nothing in `:car` currently calls
  `updateGrid`, feeds `MainDataHandler.location`, or implements `MapViewport`.

## Goals / Non-Goals

**Goals:**

- The car map's data (grid size and global/local area) follows the map viewport,
  matching the phone's automatic mode.
- The car is fixed to automatic mode: global/local automatic, grid size
  automatic, never a fixed classical region.
- No data-area outline on the car; the first request is deferred until the map
  viewport is known.

**Non-Goals:**

- Changing the grid-size thresholds, the global/local threshold, or the RPC
  methods.
- Adding region selection to the car, or any region/grid-size display.
- Changing the phone UI, the phone's data path, or the background service
  (`ServiceDataHandler` keeps its fixed local parameters).
- Clusters or animation on the car.

## Decisions

### Decision: Push the viewport explicitly from the renderer, not via map listeners

`OsmdroidCarMapRenderer` notifies a viewport listener after every camera change.
The listener is supplied by `BlitzortungMapScreen` and calls the repository.

- **Why**: the car's camera is moved programmatically by the renderer and by the
  host surface callback, not by touch on the map. Relying on osmdroid's
  `MapListener` dispatch for programmatic scrolls inside a `Presentation` on a
  `VirtualDisplay` is unverified, and the renderer already owns every camera
  change.
- **Alternative considered**: a `CarMapView : MapView, MapViewport` added with
  `mapView.addMapListener(handler)`, mirroring the phone. Rejected: it exposes
  the data handler to `:car` and depends on event dispatch the car does not
  otherwise rely on for camera state.
- **Alternative considered**: reuse `OwnMapView`. Rejected: it lives in `:app`
  and drags phone gestures, dialogs, and resources with it.

### Decision: Reuse `MapViewport` through a small car-side adapter

A car-side adapter over the `MapView` implements `MapViewport`
(`viewportZoomLevel`, `viewportBoundingBox`), reporting
`viewportIsAnimating = false` and `animator() = null`.

- **Why**: it reuses the existing view-agnostic port so `MainDataHandler` needs
  no change, and the car's camera changes are programmatic, so the phone's
  "update after animation ends" path is not needed — `updateGrid` applies at
  once.

### Decision: `StrikeDataRepository.updateViewport(viewport)` forces automatic mode

Add a repository method that forwards to the handler with region pinned to
global and auto grid sizing enabled, exposed through `CarDependencies`.

- **Why**: the car has no region or grid-size UI and must not depend on the
  phone's `REGION`/`GRID_SIZE` preferences (a restored preference backup could
  otherwise pin a fixed region or size). A narrow repository method keeps
  `MainDataHandler` and its scheduling out of `:car`.
- **Alternative considered**: expose `MainDataHandler` via `CarDependencies`.
  Rejected: wider surface and a layering leak.
- **Alternative considered**: the car writes the `REGION`/`GRID_SIZE`
  preferences at startup. Rejected: it mutates shared state and is fragile.

### Decision: Cover every camera change, and drive the initial one from layout

Push the viewport after `onScroll`, `onFling`, `onScale`, `zoomIn`, `zoomOut`,
first-fix framing, restored/world camera setup, and from
`mapView.addOnLayoutChangeListener`.

- **Why**: `updateGrid` only acts when the grid size or the data area actually
  changed, so notifying on every scroll is cheap; the layout listener covers
  both the first layout and surface resizes in one place.
- **Why layout**: the bounding box is meaningless before the map view has a
  size, so the initial update must wait for layout.

### Decision: Opt the car out of the shared data-area outline

`StrikeListOverlay` gains a `showDataArea` flag (default `true`); the car passes
`false`.

- **Why**: the outline is drawn whenever `parameters.region != 0`, and the local
  data tiles are roughly half the visible span, so their boundary is on screen.
  The car should not draw it, but the phone behaviour must not change.
- **Alternative considered**: change the shared guard to `region > 0`. Rejected:
  it would also remove the outline from the phone's local view.

## Risks / Trade-offs

- **Garbage data area before layout** → the initial update is driven by the
  layout listener, and the screen does not fetch before it.
- **Network churn while panning at high zoom** → `LocalData.update` only
  re-targets when the view leaves the loaded area or the scale changes, matching
  the phone; accepted.
- **Deferred first request** → the alert list briefly shows its waiting state
  until the first viewport notification; accepted (it is short and avoids a
  wasted global fetch).
- **Preference restore** → forced automatic mode ignores any restored fixed
  region or grid size.
- **Overlay rebuild on a grid-size change** → `updateStrikes` reinitialises the
  overlay when the parameters change, so a zoom step briefly clears it; verify
  there is no visible flicker on the emulator.
- **Location is not fed to the data handler on the car** → the automatic path
  uses the viewport, not the location, so this is not a blocker; alerts still
  receive the location through their own handler.

## Migration Plan

Code-only; no stored state, preference keys, or data migrations. Rollback is a
straight revert. Verify on the Automotive emulator: a wide view requests global
data at ≥25 km squares; zooming in switches to local `<25 km`; panning retargets
it; the settings screen is unchanged; no data-area outline appears.

## Open Questions

- Should viewport updates be debounced while a pan is in flight (e.g. act once
  the gesture settles)? Deferrable: the phone updates on every change and the
  handler already suppresses duplicate requests.
