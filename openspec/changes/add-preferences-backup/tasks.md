## 1. Restore signaling suppression preference

- [ ] 1.1 Restore the `signaling_suppression_time` `ListPreference` in `app/src/main/res/xml/preferences.xml` and its `signaling_suppression_*` resources in English and all locale variants, and verify `./gradlew processDebugResources` succeeds
- [ ] 1.2 Point `PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME` at `signaling_suppression_time` and change `SettingsFragment.enableNotifications` to look up a `ListPreference`, and verify the setting toggles with the alert-enabled preference
- [ ] 1.3 Align the `AlertHandler` signaling threshold fallback default with the XML default (`0`), and verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.alert.handler.AlertHandlerTest"` passes

## 2. Backup format and serialization

- [ ] 2.1 Add format constants and a serialization entry point (format id `bo-android/preferences`, version `1`, app version code, preferences map) in a new settings helper, and verify a unit test produces valid JSON with the metadata fields present
- [ ] 2.2 Serialize preference values with native JSON types preserving boolean/int/long/float/string, and verify a unit test asserts each type round-trips with the same type and value
- [ ] 2.3 Exclude `username`, `password`, and `osmdroid.basePath` from serialization, and verify unit tests assert these values are absent while other keys remain
- [ ] 2.4 Implement parsing and up-front validation that rejects malformed JSON, a wrong/missing format id, and an unsupported version, and verify unit tests cover each rejection with a descriptive result
- [ ] 2.5 Implement type coercion against the pre-import preference types and reject a known key whose JSON value cannot be coerced, and verify unit tests cover valid coercion and a type mismatch

## 3. Reconciliation

- [ ] 3.1 Compute the reconciliation set (recognized to apply, unknown file-only keys, missing device-only keys) from a parsed file and the current preferences, and verify unit tests assert each category for files with unknown keys, missing keys, and no differences

## 4. Apply and reset operations

- [ ] 4.1 Implement reset as `clear()` followed by `PreferenceManager.setDefaultValues(..., readAgain = true)`, and verify a Robolectric test asserts keys hold their XML default values (not empty) and credentials are cleared
- [ ] 4.2 Implement import apply as `clear()`, XML defaults, then overlay of recognized values with type coercion, excluding credentials and the device-specific map path, and verify a Robolectric test asserts a full replacement where absent keys fall back to defaults
- [ ] 4.3 Verify an import that is cancelled at the reconciliation summary leaves all preferences unchanged (Robolectric test asserting no write occurred)

## 5. Settings UI wiring

- [ ] 5.1 Add a "Backup" `PreferenceCategory` with Export, Import, and Reset entries to `preferences.xml` plus the needed English strings, and verify the settings screen renders the three entries with no resource errors
- [ ] 5.2 Wire the export entry to `ActivityResultContracts.CreateDocument("application/json")` and write the serialized content through `ContentResolver`, and verify manually that a chosen file contains the expected JSON and cancelling writes nothing
- [ ] 5.3 Wire the import entry to `ActivityResultContracts.OpenDocument` and show the reconciliation summary with unknown keys prominent and missing keys in a secondary line before any write, and verify manually that cancelling changes nothing and confirming applies the file
- [ ] 5.4 Wire the reset entry to a confirmation dialog before calling reset, and verify manually that cancelling changes nothing and confirming restores defaults
- [ ] 5.5 Show a restart prompt after a successful import or reset and perform a restart that starts `Main` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` and stops `AppService`, and verify manually that the app reopens with the imported/reset preferences in effect

## 6. Quality gate

- [ ] 6.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [ ] 6.2 Run `./gradlew lint` and verify no new lint errors are introduced by the change