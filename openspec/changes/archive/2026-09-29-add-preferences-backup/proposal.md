## Why

The app exposes ~30 preferences across map, data source, location, alarm, and
generic categories, but there is no supported way to move them between devices
or recover them after a reinstall. Android Auto Backup is enabled by default but
is opaque and unreliable for this purpose (GitHub issue #256). Users must
manually re-enter every setting. A plain, user-controlled export/import of the
settings file solves migration and reinstall recovery, and a reset-to-default
action gives users a clean escape hatch when their configuration is broken.

## What Changes

- Add an **export** action that writes the current preferences to a JSON file
  chosen through the system document picker.
- Add an **import** action that reads a previously exported JSON file, validates
  it, and replaces the current preferences after a confirmation prompt.
- Add a **reset to defaults** action that restores all preferences to the values
  defined in `res/xml/preferences.xml` after a confirmation prompt.
- Exclude credentials (`username`, `password`) from exported files so a shared
  backup never leaks a Blitzortung account password.
- Exclude the OSMDroid-managed `osmdroid.basePath` key, which is a
  device-specific storage path that does not belong in a portable backup.
- Prompt the user to restart the app after a successful import or reset, since
  running services and data providers may hold state derived from old values.
- Restore the broken `signaling_suppression_time` (alert signaling suppression
  window) preference. Commit `85ab6bcf` ("remove unused layer names") renamed the
  XML key and string resources from `signaling_threshold_time` to
  `signaling_suppression_time`, but left `PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME`
  and `AlertHandler` reading the old key. Since then the setting has been ignored
  and the alarm used a hardcoded fallback. Re-point the code at
  `signaling_suppression_time`.

## Capabilities

### New Capabilities
- `preferences-backup`: user-initiated export, import, and reset-to-default of
  application preferences, including the JSON backup format, what is included
  or excluded, validation behavior, and post-operation restart handling.

### Modified Capabilities
<!-- None. No existing capability specs are present in this repository. -->

## Impact

- `app/src/main/java/org/blitzortung/android/settings/SettingsFragment.kt`:
  new backup preference entries and SAF activity-result launchers.
- `app/src/main/res/xml/preferences.xml`: new "Backup" category; restoration of
  the `signaling_suppression_time` entry that was renamed away from its code.
- `app/src/main/java/org/blitzortung/android/app/view/PreferenceKey.kt`: point
  `ALERT_SIGNALING_THRESHOLD_TIME` at `signaling_suppression_time`.
- `app/src/main/java/org/blitzortung/android/alert/handler/AlertHandler.kt`: read
  the restored key and align its fallback default with the XML default.
- `app/src/main/java/org/blitzortung/android/settings/SettingsFragment.kt`: look up
  the signaling suppression preference as a `ListPreference` so alert toggling
  does not class-cast.
- `app/src/main/res/values/strings.xml` (and translations): new UI strings.
- New code for JSON serialization/validation and the reset operation, likely
  under `org.blitzortung.android.settings`.
- Unit tests for JSON round-trip, key exclusion, validation failures, and reset
  behavior.
- No new runtime dependencies are expected; JSON is handled with the existing
  serialization facilities available to the project.
- No changes to the OSMDroid prefs file or the version bookkeeping prefs file;
  scope is limited to the default `SharedPreferences` file.
