# Design

## Context

See `proposal.md` — Why. The relevant current state:

- The car map is drawn by the app onto a host-provided surface, hosted as an
  osmdroid `MapView` inside a `Presentation` on a `VirtualDisplay`
  (`OsmdroidCarMapRenderer`). The overlays are ordered: `[0] MapDimOverlay`
  (background scrim), then `StrikeListOverlay`, then `OwnLocationOverlay`.
- `StrikeListOverlay` already exposes `colorHandler: StrikeColorHandler` and
  `parameters: Parameters`; `Parameters.intervalDuration` and
  `ColorHandler.colors` are exactly what the legend needs. Both live in `:core`.
- The phone legend `LegendView` lives in `:app` and is a canvas `View` placed as a
  sibling of the phone `MapView`. It shows the colour scale plus optional region,
  grid-size and count-threshold rows, and taps through to quick settings. Its
  helper `TabletAwareView`, its `tablet_scaleable` styleable, and the
  `regions`/`regions_values`/`translucent_background`/`unit_minute`/`not_available`
  resources also live in `:app`.
- `:car` depends on `:core` and is depended on by `:app`; `:car` cannot see
  `:app` code or resources.
- The `MapWithContentTemplate` content list (the alert list) is rendered by the
  host as a panel over the map surface; its side and size are host-owned and not
  settable by the app. On the AAOS emulator the list covers the left side of the
  surface, so the raw surface's top-left is hidden behind it.
- The host reports the map area it does not obscure through
  `SurfaceCallback.onVisibleAreaChanged(Rect)` and `onStableAreaChanged(Rect)`;
  the app can anchor its own surface drawing inside those rectangles.
- The host routes map-surface touches to `CarMapSurfaceCallback`, not into the
  `Presentation`, so a surface-drawn element is not independently tappable.

## Goals / Non-Goals

**Goals:**

- Always show the strike age→colour scale on the car map, in the top-left.
- Keep it small: colour-scale only, compact rows.
- Style it for a car (dark translucent panel, white text), not as a phone copy.
- Reuse the existing colour scale so the legend matches the strikes with no new
  data plumbing.

**Non-Goals:**

- Reuse `LegendView` itself; move anything from `:app` into `:core`; or change the
  phone app in any way.
- Region, grid-size, or count-threshold rows.
- Any interaction (tap, settings), or any show/hide preference.
- Control the host's placement of the content list.

## Decisions

### Decision: Draw the legend as a car-native osmdroid `Overlay`, not a `View`

Add a `CarLegendOverlay : Overlay` in `:car`, registered in
`OsmdroidCarMapRenderer` after the strike and own-location overlays, so it draws
last (above the dim scrim).

- **Why**: The car already uses this exact pattern (`MapDimOverlay`) for
  surface-drawn pixels; it avoids the `View`/measure/layout lifecycle and the
  `Presentation` teardown/rebuild on every `onSurfaceAvailable`. It needs no new
  data wiring: it reads `strikeListOverlay.colorHandler` / `.parameters` at draw
  time and is repainted by the existing `mapView.invalidate()` calls.
- **Alternative considered**: Wrap the `Presentation` content in a
  `FrameLayout(mapView + legendView)` and reuse `LegendView`. Rejected: `:car`
  cannot reach `LegendView`, and reusing it would drag `TabletAwareView`, the
  `tablet_scaleable` styleable, and several `:app` resources down into `:core`.
- **Alternative considered**: Extract a pure legend renderer into `:core` shared
  by phone and car. Rejected for now: more surface area than the value, and the
  car wants a visually different panel anyway. Revisit if a third drawing host
  appears.

### Decision: Colour-scale rows only

Render one row per colour band from `colorHandler.colors` and
`parameters.intervalDuration`, using the phone's `LEGEND_FORMAT` logic
(`"%c %d %s"`): `< minutesPerColor min` per band, the last band `> (n-1)*minutesPerColor min`.

- **Why**: The car map shows strikes only; region, grid size and count threshold
  are noise on a small surface and are not part of the request.
- **Alternative considered**: Show the full phone legend. Rejected: adds rows the
  car map does not need and eats scarce surface area.

### Decision: Car-tuned panel, dpi-scaled text

Draw a dark translucent rounded panel behind the rows with white text, its
metrics scaled from the surface dpi rather than the phone's `ScreenScaling`.

- **Why**: A dark panel with light text stays glanceable over both light
  (`MAPNIK`) tiles and a dimmed night map; the phone's translucent text over the
  map is not legible enough in a car. The `Presentation` runs on a
  `VirtualDisplay` whose dpi is the surface's, while its resource density comes
  from `carContext`, so dp computed from resources may not match the surface —
  scale from the surface dpi instead.
- **Alternative considered**: Reuse the phone's translucent background + alpha
  150. Rejected: not legible enough at a glance while driving.

### Decision: Anchor the legend to the host's visible/stable map area

Forward `SurfaceCallback.onVisibleAreaChanged` / `onStableAreaChanged` through
`CarMapSurfaceCallback` and `CarMapRenderer` to the legend, and draw the panel at
the top-left of the reported area (inset), preferring the stable area and
falling back to the visible area, then to the raw surface corner.

- **Why**: `MapWithContentTemplate` draws the alert list as a panel over the map
  surface; on the AAOS emulator it covers the left side, so a legend at the raw
  surface's top-left is completely hidden. The host publishes the unobscured
  area, which is exactly what the legend must be bounded by to stay visible.
  Preferring the stable area keeps the legend from moving when transient host UI
  appears, while still clearing the persistent content panel.
- **Alternative considered**: Draw at the top-right or hard-code a left inset for
  the content panel. Rejected: the panel side/size is host-owned and not exposed,
  so any fixed guess can be wrong; the visible-area callback is the supported
  channel.
- **Alternative considered**: Leave the legend at the raw corner and accept the
  overlap. Rejected: on the emulator the legend was not visible at all.

### Decision: Always drawn, never interactive, no preference

The overlay is always registered and always draws; it has no listener and no
`CarSettings` entry.

- **Why**: This matches the phone legend's always-present behaviour and the
  driver cannot act on a tap on the map surface anyway (touches go to
  `CarMapSurfaceCallback`).
- **Alternative considered**: A `CarSettings` toggle like `MapDimMode`. Rejected:
  extra preference surface for no requested benefit.
- **Alternative considered**: Hide the legend until strike data arrives. Noted as
  an option; the requirement as written shows it whenever the surface is up,
  mirroring the phone. If pre-data labels (interval not yet set) look wrong on the
  emulator, hiding until the first result is the fallback.

### Decision: New car-only legend string

Add a `car_legend_*` string (a `"%c %d %s"`-style format) to
`car/src/main/res/values/strings.xml`; the car module has no translations yet, so
this ships English-only and is translated later.

- **Why**: `unit_minute` and the region strings are `:app` resources the car
  cannot see; the car module already owns its own small string set.

## Risks / Trade-offs

- **The host may overlay the content list over the top-left legend** → On the
  emulator the list covers the left of the surface, hiding a raw-corner legend.
  Mitigation: anchor the legend to the host's reported visible/stable area, keep
  it small, and verify on the AAOS emulator.
- **The visible/stable callbacks may not fire on some hosts** → The legend falls
  back to the visible area then the raw surface corner, so it still draws (and
  remains usable) when an area is never reported; a `Rect.isEmpty()` report is
  treated as unknown.
- **dpi scaling mismatch** → Text scaled from `carContext` resources may not match
  the `VirtualDisplay` density. Mitigation: derive metrics from the surface dpi
  and verify on-device.
- **Pre-data legend** → Before the first result the active interval may be the
  default, so labels may not reflect the driver's interval yet. Mitigation: hide
  the legend until the first strike data if the emulator shows this as confusing.
- **Phone is untouched** → Guaranteed by drawing nothing from `:app` and changing
  no shared code; the `car-integration` "Phone experience is unchanged"
  requirement holds without modification.

## Migration Plan

Code-only change in `:car` with unit tests; no stored state, preference keys, or
data migrations. Rollback is a straight revert.
