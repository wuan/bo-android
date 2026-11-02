## Why

Google Play's R8 recommendation flags the phone release artifact: optimisation,
obfuscation and shrinking rates are all 16%, and optimised resource shrinking is
off. These three figures are not three separate problems — they are R8's own
`noOptimizationPercentage` / `noObfuscationPercentage` / `noShrinkingPercentage`
stats (83.95 / 83.86 / 83.92, read straight from
`BUNDLE-METADATA/com.android.tools/r8.json` inside the shipped AAB), and they
coincide because one construct starves all three stages at once.

R8's keep-radius analysis of the current release attributes **68% of every
rule-kept class/field/method in the app to a single line**,
`-keep class androidx.** { *; }` (64 429 of 94 270 pinned items; 5 141 of 9 928
live classes). Roughly 94% of all pinned code originates in the app's own
`proguard-rules.pro`, not in library consumer rules or AGP defaults. A `{ *; }`
keep rule makes every matched member a shrink/obfuscation/optimisation root, so
AndroidX — the majority of the app's bytecode — is retained in full and left
unoptimised. This inflates the release artifact and forfeits R8's startup and
memory wins, while the same rule also retains dead AndroidX code.

## What Changes

- Replace the blanket AndroidX keeps with narrowly targeted ones:
  - remove `-keep class androidx.** { *; }` and `-keep interface androidx.** { *; }`;
  - keep only what reflection/layout inflation genuinely needs (custom view
    constructors, `Fragment` constructors, preference constructors, WorkManager
    worker constructors), relying on AndroidX AAR consumer rules for the rest.
- Replace `-keep class org.osmdroid.** { *; }` with targeted keeps for the only
  two reflection sites in the app (`MapController.mCurrentAnimator`,
  `ScaleBarOverlay.centred`) plus the required constructors.
- Narrow the other over-broad app rules that show up in the keep-radius report:
  `-keepclassmembers class **.R$* { public static <fields>; }` (10 329 pinned
  fields), the extra `*** get*();` on `android.view.View`, the `public *;` tail
  of the enum rule, `-keep class **_Factory/_MembersInjector`, `**Dagger*.*`,
  `-keep class * extends androidx.fragment.app.Fragment { *; }` and
  `-keep class * extends org.osmdroid.views.overlay.Overlay { *; }`, and the
  app event/alert `{ *; }` rules where a constructor-only keep suffices.
- Enable optimised resource shrinking (`android.r8.optimizedResourceShrinking=true`).
- Delete dead legacy rules (`-keep class * extends dagger.internal.Binding`,
  `-keep class * extends dagger.internal.ModuleAdapter`,
  `-keep class * extends android.preference.Preference`).
- Make the corpus of rules shared between `app` and `automotive` so the two
  artifacts do not drift.
- Add a repeatable local check that reads the three stats from the built AAB and
  compares them against the recorded baseline.
- Leave `android.r8.strictFullModeForKeepRules=false` unchanged (see design:
  uncertain payoff, crash-class risk).

## Capabilities

### New Capabilities
- `r8-optimization`: the release build applies R8 shrinking, obfuscation and
  optimisation, and optimised resource shrinking, to the whole program except
  for explicitly justified keep rules.

### Modified Capabilities
<!-- No existing capability describes release R8 configuration; edge-to-edge-display
     requirements are behavioural layout outcomes and are unchanged. -->

## Impact

- `app/proguard-rules.pro` and `automotive/proguard-rules.pro` — rule set
  reworked (currently byte-for-byte copies).
- `gradle.properties` — `android.r8.optimizedResourceShrinking` flipped to `true`.
- Release artifacts change materially: smaller dex, obfuscated AndroidX/OSMDroid,
  more dead code removed. No public API, preference, permission or data change.
- Verification depends on the release/`perf` build types and on reading
  `r8.json` from the built AAB; no new runtime behavior is introduced.
- Risk: the blanket keeps are defensive. Each removal must be covered by the
  on-device regression checklist (map, alerts, background service, widget,
  preferences backup, Automotive OS build) before shipping.
- Related but out of scope: `app/build.gradle.kts` declares
  `androidx.test.ext.junit.ktx` as `implementation` (a test dependency on the
  release classpath). Noted for a separate change; not part of this capability.
