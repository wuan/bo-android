# Tasks

## 1. Record the decision in AGENTS.md

- [x] 1.1 Add a note to the Edge-to-Edge Display section of `AGENTS.md` stating that Play's `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` warning is knowingly accepted, why (AndroidX `enableEdgeToEdge` for API 28–29 plus the `-keep class androidx.** { *; }` rule retaining it), and where the evidence lives; verify the note names `SHORT_EDGES`, `enableEdgeToEdge`, and the AndroidX keep rule, and points at this change.
- [x] 1.2 Correct the earlier attribution that placed the warning in "AppCompat/Material internals" so the note and `AGENTS.md` agree; verify no remaining text attributes it to Material/AppCompat.

## 2. Annotate the edge-to-edge call sites

- [x] 2.1 Add a short comment above the `enableEdgeToEdge(...)` call in `app/src/main/java/org/blitzortung/android/app/Main.kt` pointing to the `AGENTS.md` note; verify the diff is comment-only (no code/parameter changes).
- [x] 2.2 Add the same short comment above the `enableEdgeToEdge(...)` call in `app/src/main/java/org/blitzortung/android/settings/SettingsActivity.kt`; verify the diff is comment-only.

## 3. Verify nothing else changed

- [x] 3.1 Confirm the app never references the constant directly: `grep -rn "LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES\|layoutInDisplayCutoutMode" app/src car/src core/src automotive/src` returns no matches.
- [x] 3.2 Confirm the documentation compiles/links cleanly by running `./gradlew :app:assembleDebug` and `./gradlew testDebugUnitTest` and seeing both pass (comment/doc-only change; behaviour and tests must be unaffected).

## 4. Integration check

- [x] 4.1 Re-read the finished `AGENTS.md` note and `design.md` together and confirm a maintainer unfamiliar with the issue can (a) tell it is an accepted warning, not an app bug, and (b) find the rejected-fix plan if Google later makes it blocking.

## Workflow follow-up

- Archive this change once the documentation edits are reviewed.
- Verify the archived result renders the accepted-warning note in `AGENTS.md`.
