# Proposal

## Why

The car map surface is drawn by the app, so it does not follow the car host's
day/night dimming: the host dims the templated chrome (header, alert list) while
the map stays bright. The map only flips to an inverted tile render when the host
reports dark mode, and the driver has no way to control it. Night glare makes the
map hard to use, and exposing the control requires a settings surface the car
experience does not have yet.

## What Changes

- Add car map dimming as a **translucent overlay** over the map surface, matching
  the app's existing translucent-scrim idiom, with three modes:
  **Follow car** (dim when the car host is in dark mode), **On** (always dim), and
  **Off** (never dim). Default: Follow car.
- Replace the current tile-color inversion with the overlay: the map stops
  recoloring tiles and darkens uniformly instead.
- Add a simple, extensible car **settings screen**, reachable from the map header,
  starting with the map-dimming setting.
- Persist car settings in a **car-only** preferences store, independent of the
  phone's default preferences and of the `:core` preference keys.
- Apply to both Android Auto and Android Automotive OS through the shared `:car`
  module; the phone experience is unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: adds a car map-dimming requirement (mode selection rendered
  as a translucent overlay, driven by the car host's dark mode for Follow car) and
  a car settings-screen requirement (an extensible settings surface reachable from
  the map, persisting to a car-only store).

## Impact

- `:car` — new settings screen and navigation, a car settings store (car-only
  preferences plus an observable), a dimming scrim in the renderer, a settings
  entry point on the map header; the existing tile-inversion dark-mode path is
  removed; new strings, a check/select drawable, and unit tests.
- `:app` / `:automotive` — expose the car settings store through the existing
  `CarDependenciesProvider` seam; no phone UI, data-layer, alert-computation, or
  `:core` preference-key changes.
- No changes to the data layer, alert computation, or the phone experience.
