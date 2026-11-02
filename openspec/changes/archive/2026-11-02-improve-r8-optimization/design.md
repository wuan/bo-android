# Design

## Context

See `proposal.md - Why` for motivation. The constraints that shape the approach:

- **Build stack:** AGP 9.4.1, Gradle 8.13, Kotlin 2.2.10, R8 full mode (default
  since AGP 8.0). `release` sets `isMinifyEnabled = true` and
  `isShrinkResources = true` with `proguard-android-optimize.txt` plus the app's
  `proguard-rules.pro`. A `perf` build type (`initWith(release)`,
  `isDebuggable = false`, debug-signed, external baseline profiles excluded) is
  the established vehicle for on-device release-like testing.
- **Two artifacts share one `applicationId`** (`org.blitzortung.android.app`):
  the phone `app` module and the `automotive` module. Today
  `app/proguard-rules.pro` and `automotive/proguard-rules.pro` are byte-for-byte
  copies, and both carry the blanket keeps.
- **`gradle.properties` compat flags** were added in `b3a7d760` as an AGP 9
  migration batch: `android.r8.optimizedResourceShrinking=false`,
  `android.r8.strictFullModeForKeepRules=false`, plus unrelated `android.newDsl`
  / `builtInKotlin` / `enableAppCompileTimeRClass` toggles.

### Baseline, measured from the last release artifacts

The shipped AAB carries `BUNDLE-METADATA/com.android.tools/r8.json`, which is the
exact input to the Play recommendation:

```
"stats": {
    "noObfuscationPercentage":  83.86   →  16.14%
    "noOptimizationPercentage": 83.95   →  16.05%
    "noShrinkingPercentage":    83.92   →  16.08%
},
"resourceOptimization": { "isOptimizedShrinkingEnabled": false }
```

R8's own keep-radius report (`app/build/outputs/mapping/release/`, decoded from
`configanalyzer.pb` against `com.android.tools.r8.keepradius.proto`) attributes
the retained mass as follows (total = 94 270 rule-kept items; 9 928 live classes,
12 104 input classes):

```
   TOTAL    cls    fld    mth  ORIGIN                    RULE
  64429   5141  21485  37803  proguard-rules.pro:49     -keep class androidx.** { *; }       ← 68.3%
  10329      0  10329      0  proguard-rules.pro:166    -keepclassmembers **.R$* { fields }  ← 11.0%
   4677    331   1443   2903  proguard-rules.pro:77     -keep class org.osmdroid.** { *; }
   3144    164      0   2980  proguard-rules.pro:67     -keep … extends android.view.View { … }
   2647      0      0   2647  proguard-android-optimize.txt:34   (default) View set*/get*
   2397    681    328   1388  proguard-rules.pro:50     -keep interface androidx.** { *; }
   1055     34    393    628  proguard-rules.pro:81     -keep … extends …Overlay { *; }
   1021      0    678    343  proguard-rules.pro:85     -keepclassmembers enum * { public *; }
    893     21    309    563  proguard-rules.pro:118    -keep … extends …Fragment { *; }
    750    375      0    375  proguard-rules.pro:59,62  -keepclasseswithmembers … <init>(Ctx,Attrs[,int])
    261     35     51    175  proguard-rules.pro:31     -keep class **_Factory { *; }
   ─────────────────────────────────────────────────────────────────────────────────────────
   ~94% of retained code originates in the app's own file, not in library or default rules.
```

### What genuinely needs keeping

- **No `Class.forName` anywhere** in `app`, `core` or `car`. The only dynamic
  name resolution is layout inflation and framework/appwidget instantiation.
- **Two reflection sites only**, both app-side field reads into OSMDroid:
  `map/OwnMapView.kt:132` (`MapController."mCurrentAnimator"`) and
  `map/MapFragment.kt:101` (`centred` on the scale-bar overlay).
- **AndroidX and other libraries already ship consumer rules** for their own
  reflection — visible in the merged config: fragment constructors,
  `androidx.work.ListenableWorker`, `androidx.startup.Initializer`,
  `androidx.window.layout.SidecarCompat`, `kotlinx.serialization`, media
  `Parcelable`, `androidx-annotations.pro`, coroutines. The blanket keeps
  duplicate and vastly exceed these.
- **JSON beans do not need name-keeping**: parsing is manual (`org.json` /
  `DataBuilder`), not reflective. `Serializable` beans only need the default
  `-keepclassmembers … implements java.io.Serializable` handling.

### The edge-to-edge interaction

The archived change `2026-11-02-accept-edge-to-edge-cutout-warning` (design D2/D3)
concluded that `-keep class androidx.** { *; }` is the reason the Play
deprecated-API warning (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`) persists, and
rejected narrowing the rule *while the warning is non-blocking*. This change is a
second Play finding driven by the same rule. Note the outcomes are not identical:
lifting the keep should clear the dead reference in the **automotive** bundle, but
the **phone** bundle keeps a live `enableEdgeToEdge()` reference, so the
deprecated-API item may persist there. The two findings overlap but are not the
same target. `openspec/specs/edge-to-edge-display/spec.md` encodes behavioural
layout outcomes and is unchanged by this work.

## Goals / Non-Goals

**Goals:**

- Make R8 shrink, obfuscate and optimise dependency code unless a documented
  dependency requires otherwise, and enable optimised resource shrinking.
- Leave a durable, locally reproducible measurement loop (read `r8.json` from the
  built bundle) so future regressions are caught without a store round-trip.
- Keep the phone and automotive artifacts on one rule set.
- Preserve every reflection/inflation/entry-point behaviour the current blanket
  rules were protecting, verified on a minified build.

**Non-Goals:**

- Changing `android.r8.strictFullModeForKeepRules` (see D8).
- Chasing a specific numeric target as a hard requirement in a single step; the
  target is applied per phase with measurement gates (see D11).
- Removing the edge-to-edge deprecated-API warning as an explicit deliverable —
  it may follow incidentally, but the phone path keeps a live reference.
- Fixing the `androidx.test.ext.junit.ktx`-as-`implementation` issue; noted in
  the proposal as a separate change.
- Any change to runtime behaviour, preferences, permissions or data.

## Decisions

### D1: Remove the AndroidX blanket keeps rather than soften them

Remove `-keep class androidx.** { *; }` and `-keep interface androidx.** { *; }`
entirely. Rely on (a) AndroidX AAR consumer rules, (b) AGP defaults, and (c) the
app's own targeted constructor keeps.

*Alternatives:*

- **`-keep,allowobfuscation,allowshrinking class androidx.**`** — keeps the door
  open to retain the whole namespace as a fallback; still pins *optimisation* of
  every AndroidX member, so it leaves a large share of the metric on the table.
  Rejected as the primary target.
- **`-keep class androidx.** { <init>(); }`** — unbounded class-name retention
  with no shrink benefit. Rejected.

The evidence (68.3% of all retained code, and only 4.1% of AndroidX classes
obfuscated today) makes this the single highest-value change.

### D2: Replace the OSMDroid blanket keep with two targeted keeps

`-keep class org.osmdroid.** { *; }` (4 677) and
`-keep class * extends …Overlay { *; }` (1 055) are replaced by keeps for the two
reflective field reads and the map/overlay constructors, e.g.:

```
-keepclassmembers class org.osmdroid.views.MapController { *** mCurrentAnimator; }
-keepclassmembers class org.osmdroid.views.overlay.ScaleBarOverlay { boolean centred; }
```

Exact members to be confirmed from the bytecode of the pinned OSMDroid version
(6.1.20) during implementation. This is the *fragile* kind of reflection
(private field reads) and is arguably a latent bug independent of R8; a targeted
keep documents the contract.

### D3: Remove the `**.R$*` field keep

`-keepclassmembers class **.R$* { public static <fields>; }` pins 10 329 fields
(11% of retained code). With `android.nonTransitiveRClass=true` and resource
references resolved at compile time, R8 inlines `R` constants; the rule is legacy.
*Guard:* grep for reflective resource lookup (`getIdentifier`, `getField` on `R`)
before removing; none is expected.

### D4: Drop the extra `*** get*()` on `android.view.View`

The app rule adds `*** get*();` beyond the default `set*(***)`. Property getters
matched there block inlining/optimisation (3 144 methods). Keep the constructors
and `set*`; drop `get*` unless a concrete need appears.

### D5: Remove Dagger keeps that full mode makes redundant

`-keep class **_Factory { *; }` (261), `**_MembersInjector { *; }` (123) and
`**Dagger*.* { *; }` are unnecessary: KSP-generated Dagger 2 code is referenced
statically (e.g. `DaggerAppComponent`), so R8 keeps what is used; nothing
reflectively instantiates the generated graph. Also delete the Dagger 1 relics
(`dagger.internal.Binding`, `dagger.internal.ModuleAdapter`) and
`android.preference.Preference`, which match nothing.

### D6: Narrow the Fragment/Overlay and app-event keeps

`-keep class * extends androidx.fragment.app.Fragment { *; }` (893) and the
`Overlay` rule (1 055) keep all members; their default/consumer counterparts
already retain constructors. Where the app instantiates these by name from XML,
keep constructors only. The app event/alert/`result`/`location.provider` `{ *; }`
rules are reviewed individually; most only need to survive as classes, not with
all members.

### D7: Enable optimised resource shrinking

Set `android.r8.optimizedResourceShrinking=true` (AGP 9 default). Directly
addresses `"isOptimizedShrinkingEnabled": false` in the bundle metadata.
*Prerequisite:* determine why it was set to `false` in `b3a7d760` (see Open
Questions). Watch for the known class of issues (final resource IDs, dynamic
resource access).

### D8: Leave `strictFullModeForKeepRules=false` unchanged

The flag was set to restore pre-AGP-9 keep-rule semantics, likely to avoid a
startup crash. Its metric payoff is uncertain and it risks reintroducing exactly
that class of crash. Out of scope; can be revisited once the rule set is narrow
and stable.

### D9: Share one rule set between phone and automotive

Options considered:

1. **Move shared rules into `core`'s `consumerProguardFiles`** — `core` is an
   Android library consumed by both apps; a consumer rule file propagates to
   both. Clean, but exposes the rules to any future consumer of `core`.
2. **A single root file referenced by both `proguardFiles(...)` calls** (e.g.
   `config/proguard/shared-rules.pro`) plus a thin per-module file for
   module-specific rules. Explicit and easy to reason about.

Prefer (2): it keeps R8 configuration visible at the app build-type level and
avoids coupling it to library publication. Either satisfies the shared-config
requirement.

### D10: Local verification loop

`./gradlew :app:assembleRelease` then read
`BUNDLE-METADATA/com.android.tools/r8.json` from
`app/build/outputs/bundle/release/app-release.aab` and print the three stats. This
is the same measurement used to establish the baseline and needs no Play
round-trip. Optionally script it as a Gradle task or a checked-in helper.

### D11: Phase the change behind measurement gates

Apply in order, rebuilding and reading `r8.json` after each phase, and running the
on-device checklist on the `perf` build at each gate:

```
  Phase 0  Baseline (record 83.86 / 83.95 / 83.92)
  Phase 1  Safe removals: dead rules, dagger keeps, R fields, enum tail, View get*
  Phase 2  Narrow osmdroid + fragment/overlay + app-event keeps to constructors
  Phase 3  Remove the AndroidX blanket keeps        ← largest jump
  Phase 4  Enable optimized resource shrinking
  Phase 5  Share one rule file; final regression + store upload
```

Each phase is independently revertible, which keeps the "which change broke it"
search space small.

## Risks / Trade-offs

- **A removed keep breaks a reflection path at runtime** → Phase the removals;
  keep constructor/targeted keeps for every known entry point; run the `perf`
  build on real devices covering map, alerts, background service, widget,
  preferences backup and Automotive before shipping. Crashes of the form
  `ClassNotFoundException` / `NoSuch*Exception` are the signal.
- **Reflective private-field reads (`mCurrentAnimator`, `centred`) are already
  fragile** and could break on an OSMDroid bump regardless of R8 → targeted keeps
  pin the members by name and documented type.
- **Optimised resource shrinking regressions** (resources removed despite dynamic
  access) → verify dynamic-resource paths (icons, preference defaults) on device;
  revert D7 if a resource goes missing.
- **`mapping.txt` is misleading as a rate source** (it only describes survivors
  and under-reports the "no-op" fraction) → always measure from `r8.json`.
- **Automotive artifact diverges** → D9 removes the copy-paste divergence.
- **Target not reached in one step** → D11's gates make partial progress a valid,
  shippable state; the spec requires each rate to *increase* over baseline, not to
  hit an absolute number in the first release.

## Migration Plan

1. Establish and record the Phase 0 baseline from the current release bundle.
2. Work through Phases 1–5 (D11), rebuilding and measuring after each; run the
   `perf` build checklist at each gate.
3. Rollback: each phase is a self-contained change to `proguard-rules.pro` and/or
   `gradle.properties`; reverting the file restores the prior behaviour. No data,
   schema or preference migration is involved.
4. After a clean regression, publish a release and confirm the Play recommendation
   rates moved; confirm whether the edge-to-edge deprecated-API item changed.

## Open Questions

- Why was `android.r8.optimizedResourceShrinking=false` set in `b3a7d760`?
  **Resolved (task 5.1):** no comment, linked issue or CI failure explains it.
  It was added as one line in a batch of AGP 9 migration compatibility toggles
  (`android.builtInKotlin=false`, `android.newDsl=false`,
  `android.enableAppCompileTimeRClass=false`, …) whose stated purpose was to
  keep pre-AGP-9 behaviour during the upgrade. AGP 9 enables optimised resource
  shrinking by default, so `false` restored the old behaviour; no concrete
  failure was recorded. Phase 4 therefore proceeded (`true`).
- Is the `**.R$*` field keep still required by any reflective resource lookup?
  **Resolved (task 2.1):** no. There is no `getIdentifier`, `Class.forName`, or
  `getField`/`getDeclaredField` on `R` types anywhere in `app`, `core` or `car`.
  Removed in Phase 1.
- Does the app itself (not a library) read `androidx`-namespaced members by
  name anywhere beyond the two OSMDroid sites?
  **Resolved (task 4.1):** no. The only by-name references are the layout
  `android:name` for `SettingsFragment`, fully-qualified custom-view tags
  (AlarmView, HistogramView, LegendView, MessageListPreference, RegionView,
  ScaledTextView, TimeSlider — covered by the constructor keeps), and AndroidX
  view classes used in layouts (`FragmentContainerView`, `RecyclerView` —
  covered by AndroidX consumer rules / the constructor keeps). Removed in
  Phase 3.
- What absolute target should the spec's "configured target" use?
  **Resolved (task 4.4) from the Phase 3 measurement:** 75%. Phase 3 rates were
  phone opt/obf/shr = 79.58/80.44/80.17% and automotive 76.96/78.01/77.77%, so
  75% is met by both with headroom.

### Baseline note (task 1.1)

The recorded phone baseline (no-opt 83.95 / no-obf 83.86 / no-shr 83.92) was
read from the pre-edge-to-edge release (commit `080272e1`). Rebuilding at the
current HEAD (`1602fc1b`, which includes the `2d9788b6` edge-to-edge work) gives
84.12/84.03/84.08 — about 0.17pp lower, because the new screen code references
more AndroidX. The automotive baseline reproduced exactly
(89.43/89.33/89.39). The fresh HEAD numbers are the true Phase 0 comparison
point; the recorded baseline is kept for continuity.
