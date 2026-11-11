# Design

## Context

See `proposal.md` for motivation and `specs/car-integration/spec.md` for the
required behaviour. The current state that shapes the approach:

- `CarLegendOverlay` draws a `countdown` string above one swatch row per palette
  colour. Each row is a solid rectangle plus an age label (`< 10 min`, …).
- `BlitzortungMapScreen` derives the countdown string from
  `CarAlertListState.refreshCountdown`, which `CarAlertListState.reduce()` parses
  from each `StatusUpdate`. The string is pushed through
  `CarMapRenderer.setCountdown()` to the legend overlay.
- `DataReceived.referenceTime` is the server reference time for the batch and is
  already forwarded to `StrikeListOverlay.referenceTime` inside
  `OsmdroidCarMapRenderer.updateStrikes()`. The screen receives the same event in
  `handleDataEvent()`.
- `CarLegendOverlay` reads the active colours from the strike overlay's
  `StrikeColorHandler`, so it follows the colour scheme and the bright/dark
  `targetOverride` applied for map shading.
- The panel anchors to the bottom-left of the host's stable map area and scales
  its metrics from the surface dpi. Text size is 16 dp on phone/Android Auto and
  20 dp on the Automotive build.
- Realtime strike colours are computed from `System.currentTimeMillis()` rather
  than `referenceTime`, so the rendered age of a strike includes the refresh
  delay. The legend is an age scale relative to the newest data, not an exact
  clock anchor.

## Goals / Non-Goals

**Goals:**

- Replace the timer countdown with a data timestamp that changes only when new
  data arrives.
- Replace the per-colour row list with one vertical gradient band and exactly
  three age labels.
- Remove the obsolete countdown state and its per-second redraw path.
- Preserve the panel's bottom-left anchoring, density scaling, and legibility
  over the dimming scrim.

**Non-Goals:**

- Changing how strikes are coloured or binned; the map keeps discrete 10-minute
  bands.
- Changing the phone `LegendView`, data providers, data-handler refresh logic, or
  preference keys.
- Adding a timer that ticks the data timestamp; the timestamp is not a clock.
- Changing the legend's position or adding a show/hide preference.

## Decisions

### Decision: Use `DataReceived.referenceTime` as the data timestamp

The status line shows `Data HH:MM:SS` constructed from
`DataReceived.referenceTime` of the latest successful result. The screen formats
it using a fixed 24-hour `HH:mm:ss` pattern in the device's locale and time zone.

- **Why**: `referenceTime` is the timestamp the strike ages are derived from and
  is already part of the data event. It answers "how fresh is this map?" more
  directly than a refresh countdown.
- **Alternatives considered**:
  - `System.currentTimeMillis()` at receipt: always current, but describes the
    client clock rather than the data batch.
  - Both timestamp and relative age: widest status line and unnecessary detail
    for a car display.
- **Consequence**: A failed request leaves the previous timestamp in place, so a
  stale map visibly shows stale data. Before the first success, the line is
  omitted.

### Decision: Format in the screen, push a string to the renderer

`BlitzortungMapScreen` formats the timestamp and calls a renamed
`CarMapRenderer.setDataTime(dataTime: String)` (replacing `setCountdown`). The
legend stores the string in a `dataTime` field.

- **Why**: Formatting stays with the screen, which already owns car-localised
  strings and is the layer that sees `DataReceived`. The renderer and overlay stay
  free of date formatting and locale concerns.
- **Alternatives considered**:
  - Let `CarLegendOverlay` format `StrikeListOverlay.referenceTime` directly:
    fewer calls, but hides locale/time-zone policy in a drawing class and keeps a
    second source of truth for "latest data time".
  - Pass the raw `referenceTime` to the renderer: tests and resources would still
    need a formatter; the string boundary is simpler.

### Decision: Remove countdown state rather than leave it dormant

Delete `CarRefreshCountdown`, the `refreshCountdown` field on
`CarAlertListState`, and the `StatusUpdate` branch of `reduce()`. `StatusUpdate`
events then leave the car alert-list state unchanged.

- **Why**: A second, unrendered copy of refresh state would be dead weight and
  would keep tests asserting behaviour no user can observe. Nothing else in the
  car reads the countdown.
- **Alternatives considered**:
  - Keep the parser for future use: rejected; it is small and easy to restore
    from history if a future car feature needs it.
  - Keep the field but stop rendering: rejected because it leaves misleading
    state and tests.

### Decision: Unbroken vertical gradient band

The band is drawn as one rectangle of the existing swatch width, reduced by 30%
(11.2 dp), and a height of `numberOfColors * ROW_HEIGHT_DP`, newest at the top. A
`LinearGradient` shader uses the active palette with evenly spaced stops and no
separator lines. The three labels are drawn to the right of the band, vertically
centred on their positions, with the panel reserving half a text line above and
below the band for the overhang.

- **Why**: A single unbroken band communicates the age range at a glance. Three
  labels fit the automotive 20 dp text size without crowding, unlike seven
  boundary labels.
- **Alternatives considered**:
  - Seven boundary labels with two text columns: too dense for the larger
    automotive text size.
  - Horizontal band: the bottom-left panel would become very wide and collide
    with the car bar.
  - Translucent boundary ticks: an earlier iteration drew thin dark ticks at the
    colour boundaries, but they read as black lines interrupting the gradient on
    the vehicle display and were removed after review.
- **Consequence**: The gradient illustrates the age range but does not mark where
  the discrete strike colours switch. This is an accepted display abstraction.

### Decision: Interval-derived labels

Labels are `0 min` at the top, `−(interval / 2) min` in the middle, and
`−interval min` at the bottom, using a true minus sign. The labels and the data
timestamp share one paint, so they use the same size and weight, and neither is
bold. The band and panel height continue to derive from the number of colours,
so a palette or interval change resizes the panel without separate configuration.

- **Why**: The legend already follows `parameters.intervalDuration` and the
  palette count; deriving everything from those two values keeps it correct for
  30-minute intervals and alternate colour schemes.
- **Consequence**: An odd interval uses integer division for the middle label.
  Refresh intervals are even in practice, so this is not user-visible.

### Decision: Cache the gradient shader

Build the `LinearGradient` in `onDraw` only when the palette identity, band
bounds, or scale changed; otherwise reuse the cached shader. The labels are
drawn with an ordinary paint.

- **Why**: Panning and zooming redraw the map surface repeatedly. Allocating a
  shader per frame would create avoidable garbage.
- **Consequence**: Shading changes replace the palette array used by the
  handler, so the identity check refreshes the gradient on a bright/dark switch.

## Risks / Trade-offs

- **The `Data` timestamp can lag the "now" used for strike colours** → Label the
  line `Data` and keep the scale explicitly relative (`0 min` = newest data), so
  the timestamp reads as data freshness rather than a live clock.
- **Smooth band vs. discrete strike colours** → The gradient is understood as
  the age range, not a promise that strikes fade continuously; the map keeps its
  discrete colour bands.
- **Automotive 20 dp text may crowd three labels** → The band is 144 dp tall at
  six colours, and only three labels are drawn; verify on the automotive preview
  during implementation and increase row height if needed.
- **Removing `StatusUpdate` handling changes car state equality** → A status
  update no longer triggers a template invalidate. That is intended and covered
  by renderer/screen tests.
- **Timestamp formatting is locale-sensitive** → The pattern is fixed 24-hour,
  but locale-specific digits are allowed. The spec fixes the `HH:MM:SS`
  structure, not the glyph set.

## Migration Plan

No stored state or preference changes. The change ships in the car artifact with
the normal app update; reverting is a code revert with no data migration.
