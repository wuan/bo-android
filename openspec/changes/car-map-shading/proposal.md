# Proposal

## Why

The car map surface is app-drawn and always shows the same MAPNIK tiles, yet it is
only shaded in dark mode: light mode shows an untouched, visually busy map where
the lightning data competes with roads and labels. The phone app never shows an
unshaded map — its two map modes are both shading treatments over MAPNIK (a dark
scrim with bright strikes, or a bright scrim with darkened strikes) — and the car
should offer the same always-shaded behaviour in both host themes.

## What Changes

- Replace the three dimming modes (Follow car / On / Off) with three **shading**
  modes: **Follow car** (dark shading in dark theme, bright shading in light
  theme), **Dark map** (always dark shading) and **Bright map** (always bright
  shading). There is **no unshaded mode**.
- Shade the map background in two directions: a translucent black scrim for dark
  shading and a translucent white scrim for bright shading. Only the background is
  shaded; strikes and the legend stay untouched.
- Make the **strike palette follow the shading direction**: bright shading uses the
  darker strike palette so the newest (white) strikes remain distinguishable from
  the light background; dark shading keeps the bright palette.
- Give the car its own strike-colour handler with a shading-derived target override,
  so theme changes recolour strikes without mutating the phone's shared colour
  state.
- Rename the whole feature from "dimming" to "shading" in code, strings, and the
  car preference (`map_shading_mode`). No migration is needed: the car shading
  feature has not been released.
- Rewrite the `car-integration` requirements for car map shading, the shading
  preference, and the settings screen.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `car-integration`: replaces the car map dimming requirements with always-on,
  theme-aware dark/bright map shading, a shading preference defaulting to Follow
  car, and the shading choice on the car settings screen.

## Impact

- `:car` — shading mode model and preference, two-direction scrim overlay, renderer
  shading API and strike-palette coupling, map screen state, settings screen rows,
  strings, and unit tests; dimming names and the `Off` mode are removed.
- `:core` — a small target-override seam on the colour handler so the car can pick
  the strike palette without changing the phone's colour target or preferences.
- `:app` / `:automotive` — provide a car-owned strike-colour handler through the
  existing car-dependencies seam.
- No phone UI, data, alert, or phone preference changes.
