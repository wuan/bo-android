# Design

## Context

Reference: `proposal.md` for motivation and `specs/` for the required behaviour.
The current state that shapes the approach:

- `OsmdroidCarMapRenderer.goToCurrentLocation()` moves the camera synchronously
  with `controller.setZoom(8.0)` + `controller.setCenter(location)`, then stores
  the camera and notifies the viewport listener immediately.
- `CarMapViewport` reports `viewportIsAnimating = false` and `animator() = null`
  on the premise that the car camera never animates. `MainDataHandler` uses those
  two members to defer a data refresh to the end of a camera animation.
- The phone's own-location framing is `Main.onAlertViewClicked()`: the target
  distance is the outermost alert range step (`rangeSteps.last()`, currently
  500), reduced to `closestStrikeDistance * 2.0` clamped to `[50, max]` when the
  alert event is a `LocalActivity`, then
  `MapFragment.calculateTargetZoomLevel(distance * 1000f)`.
- That zoom computation divides metres-per-pixel by `2.7` per zoom step instead
  of `2.0`, and ignores latitude, so it frames roughly ten times the requested
  ground width. It also treats alert distances as kilometres regardless of the
  measurement system.
- Distances and range steps in `AlertParameters` are expressed in the active
  measurement unit: `MeasurementSystem.calculateDistance()` divides metres by
  `1000` (metric) or `1609.344` (imperial).
- `AlertRepository.getAlertParameters()` exposes the range steps and measurement
  system; the car screen already holds the latest `Warning` in
  `alertListState.warning`.
- osmdroid's `MapController.animateTo(point, zoom, speed)` uses a `ValueAnimator`
  and marks the map animating. Calling `animateTo` again while an animation is
  running does not cancel the previous animator; `stopAnimation(false)` does.

The car map is drawn by an osmdroid `MapView` hosted in a `Presentation` on a
`VirtualDisplay`. `ValueAnimator` runs in the app process on the main looper, so
the camera animation should render on the host surface like any other frame;
this is verified on a device/emulator as part of the tasks.

## Goals / Non-Goals

**Goals:**

- One exact, latitude- and unit-aware distance-to-zoom computation, shared by the
  phone and the car, with the phone's own-location target policy centralized too.
- A car location transition that animates, retargets on a repeated action, and is
  interrupted by driver navigation without snapping to the target.
- Exactly one settle per final camera: store the camera and refresh data for the
  settled viewport, never for intermediate animation frames.
- Keep the car's first-fix framing on the existing default zoom.

**Non-Goals:**

- Animating the zoom buttons or the first-fix framing.
- Adding a car-side keep-zoom preference.
- Changing data providers, alert computation, repositories, or stored formats.
- Scaling the animation duration by distance or adding a reduced-motion preference
  beyond what the system animator duration scale already provides.
- Reworking pan/fling gesture handling beyond cancelling a running transition.

## Decisions

### Decision: Exact Web-Mercator fit in `:core`, with the phone delegating

Add a pure computation that takes a ground span in metres, the shorter viewport
side in pixels, and the target latitude, and returns a fractional zoom:

```
metresPerPixelAtZoom0 = 2 * PI * 6_378_137 / 256
zoom = log2(metresPerPixelAtZoom0 * cos(latitude) * viewportPixels / spanMeters)
```

The latitude is clamped to the Web-Mercator limit (±85.051129°) so the cosine
cannot reach zero. `MapFragment.calculateTargetZoomLevel` delegates to it, taking
the target latitude from the point being framed; the phone's histogram spans stay
at their existing physical sizes (1,800 km, 5,000 km, 40,000 km, now expressed in
metres). The car uses the same function with the fix latitude and the map
surface's shorter side.

- **Why**: The current `÷2.7` loop is an approximation that frames a different
  distance than the caller requests; a pure function with the Mercator scale is
  exact, testable, and gives the car and phone identical framing.
- **Alternatives considered**:
  - Keep the `÷2.7` formula for phone parity: rejected by the decision to adopt
    an exact fit.
  - Build a `BoundingBox` around the target and use osmdroid's
    `TileSystem.getLatitudeZoom`/`getLongitudeZoom`: equivalent, but needs a
    bounding box and couples the computation to more osmdroid state.
- **Consequence**: Phone own-location and histogram views become noticeably
  tighter. This is intentional and captured by `map-zoom-framing`.

### Decision: Unit-aware framing distances

Add `MeasurementSystem.toMeters(value)` (the inverse of `calculateDistance`). Add
`AlertZoomTarget.framingMeters(warning, parameters)` implementing the shared
policy:

```
max = parameters.rangeSteps.last()                 // display units
target = if (warning is LocalActivity)
             (warning.closestStrikeDistance * 2.0f)
                 .coerceIn(50f, max)
         else
             max
return parameters.measurementSystem.toMeters(target)
```

The doubled distance, the 50-unit minimum, and the outermost-step maximum keep
nearby activity in view. Doubling is required because the fit uses the shorter
viewport side: a smaller factor can frame the closest strike in landscape yet
crop the surrounding activity in portrait. `Infinity` from a `LocalActivity`
without a finite closest sector naturally clamps to `max`.

- **Why**: Alert distances and range steps are rendered in the user's unit, so
  framing them as kilometres is wrong in imperial and inconsistent with the
  alert UI.
- **Alternatives considered**: keep the km assumption and file a separate fix
  (rejected: the framing computation is already being rewritten and the car needs
  a unit-correct span).

### Decision: The car screen computes the target span; the renderer only frames it

`CarMapRenderer.goToCurrentLocation(spanMeters: Float)` takes the target ground
span. `BlitzortungMapScreen` computes it from `alertListState.warning` and
`alertRepository.getAlertParameters()` and passes it in.

- **Why**: Alert state and policy stay in the screen/alert layer; the renderer
  remains a camera/rendering component and is easy to test without alert wiring.
- **Alternative considered**: inject `AlertRepository` into the renderer. Rejected:
  couples rendering to alert state and makes renderer tests heavier.

### Decision: Animate with osmdroid, 500 ms, matching the phone

`controller.animateTo(GeoPoint(latitude, longitude), targetZoom, 500L)` drives
the transition. The constant lives in the renderer as the car's camera animation
speed, matching `OwnMapView.DEFAULT_ZOOM_SPEED`.

- **Why**: Same interpolation, zoom-tracking and listener behaviour as the phone;
  no second animation implementation to keep in sync.
- **Alternative considered**: a hand-rolled `ValueAnimator` calling
  `setZoom`/`setCenter` per frame. Rejected: duplicates osmdroid's Mercator-aware
  interpolation and its scroll/zoom events.

### Decision: A generation token and an explicit interruption protocol

The renderer owns a monotonically increasing `cameraGeneration`. Every camera move
follows one protocol:

```
startMove(target, zoom):
    generation++
    controller.stopAnimation(false)        // invalidate and cancel the previous move
    controller.animateTo(target, zoom, 500)
    viewport.animator()?.addListener(onEnd -> if (generation == current) postSettle())

interrupt():                               // before applying driver input
    generation++
    controller.stopAnimation(false)

settle():
    storeCamera()
    notifyViewportChanged()
```

Pan (`onScroll`), fling (`onFling`), scale (`onScale`), and the zoom actions call
`interrupt()` first, then apply their input, then `settle()`. A repeated location
action calls `startMove` again, which cancels the old move without settling. The
natural animation end posts `settle()` to the map view so the viewport is notified
after the animator has fully ended, independent of `ValueAnimator` listener
ordering. If no animator is available after starting the move (an osmdroid field
change or a map that has no size), the target camera is applied immediately and
settled instead, so the camera can never get stuck half-way.

- **Why**:
  - `stopAnimation(false)` freezes the current camera; `stopAnimation(true)` would
    snap to the target, which is the wrong interruption semantics.
  - The token prevents the cancelled animator's `onAnimationEnd` (osmdroid fires
    cancel and end) from storing or notifying a camera the driver abandoned.
  - Explicitly stopping before `animateTo` avoids osmdroid starting a second
    animator while the first keeps updating the map.
  - Settling only at the end avoids one data-grid/local-area change per frame.
- **Alternatives considered**:
  - `postDelayed(speed)` or polling `isAnimating`: fragile with cancellation and
    retargeting.
  - Notify the viewport on every frame and rely on `MainDataHandler` to defer: it
    only defers the final `updateData()` call, not the per-frame grid updates, and
    would still need the animator accessor.

### Decision: The car viewport reports its real animation state

`CarMapViewport.viewportIsAnimating` returns `mapView.isAnimating`, and
`animator()` returns the controller's current animator. The reflection on
osmdroid's private `MapController.mCurrentAnimator` — already used by
`OwnMapView.animator()` — moves into one shared `:core` accessor used by both map
views.

- **Why**: The old "never animating" contract becomes false. An honest viewport
  also lets `MainDataHandler` defer correctly if a viewport notification ever
  arrives during a flight (for example a resize), rather than issuing an
  intermediate request.
- **Trade-off**: Reflection on a private osmdroid field. Isolating it in one
  tested helper matches the existing phone approach and limits the blast radius of
  an osmdroid upgrade.

### Decision: Settle once; cancel without settling on release

`storeCamera()` and `notifyViewportChanged()` happen only in `settle()`: on natural
completion or after an interruption. `release()` bumps the generation and cancels
the animation without settling, because the screen is going away and an
intermediate camera is not a meaningful session view. `onSurfaceDestroyed` keeps
its no-op behaviour (the presentation and map are deliberately retained), so an
in-flight 500 ms animation may finish while the surface is temporarily hidden.

### Decision: First-fix framing stays at 8.0

`DEFAULT_ZOOM_LEVEL` remains the first-fix framing zoom and keeps its one-shot
behaviour. The target policy applies only to the location action.

## Risks / Trade-offs

- **Animation may not render as expected in a `Presentation` on a `VirtualDisplay`**
  → Verify early on the Desktop Head Unit and an AAOS emulator; the camera math
  is independent of the host surface, so a failure would be a rendering issue,
  not a framing issue.
- **Per-frame strike re-projection cost** → osmdroid fires a zoom event per frame,
  and `StrikeListOverlay.refresh()` rebuilds every strike shape when grid data is
  in use. The phone already does this during its own transition, but car hardware
  can be slower. Measure on the target; if it janks, the duration/refresh strategy
  is a local constant change, not a spec change.
- **Phone framing visibly changes** → Own-location and histogram views now frame
  the requested distance instead of roughly ten times it. Intentional; captured by
  `map-zoom-framing` and noted in the release notes when the change ships.
- **Imperial distances now mean miles everywhere in the policy** → The 50-unit
  minimum and the range steps are display units, consistent with the alert views.
  A user switching units can see a different target until the next alert
  computation refreshes the closest-strike state; the target is a snapshot at tap
  time either way.
- **Reflection on `mCurrentAnimator`** → Centralized in one helper with a test and
  wrapped so an inaccessible field returns `null` instead of crashing. The renderer
  then applies the target camera immediately and settles, so the camera still ends
  in a coherent state; the helper is the only place that needs attention on an
  osmdroid upgrade.
- **Action tapped before the map is laid out** → `animateTo` would replay the call
  on first layout, and no animator exists to listen to. The action is only
  reachable with a visible surface; the renderer additionally no-ops when the map
  has no size instead of storing a meaningless camera.

## Migration Plan

No stored state, preference keys, or data formats change. The change is code and
tests only; rollback is a revert. The phone behaviour change rides the same
release as the car behaviour.

## Open Questions

- Is 500 ms the right duration on car surfaces? It matches the phone and is a
  single constant in the renderer; it can be tuned without changing the specs or
  task breakdown.
