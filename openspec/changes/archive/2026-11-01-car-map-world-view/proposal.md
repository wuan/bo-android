# Proposal

## Why

When the car map has no location fix (permission denied, passive mode, or simply
before the first fix), it has no defined default view: it sits at osmdroid's
built-in camera and can be zoomed out until the world tiles repeat and panned
endlessly past the world, which reads as broken on a small car display. The
phone map has always had a bounded zoom range; the car map has none. Drivers who
never grant location access therefore see an unbounded, undefined map.

## What Changes

- The car map SHALL fall back to a **world view** — the whole world covering the
  viewport (no empty margins), centered at 0°/0° — whenever it has no location
  fix and no stored camera to restore.
- The car map SHALL NOT be zoomable out past the world: the world view zoom is a
  permanent floor, and panning/zooming past the world's bounds is blocked.
- The car map SHALL **persist its camera** (center and zoom) across sessions and
  restore it in preference to the world view, so a reconnecting head unit returns
  to the driver's last view.
- No change to the phone experience, the data layer, or the alert computation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: the "Car map shows live lightning strikes" requirement's
  "No location available" behavior changes from "leave the centre and zoom
  unchanged" to showing the bounded world view, and the capability gains
  requirements for the world-view fallback, the visible-world bounds, and camera
  persistence across sessions.

## Impact

- `:car` — `OsmdroidCarMapRenderer` (world-view fallback, min-zoom floor,
  scrollable-area limit, camera save/restore), `CarSettings` (new camera keys),
  and the renderer/settings unit tests.
- `:app`, `:core`, the phone UI, preference keys, and the data layer are
  untouched.
