# Proposal

## Why

The car map colours each strike by its age, but nothing in the car experience
explains that scale: the alert list reports distances and counts, and the legend
that explains the colours exists only on the phone. A driver therefore sees red
and blue strikes with no way to tell which are recent.

The phone legend cannot simply be reused as-is: it lives in `:app` (which depends
on `:car`, so `:car` cannot reach it), and it also renders region, grid-size and
count-threshold rows that the car map does not need.

## What Changes

- Draw a small, always-on legend on the car map surface, in the top-left corner,
  showing only the strike age→colour scale (one row per colour band, e.g.
  `< 5 min` … `> 20 min`).
- The legend is a car-native map overlay, drawn above the strike, grid and
  dimming layers, non-interactive, and styled for the car (dark translucent
  panel, white text) rather than reusing the phone's translucent text-on-map look.
- New car-only strings; English for now, translation later.
- No change to the phone app, the data layer, alert computation, or preference
  keys.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: adds a requirement that the car map shows a persistent
  legend explaining the strike age colours.

## Impact

- `:car` — a new `CarLegendOverlay` (osmdroid overlay) registered in
  `OsmdroidCarMapRenderer`; new car strings; unit tests.
- `:app`, `:core`, `:automotive`, the phone UI, preference keys, and the data
  layer are untouched.
