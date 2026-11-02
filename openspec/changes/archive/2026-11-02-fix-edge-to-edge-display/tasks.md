# Tasks

## 1. Enable edge-to-edge in activities

- [x] 1.1 Replace `WindowCompat.setDecorFitsSystemWindows(window, false)` with the `enableEdgeToEdge()` extension in `Main.kt` (in `onCreate`, before `setContentView`); verify on an API 34 emulator that the main screen draws behind the status and navigation bars.
- [x] 1.2 Replace `WindowCompat.setDecorFitsSystemWindows(window, false)` with `enableEdgeToEdge()` in `SettingsActivity.kt`; verify on an API 34 emulator that the settings screen draws behind the system bars.
- [x] 1.3 Confirm no `setDecorFitsSystemWindows` call sites remain (`grep -rn setDecorFitsSystemWindows app/src/main/java` returns nothing) and that `./gradlew :app:assembleDebug` succeeds.
- [x] 1.4 Document the convention in `AGENTS.md` (new activities call `enableEdgeToEdge()` and apply insets once per screen); verify the note names the API and the single-application rule.

## 2. Main screen full-bleed layout and insets

- [x] 2.1 Remove `android:fitsSystemWindows="true"` from the root of `main.xml`; verify `git grep -n fitsSystemWindows app/src/main/res` no longer lists `main.xml`.
- [x] 2.2 Add a single root `ViewCompat.setOnApplyWindowInsetsListener` in `Main.kt` applying `systemBars() or displayCutout()` insets to `upper_row` (top + horizontal) and `map_overlay` (horizontal + bottom) as padding; verify on an emulator that the status text and the top/bottom/side controls clear the bars while map imagery shows behind them.
- [x] 2.3 Resolve the `map_overlay` geometry per design D4 (inspect laid-out bounds; choose fill-parent vs bottom inset on `time_slider`); verify the time slider and its stacked controls sit fully above the navigation bar in three-button navigation.
- [x] 2.4 Verify the button column and the edge-anchored controls (`RegionView`, `HistogramView`, `AlarmView`, `LegendView`) stay clear of the display cutout on a cutout emulator in landscape.

## 3. Map-canvas overlays

- [x] 3.1 Add the bottom system-bar inset to the osmdroid scale-bar and copyright offsets in `MapFragment.kt`; verify both are fully visible above the navigation bar in portrait and landscape.

## 4. Settings screen single inset

- [x] 4.1 Remove `android:fitsSystemWindows="true"` from `activity_settings.xml` and confirm the existing RecyclerView insets listener is the only application point; verify the settings list has no extra gap and its last item is reachable above the navigation bar.

## 5. System bar icon styling

- [x] 5.1 Check icon contrast against light and dark map types in both system light and dark modes; if contrast is insufficient, pass an explicit `SystemBarStyle` consistent with the app's light-on-dark controls and re-verify legibility.

## 6. Integration verification

- [x] 6.1 Run the full test suite (`./gradlew testDebugUnitTest`) and confirm it passes.
- [x] 6.2 Walk the display matrix — Android 15/16 in gesture and three-button navigation, a pre-Android-15 device (API 23/29/33), a display cutout in landscape, and a tablet — and confirm no control is obscured and the map fills the window on each.
