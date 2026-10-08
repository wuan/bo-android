## Context

See `proposal.md` for motivation. The current state that shapes this design:

- Application preferences live in the default `SharedPreferences` file
  (`PreferenceManager.getDefaultSharedPreferences`, Dagger-provided as
  `SharedPreferences`). Two *other* files exist and are deliberately out of
  scope: the version-bookkeeping file used by `VersionComponent`
  (`context.getSharedPreferences(packageName, ...)`) and the OSMDroid map-state
  file (`org.andnav.osm.prefs` used by `MapFragment`).
- `PreferenceKey` mirrors most keys but is incomplete: `signaling_suppression_time`
  is defined in `preferences.xml` and localized, but the enum's
  `ALERT_SIGNALING_THRESHOLD_TIME` still points at the pre-rename key
  `signaling_threshold_time`. The setting is therefore read by code but never
  matches the stored value. This is a regression from commit `85ab6bcf`, which
  renamed the preference without updating its consumers.
- The default prefs file receives one literal, library-owned key:
  `osmdroid.basePath`, written by `Configuration.load(...)`. It also receives
  `background_location_disclosure_shown`.
- `Main` calls `PreferenceManager.setDefaultValues(this, R.xml.preferences, false)`
  at startup, so on a normally-started device every XML key already exists with
  its correct value and type before the user can reach the settings screen.
- Many components implement `OnSharedPreferenceChangeListener`, and `AppService`
  plus `DataProviderFactory` hold state derived from preferences.
- The project already uses `org.json` for API parsing and `ActivityResultContracts`
  in `SettingsFragment` (ringtone picker), so no new dependency is required for
  JSON or file picking.

## Goals / Non-Goals

**Goals:**
- Export the user-meaningful preferences to a portable, human-readable JSON file.
- Import that file on another device or after a reinstall, with all-or-nothing
  validation.
- Reset all preferences to the values declared in `preferences.xml`.
- Keep credentials and device-specific values out of the file.
- Apply changes coherently despite live listeners and running services.

**Non-Goals:**
- Named settings profiles or presets.
- Cloud sync or changes to Android Auto Backup behavior.
- Exporting the map-state file or the version-bookkeeping file.
- Per-key merge or cherry-picking on import.

## Decisions

### Decision: Serialize from the live preference map, filtered by a denylist

Export iterates `SharedPreferences.all` rather than the `PreferenceKey` enum or a
hand-maintained schema. This is drift-proof: it automatically covers the dead
`signaling_suppression_time` question and any future key, and the value's runtime
type is available directly. Two categories are then removed:

- **Credentials**: `username`, `password` (never written to a shareable file).
- **Device-specific**: `osmdroid.basePath` (a storage path belonging to the map
  library, not a portable setting).

*Alternatives considered:* the `PreferenceKey` enum is incomplete and carries no
type information, so it would silently drop keys. A bespoke export schema is more
code and another list to maintain. The filtered live map gets the same safety with
less upkeep.

### Decision: JSON via `org.json`, versioned header, native JSON types

The file is:

```json
{
  "format": "bo-android/preferences",
  "version": 1,
  "appVersionCode": 123,
  "preferences": { "alarm_enabled": true, "map_scale": 75, "map_mode": "SATELLITE" }
}
```

`format` identifies our files; `version` gates future format evolution;
`appVersionCode` aids diagnostics. Values use native JSON types. `org.json` is
already used by the API client, so this adds no dependency.

### Decision: Import replaces by clearing, applying defaults, then overlaying the file

Import is a single replacement, not a merge. Implementation order:

1. Parse and validate the whole file first (all-or-nothing).
2. Snapshot the type of every currently-existing preference key.
3. Compute a reconciliation against that snapshot: recognized keys to apply,
   unknown keys (in the file, not recognized), and missing keys (recognized on
   the device, absent from the file).
4. Present the reconciliation summary with split emphasis (unknown keys
   prominent, missing keys secondary) and wait for explicit confirmation.
5. `clear()`, then re-apply XML defaults via `setDefaultValues(..., readAgain = true)`.
6. Overlay the values from the file, coercing each to its expected type.

This gives a clean semantic: keys absent from the file fall back to their
defaults rather than lingering, and credentials/path end up cleared. Applying
defaults before the overlay also means a backup produced by an older app version
does not leave unknown removal-tombstones.

### Decision: Import warns about unknown keys and secondarily summarizes missing keys

Because replacement is destructive and the set of preferences can drift between
app versions, the import confirmation is preceded by a reconciliation summary.
Emphasis is split by severity: unknown keys (file-only) are potentially a sign of
a foreign or newer file, so they produce a prominent warning that names them;
missing keys (device-only) are normal version drift, so they are listed in a
secondary, collapsed line and explained as being reset to defaults. No preference
is touched until the user confirms. When there are no differences, neither is
shown and the normal confirmation is displayed. This turns a silent, potentially
surprising replacement into an informed one without making routine migrations
feel alarming, and it does not require per-key merge logic.

*Alternatives considered:* silently ignoring unknown keys and silently defaulting
missing ones (rejected as surprising); treating both categories with equal,
prominent emphasis (rejected because version drift would make legitimate
migrations look like errors); blocking the import when differences exist
(rejected as too strict, since version drift is normal).

### Decision: Derive expected types from the pre-existing defaults

JSON numbers are ambiguous (int/long/float). Because `setDefaultValues` has
already populated every known key on a normally-started device, the pre-import
snapshot tells us each key's expected type. Incoming values are coerced to that
type; a known key whose JSON type cannot be coerced to its expected type fails
validation. Keys not present in the snapshot are treated as unknown and ignored,
which doubles as the allowlist.

### Decision: Reset reuses the same clear-and-default mechanism

Reset is `clear()` followed by `setDefaultValues(..., readAgain = true)`. The
`readAgain = true` argument is required: without it, `setDefaultValues` consults a
framework flag that suppresses re-applying defaults after a clear, leaving keys
empty instead of defaulted. Reset clears credentials as well, per the spec.

### Decision: Gather in `SettingsFragment` with SAF contracts

A new "Backup" preference category hosts three entries: Export, Import, Reset.
`SettingsFragment` already registers activity-result launchers, so export uses
`ActivityResultContracts.CreateDocument("application/json")` and import uses
`ActivityResultContracts.OpenDocument`. File I/O goes through `ContentResolver`.
Serialization/validation/reset live in a dedicated settings helper class so they
are unit-testable without the fragment.

### Decision: Prompt and perform a restart after import/reset

After import or reset, show a dialog offering to restart. Restarting starts
`Main` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` and stops
`AppService` so it re-reads preferences on its next start. This satisfies the
requirement to move running state onto the new values and avoids reasoning about
each listener firing during a bulk write.

### Decision: Restore the `signaling_suppression_time` preference instead of deleting it

Commit `34f48c2f` (2016) added an optional "signaling threshold time" that
suppresses repeated alarm signals within a chosen window, keyed
`signaling_threshold_time`. Commit `85ab6bcf` renamed the preference and its
resources to `signaling_suppression_time` but did not update
`PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME` or `AlertHandler`, silently
disabling the user setting. The fix is to re-point the code at
`signaling_suppression_time` and to look up the preference as a `ListPreference`
in `SettingsFragment` (it is not an `EditTextPreference`), rather than removing
the preference. The default `0` (off) declared in `preferences.xml` is preserved.
This also makes the restored preference a normal, recognized key in exports.

## Risks / Trade-offs

- **JSON number type ambiguity (int vs long vs float)** →
  Coercion against the pre-existing default type (see decision above); add tests
  for each numeric case.
- **Import clears credentials**, since backups never contain them → This is the
  intended replace semantic and is stated in the spec; surface it in the import
  confirmation text so users are not surprised.
- **`setDefaultValues` re-apply quirk** → Use `readAgain = true` after `clear()`
  and cover it with a Robolectric test asserting defaults, not empties, after reset.
- **Bulk write fires many change listeners while `AppService` runs** → Require a
  restart prompt and stop the service, rather than trying to serialize listener
  reactions during the write.
- **Overlap with Android Auto Backup** → Out of scope; this feature is an explicit,
  user-controlled escape hatch and does not change backup rules.
- **New user-facing strings require translation** → Add English strings first;
  translations can follow, matching the project's existing workflow.
- **Importing files from a future format version** → Rejected via the `version`
  header with a clear message, keeping old app versions from misreading new files.

## Migration Plan

- Change ships as a normal app update. No data migration is needed for the backup
  feature itself.
- Existing installs that stored the old `signaling_threshold_time` key keep that
  orphan value, which is harmless. The restored preference uses the newer
  `signaling_suppression_time` key; the `0` (off) default applies until the user
  chooses a value.
- Backup `version` starts at `1`. Future changes to the file shape must increment
  it and add conversion handling; the reader already rejects unknown versions.
- Rollback: the feature is additive (new settings entries and a helper). Reverting
  the code leaves any exported files inert and existing preferences intact.

## Open Questions

- Whether to additionally offer a share-sheet (`ACTION_SEND`) export path beside
  the document picker. This is a transport detail that does not affect the spec or
  the format and can be decided during implementation.
- Whether to localize the new strings immediately or let volunteers follow up,
  consistent with the project's existing translation workflow.
