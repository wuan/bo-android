# Proposal

## Why

Google Play reports **"Edge-to-edge may not display for all users"** for releases targeting SDK 35+. The app targets SDK 37, so Android 15+ forces edge-to-edge whether the app is ready or not, yet the window layout is inconsistent: the app opts in with `WindowCompat.setDecorFitsSystemWindows(window, false)` but then re-insets everything with `android:fitsSystemWindows="true"` on the root layouts, never uses the recommended `enableEdgeToEdge()`, and leaves the system bars untransparent on pre-Android-15 devices. The map therefore never actually fills the screen, and the result varies by OS version, navigation mode, and display cutout.

## What Changes

- Adopt `enableEdgeToEdge()` in `Main` and `SettingsActivity`, replacing the manual `setDecorFitsSystemWindows(false)` calls, so edge-to-edge is backward compatible on API 23–34 as well as Android 15+.
- Make the main map truly full-bleed: remove root `fitsSystemWindows` and apply system-bar and display-cutout insets individually to the top status row and the map-overlay controls through a single root insets listener.
- Remove the duplicated inset handling on the settings screen (root `fitsSystemWindows` plus the RecyclerView listener).
- Compensate the osmdroid scale-bar and copyright overlays, whose offsets are computed in raw pixels inside the map canvas, so they clear the navigation bar.
- Keep right/left-anchored controls clear of the display cutout in landscape.

## Capabilities

### New Capabilities
- `edge-to-edge-display`: how the app window is laid out relative to system bars and window insets, including backward-compatible edge-to-edge enablement and per-surface inset application.

### Modified Capabilities
<!-- No existing spec-level behavior changes; this introduces a new capability. -->

## Impact

- **Code**: `app/src/main/java/org/blitzortung/android/app/Main.kt`, `app/src/main/java/org/blitzortung/android/settings/SettingsActivity.kt`, `app/src/main/java/org/blitzortung/android/settings/SettingsFragment.kt`, `app/src/main/java/org/blitzortung/android/map/MapFragment.kt`.
- **Layouts**: `app/src/main/res/layout/main.xml`, `app/src/main/res/layout/activity_settings.xml`.
- **Theme**: `app/src/main/res/values/styles.xml` (`AppTheme`) system-bar treatment.
- **Dependencies**: none added — `androidx.core` 1.19.0 and `androidx.activity` 1.13.0 already provide `WindowCompat.enableEdgeToEdge` / `enableEdgeToEdge()`.
- **Out of scope**: `automotive` (its `CarAppActivity` uses a platform theme and the Car App library handles insets) and `car` (templated, no activity).
- **Verification**: Android 15/16 with gesture and 3-button navigation, a pre-Android-15 device, a display cutout in landscape, and a tablet.
