# Design

## Context

See `proposal.md` — Why. The relevant current state:

- `:car` renders its map with `OsmdroidCarMapRenderer`, which owns an osmdroid
  `MapView` hosted in a `Presentation` on a `VirtualDisplay`. The `MapView` is
  constructed with osmdroid's default camera (centre `0,0`, default zoom); no
  code ever sets an initial centre or zoom.
- `OsmdroidCarMapRenderer.onLocationEvent` currently only forwards the event to
  `OwnLocationOverlay.locationEventConsumer`, which moves the own-position
  marker. `BlitzortungMapScreen` collects the shared `LocationRepository` flow
  and calls this method for every location event.
- The screen already forwards pan/zoom gestures and the action-strip zoom buttons
  to the renderer (`onScroll`, `onFling`, `onScale`, `zoomIn`, `zoomOut`); the
  renderer is the single place that touches the `MapView` camera.
- The `car-integration` spec requires the same car experience on Android Auto and
  Android Automotive OS; both use this renderer, so a renderer-level change
  covers both platforms.

## Goals / Non-Goals

**Goals:**

- Frame the car map on the first location fix: centre on the position and apply a
  sensible default zoom.
- Do it exactly once, and never override a driver's pan or zoom (including a pan
  or zoom that happens before the first fix).
- Keep the change inside the renderer so the screen, the data layer, and the
  phone UI are untouched.

**Non-Goals:**

- Following the driver continuously (recentring on every fix). That would fight
  manual navigation and is not requested.
- Deriving the zoom from the alert range or a user preference. A fixed default is
  enough for the first iteration; see Open Questions.
- Any change to the phone `MapFragment`/`Main` camera behaviour.

## Decisions

### Decision: Perform the initial framing inside `OsmdroidCarMapRenderer`

Handle framing in `onLocationEvent` rather than in `BlitzortungMapScreen` or on
the `CarMapRenderer` interface.

- **Why**: The renderer already owns the `MapView` camera and already receives
  every location event, so no interface change, no new screen wiring, and no new
  call path are needed. It also keeps the behaviour identical on both car
  platforms, which share the renderer.
- **Alternative considered**: Add a `frameOnLocation()` method to the
  `CarMapRenderer` interface and call it from the screen. Rejected: it adds
  surface area for no gain, since the renderer already sees the same events.

### Decision: One-shot framing guarded by a "has framed" flag and a "user interacted" flag

Track two pieces of state in the renderer: whether the initial framing has been
applied, and whether the user has panned or zoomed. On a `LocationUpdate`, if no
framing has happened yet and the user has not interacted, set the centre to the
fix and set the default zoom; otherwise only update the marker. Set the
"interacted" flag from the gesture/zoom entry points (`onScroll`, `onFling`,
`onScale`, `zoomIn`, `zoomOut`).

- **Why**: The spec requires framing once and preserving user navigation, and
  must not recentre when the driver has already positioned the map. Ordinary
  later fixes must keep moving the marker only.
- **Why separate flags**: A single "framed" flag cannot express "the user moved
  the map before the first fix", which the spec explicitly requires to suppress
  framing. Programmatic `controller.setCenter`/`setZoom` does not pass through
  the gesture entry points, so it does not incorrectly set the interaction flag.
- **Alternative considered**: Recentre on every fix. Rejected: overrides manual
  navigation while driving.

### Decision: A fixed car default zoom, independent of the phone default

Define a car-local constant (the phone's `MapViewModel.DEFAULT_ZOOM_LEVEL = 3.0`
is a world view and lives in `:app`, which `:car` must not depend on) and apply
it when framing.

- **Why**: `:car` cannot reuse the app module's constant, and the car wants a
  closer, local view than the phone's initial world view. A named constant keeps
  the value easy to tune.
- **Alternative considered**: Derive the zoom from the configured alert range as
  the phone's "go to own location" does. Rejected for now: it couples the
  renderer to alert parameters for marginal benefit; the constant is sufficient
  and can be revisited.

## Risks / Trade-offs

- **Location arrives before the map surface exists** → osmdroid's controller
  caches the requested centre and zoom and applies them when the `MapView` is
  laid out, so framing the camera before the surface is available is safe. No
  mitigation needed; covered by a unit test.
- **Driver's first gesture races the first fix** → The interaction flag is set by
  the same callback that performs the gesture, so whichever happens first wins
  deterministically; if the gesture is first, framing is suppressed as the spec
  requires.
- **A default zoom that is too close or too far** → Exposed as a single named
  constant and called out in Open Questions so it can be tuned without touching
  the logic.

## Migration Plan

Code-only change in `:car` with unit tests; no stored state, preference keys, or
data migrations. Rollback is a straight revert. No release sequencing is
required beyond the normal car build.

## Open Questions

- Is the chosen default zoom the right first impression for a typical car screen,
  or should it be derived from the alert range / a preference later? Safe to
  answer after seeing it on the Desktop Head Unit or AAOS emulator; the value is
  a single constant and does not change the specs or the task breakdown.
