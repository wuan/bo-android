# Design

## Context

See `proposal.md` — Why. The current state that shapes the approach:

- `BlitzortungMapScreen.onGetTemplate()` calls
  `renderer.setDarkMode(carContext.isDarkMode)`; `OsmdroidCarMapRenderer` applies
  `TilesOverlay.INVERT_COLORS` to the tile overlay. Dimming is therefore binary,
  automatic, and recolors tiles rather than dimming the surface.
- `BlitzortungCarSession.onCarConfigurationChanged` already invalidates the top
  screen, so a day/night flip already re-renders the map.
- The car experience has a single screen (`BlitzortungMapScreen`) and no settings
  surface; the map header is built in `buildAlertListTemplate()` and its
  `ActionStrip` holds PAN and zoom actions.
- The Car App Library is 1.7.0. It has `ListTemplate`, `Row` (with `Toggle`,
  `setOnClickListener`, `setImage`), and `Header.addEndHeaderAction`, but **no
  slider (`SeekbarTemplate`)** and no `Chip`, so a continuous value cannot be a
  template control.
- Car dependencies are injected per app module through `CarDependenciesProvider`
  (`AppCarDependencies` in `:app`, `AutomotiveCarDependencies` in `:automotive`),
  and `PreferenceKey`/default prefs live in `:core`.

## Goals / Non-Goals

**Goals:**

- Dim the app-drawn map surface via a translucent overlay, in three modes
  (Follow car / On / Off), default Follow car.
- Expose the setting from an extensible settings screen reachable from the map.
- Keep the setting in a car-only store, independent of the phone app and `:core`.
- Ship the same behavior on Android Auto and Android Automotive OS.

**Non-Goals:**

- A continuous dim level (no slider in the Car App Library; three modes instead).
- Automatic-only behavior with no user control.
- Recoloring map tiles, and any change to the strike/alert rendering rules.
- Any phone UI, data-layer, or alert-computation change.

## Decisions

### Decision: Dim with a translucent overlay, not tile inversion

Add a dedicated osmdroid overlay that draws a translucent black scrim, and place
it as the first entry of `mapView.overlays` so it is drawn above the tiles and
below the strike and grid overlays.

- **Why**: The request is "dimmable like the app", and the app already dims with
  translucent black (`translucent_background`, `doc_overlay_scrim`, `FadeOverlay`).
  An overlay dims the background uniformly and reads as a brightness change; the
  existing `INVERT_COLORS` recolor reads as a different map, not a dimmed one.
  osmdroid draws the tile overlay before iterating `mapView.overlays`, so a
  front-of-list overlay covers the tiles only and keeps the lightning data
  readable — which matters most at night.
- **Alternative considered**: Keep/parameterize tile inversion. Rejected: it
  changes map colors rather than dimming, and was never what the driver could
  perceive as a dim control.
- **Alternative considered**: Color matrix over tiles only. Rejected: more code
  for a less natural result than a plain scrim.
- **Alternative considered**: A `View` scrim on top of the whole `MapView`.
  Rejected after implementation: it also dims the strike and grid layers, hiding
  the data the app exists to show.

### Decision: Three modes, not a slider

Store a `MapDimMode` of `FOLLOW_CAR` / `ON` / `OFF`, default `FOLLOW_CAR`.

- **Why**: 1.7.0 has no slider template, so a continuous value is not available
  in the templated UI; three modes cover night glare (On), off (Off), and the
  sensible default that tracks the car (Follow car).
- **Alternative considered**: A single on/off toggle plus auto. Rejected: three
  explicit modes are clearer in a settings list and extend the same way.
- **Alternative considered**: Three dim *strengths*. Not adopted; the modes are
  the three options. (If strengths are wanted later, they are an added setting,
  not a change to this one.)
- Store the mode as a string so adding modes later is data-compatible.

### Decision: Follow car keys off `carContext.isDarkMode`

The effective dim state is `mode == ON || (mode == FOLLOW_CAR &&
carContext.isDarkMode)`. Reuse the existing `onCarConfigurationChanged`
invalidation so theme flips refresh the map.

- **Why**: The host already reports the theme and already triggers a redraw, so
  Follow car needs no new signal and mirrors the current automatic trigger.

### Decision: Car-only preferences behind a small store, observed by the screen

Add a `CarSettings` accessor backed by a dedicated `SharedPreferences` file
(e.g. `car_settings`, key `map_dim_mode`), read by both the settings screen and
the renderer, and expose changes (listener or `Flow`) so the map screen can
`invalidate()` after a change. Provide it through `CarDependenciesProvider`.

- **Why**: The requirement is car-only persistence; a dedicated file keeps the
  phone's default prefs and `:core`'s `PreferenceKey` untouched, and the existing
  provider seam is where per-app-module wiring already lives.
- **Alternative considered**: Reuse `:core` default prefs / `PreferenceKey`.
  Rejected: leaks a car setting into the phone namespace.
- **Alternative considered**: Fetch the mode once per `onGetTemplate`. Rejected:
  the map must refresh when the setting changes while the map screen is alive.

### Decision: Settings entry is a header end action on the map

Add a gear `Action` via `Header.addEndHeaderAction` in
`buildAlertListTemplate()`, pushing a `BlitzortungSettingsScreen`.

- **Why**: Header actions render as icons in map templates (labels are stripped),
  and the header is the conventional place for a settings affordance without
  mixing it into the map's PAN/zoom action strip.
- **Alternative considered**: A gear in the `ActionStrip`. Rejected: the strip is
  map controls; a settings entry there is confusing and space-constrained.

### Decision: Settings screen lists settings; a setting opens its choices

`BlitzortungSettingsScreen` is a `ListTemplate` of setting rows; a row shows the
current value and opens a `ListTemplate` of choices (the three modes). Selecting a
mode writes `CarSettings` and pops back.

- **Why**: Keeps the top-level list compact and scales as settings are added,
  matching the "settings will grow" direction.
- **Alternative considered**: Inline check-marked radio rows on one screen.
  Rejected as the default because it couples the list layout to a single-choice
  setting; a value screen per setting composes better as rows accumulate.

## Risks / Trade-offs

- **Fixed scrim alpha may read too strong or too weak** → Tune the constant; it is
  not part of the behavior contract, so no spec change is needed.
- **The scrim must not dim the data layers** → Resolved by placing the overlay
  beneath the strike and grid overlays; a renderer test asserts the dim overlay
  precedes the strike overlay so a future reordering cannot silently regress it.
- **Header action visibility in map templates** → Icons render but labels are
  stripped; use an icon-only gear and verify it appears on the AAOS emulator,
  consistent with the header-countdown finding.
- **Threading of preference change notification** → SharedPreferences listeners
  fire on the main thread; only forward to `invalidate()`/scrim alpha, no work off
  the main thread.
- **Removing `setDarkMode`** → The renderer interface changes (`setDarkMode` →
  a dimming call). Any callers/tests referencing it must be updated in the same
  change.

## Migration Plan

Code-only change in `:car` (plus wiring in `:app`/`:automotive`); no stored-state
migration, and no existing user preference is read or reinterpreted. The new
preference defaults to Follow car, matching the intent of today's automatic
behavior. Rollback is a straight revert.
