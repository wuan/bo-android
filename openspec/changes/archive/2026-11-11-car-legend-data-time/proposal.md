# Proposal

## Why

The car legend's status line shows a `remaining / period` refresh countdown, which
describes the refresh timer rather than the data on the map. Drivers cannot tell
how old the displayed strikes are, and the per-second countdown forces a map
redraw every second. The legend itself repeats one swatch row per palette colour,
so its age scale is a set of hard blocks rather than the continuous age range the
colours represent.

## What Changes

- Replace the car legend's refresh countdown with the timestamp of the current
  data, labelled `Data HH:MM:SS`, taken from the reference time of the latest
  successful `DataReceived` event.
- Draw the car legend as one continuous vertical colour band from `0 min` at the
  top to the full interval (for example `−60 min`) at the bottom, using a smooth
  gradient built from the active strike palette.
- Label the band at three points only: top (`0 min`), middle (half the interval),
  and bottom (the full interval).
- Keep the band orientation and palette consistent with the strikes: newest at
  the top, oldest at the bottom, following the active colour scheme and the
  bright/dark shading override.
- Remove the countdown status-line path: the car no longer needs to render
  `StatusUpdate` countdown values, and the map surface no longer redraws every
  second for timer changes.
- Keep the phone experience, data providers, alert computation, and preference
  keys unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: replaces the "time to the next refresh" requirement with a
  data-timestamp status line, and replaces the row-per-colour legend requirement
  with a continuous unbroken band and three age labels.

## Impact

- `:car` — `CarLegendOverlay` gains a data-time status line, gradient band
  rendering, and three interval-derived labels. `CarMapRenderer` and
  `OsmdroidCarMapRenderer` replace the countdown API with a data-time update.
  `BlitzortungMapScreen` formats `DataReceived.referenceTime` for the status line.
  If the countdown state has no remaining consumer, the `CarRefreshCountdown`
  parsing and the reducer's `StatusUpdate` branch are removed.
- `:car` resources — add the `Data` status-line format string and any band label
  format, replacing the countdown string.
- `:car` tests — update legend, renderer, and map-screen tests from countdown and
  row-based assertions to data-time and band/label assertions. Remove or
  reduce countdown reducer/presenter tests if the state is deleted.
- `:app` and `:core` — no production changes; the reusable strike palette and
  `DataReceived.referenceTime` already provide everything the car needs.
- No preference keys, data providers, alert computation, dependencies, or stored
  formats change.
