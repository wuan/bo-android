# Proposal

## Why

Google Play reports **"Your app uses deprecated APIs or parameters for edge-to-edge"**, naming `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`. The constant is not used by any of the app's own code: it is emitted by AndroidX's edge-to-edge helper (`enableEdgeToEdge()`, and `androidx.core.view.WindowCompat.enableEdgeToEdge`) for API 28–29, and is retained in the shipped dex by the project's blanket `-keep class androidx.** { *; }` rule. The deprecation is about the *effect* of the flag on Android 15+ (where `SHORT_EDGES`, `NEVER`, and `DEFAULT` are interpreted as `ALWAYS`), not a broken app behavior, so there is nothing to migrate to that would both keep edge-to-edge on API 23–34 and satisfy the scanner.

The previous change (`2026-11-02-fix-edge-to-edge-display`) already treated this as an expected sibling warning but attributed it to "AppCompat/Material internals". The goal here is to record the verified root cause, decide to accept the warning, and leave a durable note so future maintainers and agents do not attempt a risky "fix" repeatedly.

## What Changes

- Record the verified root cause of the `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` Play warning in a design note (bytecode evidence and the originating AndroidX APIs).
- Document the decision to accept the warning in `AGENTS.md`, correcting the earlier attribution and explaining why it is not fixable from app source alone.
- Add short in-code comments at the two `enableEdgeToEdge()` call sites pointing at the note, so a reader understand why the warning appears.
- Explicitly reject the alternatives (hand-rolling edge-to-edge, narrowing the AndroidX keep rule) as out of scope unless the warning later becomes blocking.

No behavioral, UI, or dependency changes. Nothing in the app's edge-to-edge layout changes.

## Capabilities

### New Capabilities

<!-- None. This change records a decision and documentation only; it does not introduce behavior. -->

### Modified Capabilities

<!-- None. Window-layout behavior is unchanged and remains described by specs/edge-to-edge-display/spec.md. -->

No spec-level behavior changes; this change is documentation only and sets `skip_specs: true`.

## Impact

- **Documentation**: `AGENTS.md` (edge-to-edge conventions section), this change's `design.md`.
- **Code (comments only)**: `app/src/main/java/org/blitzortung/android/app/Main.kt`, `app/src/main/java/org/blitzortung/android/settings/SettingsActivity.kt`.
- **Not changed**: `app/proguard-rules.pro` / `automotive/proguard-rules.pro` keep rules, dependencies, layouts, themes, runtime behavior.
- **Follow-up risk**: if Google later makes this a blocking issue, the rejected options in `design.md` (drop `enableEdgeToEdge()` + narrow the AndroidX keep rule) become the starting point.
