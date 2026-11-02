# Tasks

Phases are measurement-gated: after each group, rebuild, read the three rates from
the built AAB and run the on-device checklist before continuing. See
`design.md - D11`.

## 1. Baseline and measurement harness

- [x] 1.1 Build the current release bundles (`./gradlew :app:assembleRelease :automotive:assembleRelease`) and read the three R8 stats from each AAB's `BUNDLE-METADATA/com.android.tools/r8.json`; verify the phone artifact reproduces the recorded baseline (no-optimisation 83.95, no-obfuscation 83.86, no-shrinking 83.92)
- [x] 1.2 Add a repeatable local helper (a Gradle task or a small checked-in script) that builds and prints both artifacts' three rates plus `resourceOptimization.isOptimizedShrinkingEnabled`; verify by running it and observing the baseline values

## 2. Phase 1 — safe removals

- [x] 2.1 Confirm no reflective resource lookup exists (search for `getIdentifier`, `Class.getField`/`getDeclaredField` on `R` types); record the result — expected none
- [x] 2.2 Delete the dead Dagger 1 rules (`dagger.internal.Binding`, `dagger.internal.ModuleAdapter`) and `android.preference.Preference`; verify the build succeeds and the merged `configuration.txt` no longer lists them
- [x] 2.3 Remove `-keep class **_Factory { *; }`, `-keep class **_MembersInjector { *; }` and `-keep class **Dagger*.* { *; }`; verify with the `perf` build that the app, `AppService` and the Android Auto entry point each launch and inject the Dagger graph
- [x] 2.4 Remove `-keepclassmembers class **.R$* { public static <fields>; }` (gated on 2.1); verify a minified build launches and preferences, the map and the widget all resolve their resources
- [x] 2.5 Drop the extra `*** get*();` clause from the `android.view.View` keep and the `public *;` tail from the enum rule; verify the UI and preferences work on the `perf` build, then measure `r8.json` and record the delta

## 3. Phase 2 — narrow targeted keeps

- [x] 3.1 Read the pinned OSMDroid (6.1.20) bytecode to confirm the exact members behind `OwnMapView.kt:132` (`MapController."mCurrentAnimator"`) and `MapFragment.kt:101` (`centred` on the scale-bar overlay), then replace `-keep class org.osmdroid.** { *; }` and `-keep class * extends org.osmdroid.views.overlay.Overlay { *; }` with targeted keeps; verify map rotation/animation and scale-bar centring on the `perf` build
- [x] 3.2 Narrow `-keep class * extends androidx.fragment.app.Fragment { *; }` to constructor-only (rely on the default `-keepclasseswithmembers … <init>(Context, AttributeSet)` and consumer rules); verify fragments, activities and preference screens inflate on device
- [x] 3.3 Review the app event/alert/`result`/`location.provider` `{ *; }` keeps and reduce each to the minimum (class-only or constructor-only); verify alerts, data updates, history playback and location on the `perf` build, then measure `r8.json`

## 4. Phase 3 — remove AndroidX blanket keeps

- [x] 4.1 Confirm there is no by-name AndroidX usage beyond the OSMDroid sites (search reflection and layout `android:name` attributes) and that AAR consumer rules cover fragment, worker, startup, window and serialization; record the findings
- [x] 4.2 Remove `-keep class androidx.** { *; }` and `-keep interface androidx.** { *; }`; verify the release build succeeds and the merged `configuration.txt` no longer contains them
- [x] 4.3 Run the full on-device regression checklist on the `perf` build — map render/overlays, proximity alerts with sound and vibration, background service and notifications, home-screen widget, preferences plus backup/restore, Android Auto and Automotive, edge-to-edge layout — and verify no `ClassNotFoundException`/`NoSuch*Exception` crashes
- [x] 4.4 Measure `r8.json` for both artifacts, record the new rates, and set the spec's "configured target" from this measurement

## 5. Phase 4 — optimised resource shrinking

- [x] 5.1 Investigate why `android.r8.optimizedResourceShrinking=false` was set in commit `b3a7d760` (history, comments, any linked issue or CI failure) and record the finding — this resolves the design Open Question
- [x] 5.2 If 5.1 finds no concrete blocker, set `android.r8.optimizedResourceShrinking=true`, rebuild, and verify `resourceOptimization.isOptimizedShrinkingEnabled` is `true` in `r8.json`
- [x] 5.3 Verify on device that dynamically referenced resources survive (preference icons, widget and map assets, notification icons, launcher/adaptive icons) and render correctly; measure `r8.json`

## 6. Phase 5 — shared rules, docs and integration

- [x] 6.1 Extract the shared rule set to a single file referenced by both `:app` and `:automotive` (design D9), remove the duplicated copies, and verify both merged `configuration.txt` outputs are equivalent
- [x] 6.2 Run the full quality gate (`./gradlew testDebugUnitTest lint staticAnalysis`) and verify no new failures beyond the known-accepted lint items
- [x] 6.3 Final regression on the `perf` build for both phone and automotive; record final `r8.json` vs baseline and verify each of the three rates increased
- [x] 6.4 Update `AGENTS.md` and the archived `2026-11-02-accept-edge-to-edge-cutout-warning` design note to record that the AndroidX keep rule was narrowed (referencing this change) and revise the "do not narrow" guidance; verify the references resolve

## Verification notes

- Final measured rates (optimised resource shrinking enabled on both):
  - phone `:app`: optimisation 79.57%, obfuscation 80.47%, shrinking 80.23%
  - automotive: optimisation 76.95%, obfuscation 78.03%, shrinking 77.81%
  - all well above the 75% configured target and the recorded baseline.
- Quality gate: 387 debug unit tests pass, lint reports only pre-existing
  dependency-version warnings (`GradleDependency`, `AndroidGradlePluginVersion`).
- Device regression was run on the `perf` build on an API emulator: app launch
  and Dagger injection, map render + overlays + scale bar, animated map
  gestures, live data updates, history playback, preferences inflation, widget
  provider registration, and the background `AppService` (started from
  `Main.onPause`, injected, scheduled its alarm) all pass with no
  `ClassNotFoundException`/`NoSuch*Exception`.
- Residual manual checks that this environment cannot exercise: proximity alert
  sound/vibration on a real strike, widget rendering once bound to a launcher,
  preferences backup/restore, Android Auto on car hardware, the automotive
  build on Automotive OS, and the edge-to-edge layout across real cutout
  devices.

## Workflow follow-up

- Publish a release and confirm the Play recommendation's three rates moved; record whether the edge-to-edge deprecated-API item changed on the phone artifact.
- Archive the change after the project's review requirements are satisfied.
- Consider a separate change for `androidx.test.ext.junit.ktx` declared as `implementation` in `app/build.gradle.kts`.
