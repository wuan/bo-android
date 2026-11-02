# Design

## Context

See `proposal.md - Why` for motivation. The constraints that shape the approach:

- Views-based app (no Compose), `minSdk 23`, `targetSdk 37`, so Android 15+ already forces edge-to-edge.
- `Main` is a `FragmentActivity`, `SettingsActivity`/`SettingsFragment` are AppCompat; both activities currently call `WindowCompat.setDecorFitsSystemWindows(window, false)`.
- Root layouts (`main.xml`, `activity_settings.xml`) set `android:fitsSystemWindows="true"`, which consumes insets before any code sees them.
- The main screen's controls are laid out with `RelativeLayout`; the button column is repositioned **in code** (`ButtonColumnHandler.updateButtonColumn()` rebuilds `RelativeLayout.LayoutParams` with `ALIGN_PARENT_TOP`/`ALIGN_PARENT_RIGHT` on every update), so any margin set in XML is discarded.
- osmdroid's scale bar and copyright are drawn inside the map canvas with pixel offsets derived from `displayMetrics`, not from view insets.
- `AppTheme` is a fixed light Material Components theme (not DayNight), so the app's own styling assumes light-on-dark controls regardless of the system dark setting.

## Goals / Non-Goals

**Goals:**
- One consistent edge-to-edge layout across API 23–37, verified on Android 15/16 and older devices.
- The map genuinely fills the window; controls are individually inset.
- Exactly one inset application per screen.
- Silence the Play Console "may not display for all users" warning by adopting the recommended API.

**Non-Goals:**
- Restyling the app or introducing a DayNight theme.
- Changing the `automotive` `CarAppActivity` (platform theme; Car App library owns insets) or the `car` templated module.
- Resolving the separate "deprecated APIs for edge-to-edge" warning, which originates in AppCompat/Material internals.

## Decisions

### D1: Enable edge-to-edge with `enableEdgeToEdge()`
Replace `WindowCompat.setDecorFitsSystemWindows(window, false)` in both activities with the `androidx.activity.enableEdgeToEdge()` extension, called in `onCreate` before `setContentView`. It encapsulates the ~100 lines needed for true backward compatibility (transparent bars, icon appearance) on API 23–34, and is the API Play's analyzer looks for.

*Alternatives considered:* `WindowCompat.enableEdgeToEdge(window)` (equivalent, core 1.17+) — acceptable but the activity extension is the documented default; keeping `setDecorFitsSystemWindows` (current state) — rejected, it is what triggered the warning and does not configure pre-Android-15 bars.

### D2: Make the map full-bleed and distribute insets from one root listener
Remove `android:fitsSystemWindows="true"` from `main.xml`. Install a single `ViewCompat.setOnApplyWindowInsetsListener` on the root that reads `WindowInsetsCompat.Type.systemBars() or Type.displayCutout()` and applies:

- `upper_row` (top status row): top and horizontal insets as padding.
- `map_overlay` (controls container): horizontal and bottom insets as padding.

`map_view` stays `match_parent` and is never inset, so the map draws behind both bars. The vertical insets are applied to the two containers rather than the root so the map itself is not clipped.

*Alternatives considered:* keep `fitsSystemWindows` on the root (the current "letterbox") — rejected: the map never reaches the edges, and `fitsSystemWindows` insets on all edges and defeats edge-to-edge; per-control margins — rejected because `ButtonColumnHandler` overwrites `LayoutParams`, so margins would be lost.

### D3: Inset the right/left-anchored controls via parent padding, not child margins
Because `updateButtonColumn()` replaces each button's `LayoutParams`, the button column inherits `map_overlay`'s padding for edge anchoring (`ALIGN_PARENT_RIGHT` aligns to the padding edge). The same parent padding keeps `RegionView`, `HistogramView`, `AlarmView`, and `LegendView` clear of the display cutout in landscape, so no controller code needs to know about insets.

*Alternatives considered:* threading inset values into `ButtonColumnHandler` — rejected as unnecessary coupling; wrapping the buttons in a new container — rejected as a larger layout change for no benefit.

### D4: Apply the bottom inset where it reliably lands
`map_overlay` is `match_parent` height while also `layout_below="@id/upper_row"`. If its measured bottom extends past the window, bottom padding would be measured from an off-screen edge. Resolve this before finalizing: either give `map_overlay` both `alignParentTop`/`alignParentBottom` (fill the window) so padding behaves predictably, or apply the bottom inset directly to `time_slider`. The chosen variant must be confirmed by inspecting the laid-out bounds, then the other controls that anchor to `time_slider` follow for free.

*Alternatives considered:* guessing from `RelativeLayout` semantics — rejected; this is verifiable in minutes and the wrong choice produces a subtle off-screen inset.

### D5: Bump osmdroid canvas offsets by the navigation-bar inset
`MapFragment` computes the scale-bar and copyright offsets in pixels. Add the bottom system-bar inset to those offsets so they clear the navigation bar. This is the one place insets must be applied outside the view-hierarchy listener.

*Alternatives considered:* moving the overlays into the view hierarchy — rejected as a much larger change to map rendering.

### D6: Remove the duplicate settings inset
Delete `android:fitsSystemWindows="true"` from `activity_settings.xml`; keep the existing `ViewCompat.setOnApplyWindowInsetsListener` on the RecyclerView as the single application point, so the settings list no longer risks double padding.

### D7: System bar icon style
Start with `enableEdgeToEdge()` defaults and verify icon contrast against the app's light and dark map types in both system light and dark modes. If the default (system-driven) mode yields poor contrast — plausible because `AppTheme` is a fixed light theme while the system may be dark — pass an explicit `SystemBarStyle` matching the app's light-on-dark control styling. This only changes a parameter, not the approach.

## Risks / Trade-offs

- **`map_overlay` bottom inset lands off-screen** → mitigate per D4 by verifying laid-out bounds and choosing the container-vs-`time_slider` variant accordingly.
- **Button column margins silently ignored** → avoided by D3 (parent padding).
- **osmdroid overlays collide with the nav bar** → D5.
- **Icon contrast regressions on light map types** → D7 verification; fall back to an explicit `SystemBarStyle`.
- **Pre-Android-15 visual change**: today the bars are effectively opaque-ish, so older devices will *start* drawing edge-to-edge. This is intentional but is the highest-regression area → cover with the test matrix (API 23, 29, 33).
- **Theme is not DayNight** → system dark mode with a light app can look inconsistent; explicitly out of scope, noted as a follow-up.
- **Sibling Play warning** ("deprecated APIs for edge-to-edge") is expected to appear and is not caused by this change.

## Migration Plan

UI-only change; no data or preference migration. Ship in a normal release. Rollback is a revert of the commit(s) — `fitsSystemWindows` and `setDecorFitsSystemWindows` are restored together. No remote or staged rollout concerns.

## Open Questions

- Exact `SystemBarStyle` (fixed light vs system-driven icons) — decided by visual check during implementation (D7).
- Whether to later adopt a DayNight theme and dynamic system-bar styling — deferred; not needed to satisfy the specs.
