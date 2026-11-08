# Proposal

## Why

The car map's location action teleports the camera to a fixed zoom (`8.0`),
unlike the phone's own-location action, which animates and frames the alert area
around the closest strike. The phone's zoom-fit computation, shared by its
own-location and histogram actions, is also approximate: it ignores latitude and
assumes kilometres, so it frames roughly ten times the requested ground width at
the alert range and is wrong when the measurement system is imperial.

## What Changes

- Make the car location action animate the camera (pan and zoom together, 500 ms,
  like the phone) instead of jumping to the target.
- Give the car the phone's own-location target policy: frame the outermost alert
  range, reduced to twice the closest known strike distance when one is nearer,
  bounded by the range minimum and maximum.
- Replace the phone's approximate zoom-fit with an exact Web-Mercator fit that
  accounts for the target latitude and produces a fractional zoom. The phone's
  own-location and histogram framing both adopt it.
- Make framing distances unit-aware: alert range steps and closest-strike
  distances are converted from the active measurement system (kilometres or
  miles) to metres before the fit. Both phone and car use the corrected result.
- Make the car's camera transition interruptible: pan, fling, scale, and the zoom
  actions stop the transition and take effect from the camera's current position;
  using the location action again retargets it. The camera settles once — the
  final camera is stored and the data layer is refreshed (and only then) for the
  settled viewport.
- Report the car viewport's real animation state instead of claiming the camera
  never animates.
- Honor a configured manual location in the car instead of the device provider,
  so an explicit manual setting is not silently replaced by GPS.
- Collapse the car alert list to one closest-activity status line instead of a
  closest-strike row followed by per-sector strike counts.
- Make the phone histogram a two-step zoom-out: frame the data area first, then
  the global view once that framing is already active.
- Keep the car's first-fix framing at the existing `8.0` default zoom; the target
  policy applies to the location action only.
- Update the car spec for animated framing, the shared target policy, and
  interruption, and add a spec for the shared, unit-aware zoom framing.

## Capabilities

### New Capabilities

- `map-zoom-framing`: the exact, latitude- and unit-aware computation that turns a
  requested ground distance into a map zoom, and the shared own-location target
  policy used by the phone and the car.

### Modified Capabilities

- `car-integration`: the location action animates instead of jumping, uses the
  shared target policy, is interrupted by driver navigation, and settles the
  camera and data request once; the promise that the phone experience is unchanged
  is qualified to allow the shared framing correction.

## Impact

- `:core` — unit conversion on `MeasurementSystem`, the shared own-location target
  policy, the exact zoom fit, and one shared accessor for an in-flight osmdroid
  camera animator; unit tests for each.
- `:app` — `Main` and `MapFragment` delegate own-location and histogram framing to
  the shared computation. Phone map framing becomes correct and noticeably
  tighter; the keep-zoom-on-goto-own-location preference is preserved, and a
  histogram tap reaches the global view after the data-area framing.
- `:car` — `CarMapRenderer.goToCurrentLocation` takes a target span, the renderer
  gains the animated/retarget/interrupt/settle protocol, the map screen passes the
  alert-derived span, `CarMapViewport` reports the real animation state,
  `CarLocationProvider` honors a configured manual location, and the alert list
  collapses to one closest-activity status line; unit tests cover animation,
  target zoom, interruption, retargeting, settling, manual-location precedence,
  and the single status row.
- No preference keys, data providers, alert computation, dependencies, or stored
  formats change.
