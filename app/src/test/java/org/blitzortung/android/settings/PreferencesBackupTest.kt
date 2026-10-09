package org.blitzortung.android.settings

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesBackupTest {
    // --- 2.1 format metadata -------------------------------------------------

    @Test
    fun serializedBackupContainsFormatMetadata() {
        val json = PreferencesBackup.serialize(mapOf("alarm_enabled" to true), appVersionCode = 352)

        val root = JSONObject(json)
        assertThat(root.getString(PreferencesBackup.FIELD_FORMAT)).isEqualTo(PreferencesBackup.FORMAT_ID)
        assertThat(root.getInt(PreferencesBackup.FIELD_VERSION)).isEqualTo(PreferencesBackup.FORMAT_VERSION)
        assertThat(root.getInt(PreferencesBackup.FIELD_APP_VERSION_CODE)).isEqualTo(352)
        assertThat(root.getJSONObject(PreferencesBackup.FIELD_PREFERENCES).getBoolean("alarm_enabled")).isTrue()
    }

    // --- 2.2 native types round-trip ----------------------------------------

    @Test
    fun valueTypesRoundTripWithSameTypeAndValue() {
        val original: Map<String, Any> =
            mapOf(
                "bool_value" to true,
                "int_value" to 42,
                "long_value" to 4_000_000_000L,
                "float_value" to 1.5f,
                "string_value" to "SATELLITE",
            )

        val parsed = parseSuccess(PreferencesBackup.serialize(original, appVersionCode = 1))
        val plan = planSuccess(parsed, original)

        assertThat(plan.toApply.keys).containsExactlyInAnyOrderElementsOf(original.keys)
        original.forEach { (key, expectedValue) ->
            val actual = plan.toApply[key]
            assertThat(actual).isEqualTo(expectedValue)
            assertThat(actual!!::class.java).isEqualTo(expectedValue::class.java)
        }
        assertThat(plan.unknownKeys).isEmpty()
        assertThat(plan.missingKeys).isEmpty()
    }

    @Test
    fun largeLongIsNotTruncatedToInt() {
        val original: Map<String, Any> = mapOf("long_value" to 4_000_000_000L)

        val parsed = parseSuccess(PreferencesBackup.serialize(original, appVersionCode = 1))
        val plan = planSuccess(parsed, original)

        assertThat(plan.toApply["long_value"]).isEqualTo(4_000_000_000L)
        assertThat(plan.toApply["long_value"]).isInstanceOf(Long::class.javaObjectType)
    }

    // --- 2.3 exclusions ------------------------------------------------------

    @Test
    fun serializationExcludesCredentialsAndDeviceSpecificPath() {
        val preferences: Map<String, Any?> =
            mapOf(
                "username" to "user@example.com",
                "password" to "secret",
                "osmdroid.basePath" to "/data/user/0/tiles",
                "alarm_enabled" to true,
                "map_scale" to 75,
            )

        val prefs = JSONObject(PreferencesBackup.serialize(preferences, appVersionCode = 1))
            .getJSONObject(PreferencesBackup.FIELD_PREFERENCES)

        assertThat(prefs.has("username")).isFalse()
        assertThat(prefs.has("password")).isFalse()
        assertThat(prefs.has("osmdroid.basePath")).isFalse()
        assertThat(prefs.getBoolean("alarm_enabled")).isTrue()
        assertThat(prefs.getInt("map_scale")).isEqualTo(75)
    }

    @Test
    fun planImportIgnoresExcludedKeysEvenWhenPresentInFile() {
        val file: Map<String, Any> =
            mapOf(
                "password" to "secret",
                "osmdroid.basePath" to "/tmp",
                "alarm_enabled" to true,
            )
        val device: Map<String, Any> =
            mapOf(
                "password" to "old-secret",
                "osmdroid.basePath" to "/old",
                "alarm_enabled" to false,
            )

        val plan = planSuccess(file, device)

        assertThat(plan.toApply).containsOnlyKeys("alarm_enabled")
        assertThat(plan.unknownKeys).isEmpty()
        assertThat(plan.missingKeys).isEmpty()
    }

    // --- 2.4 parse and validation -------------------------------------------

    @Test
    fun parseRejectsMalformedJson() {
        val result = PreferencesBackup.parse("{not valid json")

        assertThat(result).isInstanceOf(BackupParseResult.Failure::class.java)
        assertThat((result as BackupParseResult.Failure).reason)
            .isEqualTo(BackupFailureReason.MALFORMED_JSON)
    }

    @Test
    fun parseRejectsForeignFormat() {
        val result = PreferencesBackup.parse("""{"format":"other/app","version":1,"preferences":{}}""")

        assertThat((result as BackupParseResult.Failure).reason)
            .isEqualTo(BackupFailureReason.NOT_A_BACKUP)
    }

    @Test
    fun parseRejectsMissingFormat() {
        val result = PreferencesBackup.parse("""{"version":1,"preferences":{}}""")

        assertThat((result as BackupParseResult.Failure).reason)
            .isEqualTo(BackupFailureReason.NOT_A_BACKUP)
    }

    @Test
    fun parseRejectsUnsupportedVersion() {
        val result =
            PreferencesBackup.parse(
                """{"format":"${PreferencesBackup.FORMAT_ID}","version":999,"preferences":{}}""",
            )

        assertThat((result as BackupParseResult.Failure).reason)
            .isEqualTo(BackupFailureReason.UNSUPPORTED_VERSION)
    }

    @Test
    fun parseRejectsMissingVersion() {
        val result =
            PreferencesBackup.parse("""{"format":"${PreferencesBackup.FORMAT_ID}","preferences":{}}""")

        assertThat((result as BackupParseResult.Failure).reason)
            .isEqualTo(BackupFailureReason.UNSUPPORTED_VERSION)
    }

    @Test
    fun parseRejectsMissingPreferencesObject() {
        val result =
            PreferencesBackup.parse("""{"format":"${PreferencesBackup.FORMAT_ID}","version":1}""")

        assertThat((result as BackupParseResult.Failure).reason)
            .isEqualTo(BackupFailureReason.INVALID_PREFERENCES)
    }

    @Test
    fun parseReturnsPreferencesAndAppVersionCode() {
        val result =
            PreferencesBackup.parse(
                """{"format":"${PreferencesBackup.FORMAT_ID}","version":1,""" +
                    """"appVersionCode":352,"preferences":{"map_scale":75}}""",
            )

        assertThat(result).isInstanceOf(BackupParseResult.Success::class.java)
        val success = result as BackupParseResult.Success
        assertThat(success.appVersionCode).isEqualTo(352)
        assertThat(success.preferences["map_scale"]).isEqualTo(75)
    }

    // --- 2.5 type coercion ---------------------------------------------------

    @Test
    fun planImportCoercesNumbersToExpectedDeviceTypes() {
        val file: Map<String, Any> =
            mapOf(
                "int_value" to 7L,
                "long_value" to 7,
                "float_value" to 7.5,
            )
        val device: Map<String, Any> =
            mapOf(
                "int_value" to 0,
                "long_value" to 0L,
                "float_value" to 0f,
            )

        val plan = planSuccess(file, device)

        assertThat(plan.toApply["int_value"]).isEqualTo(7)
        assertThat(plan.toApply["int_value"]).isInstanceOf(Int::class.javaObjectType)
        assertThat(plan.toApply["long_value"]).isEqualTo(7L)
        assertThat(plan.toApply["long_value"]).isInstanceOf(Long::class.javaObjectType)
        assertThat(plan.toApply["float_value"]).isEqualTo(7.5f)
        assertThat(plan.toApply["float_value"]).isInstanceOf(Float::class.javaObjectType)
    }

    @Test
    fun planImportRejectsKnownKeyWithUnexpectedType() {
        val file: Map<String, Any> = mapOf("alarm_enabled" to "not-a-boolean")
        val device: Map<String, Any> = mapOf("alarm_enabled" to false)

        val result = PreferencesBackup.planImport(file, device)

        assertThat(result).isInstanceOf(ImportPlanResult.Failure::class.java)
        assertThat((result as ImportPlanResult.Failure).key).isEqualTo("alarm_enabled")
    }

    @Test
    fun planImportRejectsIntExpectedButFloatProvided() {
        val result = PreferencesBackup.planImport(mapOf("map_scale" to 75.5), mapOf("map_scale" to 75))

        assertThat(result).isInstanceOf(ImportPlanResult.Failure::class.java)
        assertThat((result as ImportPlanResult.Failure).key).isEqualTo("map_scale")
    }

    // --- 3.1 reconciliation --------------------------------------------------

    @Test
    fun reconciliationClassifiesUnknownMissingAndAppliedKeys() {
        val file: Map<String, Any> =
            mapOf(
                "alarm_enabled" to true,
                "map_scale" to 50,
                "unknown_key" to "ignored",
            )
        val device: Map<String, Any> =
            mapOf(
                "alarm_enabled" to false,
                "map_scale" to 75,
                "map_fade" to 55,
            )

        val plan = planSuccess(file, device)

        assertThat(plan.toApply.keys).containsExactlyInAnyOrder("alarm_enabled", "map_scale")
        assertThat(plan.unknownKeys).containsExactly("unknown_key")
        assertThat(plan.missingKeys).containsExactly("map_fade")
    }

    @Test
    fun reconciliationReportsNoDifferencesWhenKeysMatch() {
        val file: Map<String, Any> = mapOf("alarm_enabled" to true, "map_scale" to 50)
        val device: Map<String, Any> = mapOf("alarm_enabled" to false, "map_scale" to 75)

        val plan = planSuccess(file, device)

        assertThat(plan.unknownKeys).isEmpty()
        assertThat(plan.missingKeys).isEmpty()
    }

    @Test
    fun reconciliationReportsMissingKeysOnly() {
        val plan = planSuccess(mapOf("alarm_enabled" to true), mapOf("alarm_enabled" to false, "map_fade" to 55))

        assertThat(plan.unknownKeys).isEmpty()
        assertThat(plan.missingKeys).containsExactly("map_fade")
    }

    @Test
    fun reconciliationReportsUnknownKeysOnly() {
        val plan = planSuccess(mapOf("alarm_enabled" to true, "extra" to 1), mapOf("alarm_enabled" to false))

        assertThat(plan.unknownKeys).containsExactly("extra")
        assertThat(plan.missingKeys).isEmpty()
    }

    // --- helpers -------------------------------------------------------------

    private fun parseSuccess(json: String): Map<String, Any> {
        val result = PreferencesBackup.parse(json)
        assertThat(result).isInstanceOf(BackupParseResult.Success::class.java)
        return (result as BackupParseResult.Success).preferences
    }

    private fun planSuccess(
        file: Map<String, Any>,
        device: Map<String, Any>,
    ): ImportPlan {
        val result = PreferencesBackup.planImport(file, device)
        assertThat(result).isInstanceOf(ImportPlanResult.Success::class.java)
        return (result as ImportPlanResult.Success).plan
    }
}
