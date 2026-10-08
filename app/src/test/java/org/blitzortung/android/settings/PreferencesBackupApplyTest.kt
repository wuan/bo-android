package org.blitzortung.android.settings

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.blitzortung.android.app.R

@RunWith(RobolectricTestRunner::class)
class PreferencesBackupApplyTest {
    private val context = RuntimeEnvironment.getApplication()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    // --- 4.1 reset ----------------------------------------------------------

    @Test
    fun resetRestoresXmlDefaultsAndClearsCredentials() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true)
        preferences.edit(commit = true) {
            putBoolean("alarm_enabled", false)
            putInt("map_scale", 25)
            putString("measurement_unit", "IMPERIAL")
            putString("username", "user@example.com")
            putString("password", "secret")
        }

        PreferencesBackup.resetToDefaults(context, preferences)

        assertThat(preferences.getBoolean("alarm_enabled", false)).isTrue()
        assertThat(preferences.getInt("map_scale", -1)).isEqualTo(75)
        assertThat(preferences.getString("measurement_unit", null)).isEqualTo("METRIC")
        assertThat(preferences.getString("username", null)).isEmpty()
        assertThat(preferences.getString("password", null)).isNull()
    }

    // --- 4.2 import apply ---------------------------------------------------

    @Test
    fun importAppliesFileValuesAndResetsAbsentKeysToDefaults() {
        PreferencesBackup.resetToDefaults(context, preferences)
        preferences.edit(commit = true) {
            putInt("map_scale", 25)
            putString("color_scheme", "DARK")
            putString("username", "user@example.com")
            putString("password", "secret")
            putString("osmdroid.basePath", "/data/tiles")
        }

        val plan =
            ImportPlan(
                toApply = mapOf("map_scale" to 50, "measurement_unit" to "IMPERIAL"),
                unknownKeys = emptyList(),
                missingKeys = listOf("color_scheme"),
            )

        PreferencesBackup.applyImport(context, preferences, plan)

        assertThat(preferences.getInt("map_scale", -1)).isEqualTo(50)
        assertThat(preferences.getString("measurement_unit", null)).isEqualTo("IMPERIAL")
        assertThat(preferences.getString("color_scheme", null)).isEqualTo("BLITZORTUNG")
        assertThat(preferences.getString("username", null)).isEmpty()
        assertThat(preferences.getString("password", null)).isNull()
        assertThat(preferences.getString("osmdroid.basePath", null)).isNull()
    }

    // --- 4.3 cancelled import -----------------------------------------------

    @Test
    fun planningAnImportWithoutApplyingLeavesPreferencesUnchanged() {
        PreferencesBackup.resetToDefaults(context, preferences)
        preferences.edit(commit = true) {
            putInt("map_scale", 25)
            putString("username", "user@example.com")
        }
        val before = PreferencesBackup.snapshot(preferences)

        val parsed =
            PreferencesBackup.parse(
                PreferencesBackup.serialize(mapOf("map_scale" to 50), appVersionCode = 1),
            ) as BackupParseResult.Success
        PreferencesBackup.planImport(parsed.preferences, PreferencesBackup.snapshot(preferences))

        assertThat(PreferencesBackup.snapshot(preferences)).isEqualTo(before)
    }
}
