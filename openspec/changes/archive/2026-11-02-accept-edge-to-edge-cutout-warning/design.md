# Design

## Context

See `proposal.md - Why`. The constraints that shape this decision:

- Views-based app, `minSdk 23`, `targetSdk 37`. Android 15 forces edge-to-edge for apps targeting SDK 35+, and the app already adopts `enableEdgeToEdge()` in `Main` and `SettingsActivity` (change `2026-11-02-fix-edge-to-edge-display`).
- The app ships two artifacts to Play: the phone `app` module and the `automotive` module. Both include AndroidX and share the same keep strategy.
- `app/proguard-rules.pro` and `automotive/proguard-rules.pro` both contain `-keep class androidx.** { *; }` and `-keep interface androidx.** { *; }` (present since `f43b0d35`, 2025-11-21).

## Goals / Non-Goals

**Goals:**
- Establish, with evidence, exactly where `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` comes from.
- Make a deliberate accept/decline decision and record it durably for future maintainers and agents.
- Avoid any change that risks the working edge-to-edge layout or R8 behavior.

**Non-Goals:**
- Removing the Play warning. As shown below, no app-source-only change can remove it while the AndroidX keep rule is in place.
- Changing `enableEdgeToEdge()`, the inset handling, the keep rules, or the `edge-to-edge-display` spec (behavior is unchanged).
- Chasing Google to change the analyzer.

## Decisions

### D1: Accept the warning and document it

`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` is the integer constant `1`. Play's analyzer flags any code that writes `WindowManager.LayoutParams.layoutInDisplayCutoutMode = 1`. In this project that value is only ever produced by AndroidX, confirmed by disassembling the shipped artifacts:

```
androidx/activity/EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode(Window)
    iconst_1  →  iput  LayoutParams.layoutInDisplayCutoutMode      # 1 = SHORT_EDGES

androidx/core/view/WindowCompat.enableEdgeToEdge(Window)   (core 1.19.0)
    int mode = (SDK >= 30) ? 3 /*ALWAYS*/ : 1 /*SHORT_EDGES*/;
    attrs.layoutInDisplayCutoutMode = mode;                        # includes const 1
```

Google's Android 15 behavior-change page describes the deprecation as an *effect*, not a broken call:

> **Display cutout** — `layoutInDisplayCutoutMode` of non-floating windows must be `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`. `SHORT_EDGES`, `NEVER`, and `DEFAULT` are interpreted as `ALWAYS` so that users don't see a black bar caused by the display cutout.

On API 35+ the same helper sets `ALWAYS` (3); `SHORT_EDGES` is only used on API 28–29, where it is the correct and only way to draw into a cutout. Behavior is therefore correct; the warning is a static-scan match on retained library code.

*Alternatives considered:* treat it as a bug and try to remove it — rejected, see D2/D3.

### D2: The blanket AndroidX keep rule retains the reference even when unused

`-keep class androidx.** { *; }` prevents R8 from removing or obfuscating *any* AndroidX member. This was verified against the shipped `automotive-release.aab`: it contains `androidx.core.view.WindowCompat.enableEdgeToEdge` with `const 1`, which **nothing invokes** (no method reference in the dex) — pure dead code retained by the keep rule. That means:

- Removing the two `enableEdgeToEdge()` calls from app source would *not* clear the warning, because the keep rule still retains the AndroidX code that references the constant.
- The `automotive` bundle warns for this reason alone (it never calls edge-to-edge).
- `app` additionally emits the reference through `androidx.activity.EdgeToEdgeApi28` (via the live `enableEdgeToEdge()` calls). The Play location `v.n` is an obfuscated app frame into which R8 inlined that call, or a Play symbolization artifact; app source never names the constant.

Note: Material Components 1.14.0's `EdgeToEdgeUtils` does **not** reference the cutout constant (it does call the deprecated bar APIs), so it is not a source. This corrects the earlier "AppCompat/Material internals" attribution.

### D3: Rejected fix — drop `enableEdgeToEdge()` and narrow the keep rule

The only way to remove the warning is to do both of the following, which is why it is declined for now:

1. Stop calling `enableEdgeToEdge()` and hand-roll edge-to-edge (`setDecorFitsSystemWindows(false)`, transparent bars, `ALWAYS` on API 30+, no cutout mode on API 28–29). Cost: re-implementing bar transparency and icon contrast; `setDecorFitsSystemWindows` / `setStatusBarColor` are themselves on Play's deprecated list, risking new warnings; lost cutout draw on API 28–29 notched devices in landscape.
2. Narrow `-keep class androidx.** { *; }` (e.g. `-keep,allowshrinking …`) so the now-dead AndroidX code is stripped. Cost: broad R8-config change that affects the whole app and needs a full regression pass.

Rejected while the warning is non-blocking; this note is the reference if that changes.

## Risks / Trade-offs

- **Warning persists across releases** → accepted and documented; it is a Play recommendation, not a rejection.
- **Warning becomes blocking later** → D3 records the concrete, reversible starting point.
- **Attribution drifts again** → the evidence (constant value, API classes, the uncalled retained method) lives here and in `AGENTS.md`, so it can be re-verified rather than re-guessed.
- **No behavior change** → risk is limited to documentation only.

## Migration Plan

Docs and comments only. No data or preference migration, no rollout concerns. Rollback is reverting the doc/comment edits. Ship in a normal release.
