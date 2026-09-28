package org.blitzortung.android.settings

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Looper
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class SettingsFragmentTest {
    private lateinit var context: Context
    private lateinit var controller: ActivityController<SettingsActivity>
    private lateinit var activity: SettingsActivity
    private lateinit var fragment: SettingsFragment
    private lateinit var preferences: SharedPreferences
    private var fileCounter = 0

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true)

        controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        activity = controller.get()
        fragment = activity.supportFragmentManager.findFragmentById(R.id.activity_settings) as SettingsFragment
        preferences = fragment.preferences
    }

    // --- wiring ---------------------------------------------------------------

    @Test
    fun exportPreferenceClickLaunchesDocumentPicker() {
        exportPreference().performClick()

        assertThat(shadowOf(activity).nextStartedActivityForResult).isNotNull()
    }

    @Test
    fun importPreferenceClickLaunchesDocumentPicker() {
        importPreference().performClick()

        assertThat(shadowOf(activity).nextStartedActivityForResult).isNotNull()
    }

    // --- export ---------------------------------------------------------------

    @Test
    fun exportWritesVersionedJsonWithCurrentPreferences() {
        preferences.edit(commit = true) {
            putBoolean(PreferenceKey.ALERT_ENABLED.toString(), true)
            putInt(PreferenceKey.MAP_SCALE.toString(), 125)
        }

        val file = backupFile("export.json")
        fragment.exportPreferences(Uri.fromFile(file))

        val root = JSONObject(file.readText())
        assertThat(root.getString(PreferencesBackup.FIELD_FORMAT)).isEqualTo(PreferencesBackup.FORMAT_ID)
        assertThat(root.getInt(PreferencesBackup.FIELD_VERSION)).isEqualTo(PreferencesBackup.FORMAT_VERSION)
        assertThat(root.getJSONObject(PreferencesBackup.FIELD_PREFERENCES).getBoolean("alarm_enabled")).isTrue()
        assertThat(root.getJSONObject(PreferencesBackup.FIELD_PREFERENCES).getInt("map_scale")).isEqualTo(125)
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(context.getString(R.string.backup_export_success))
    }

    @Test
    fun exportFailureShowsErrorToast() {
        fragment.exportPreferences(Uri.parse("content://org.blitzortung.missing/backup.json"))

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(context.getString(R.string.backup_export_failed))
    }

    // --- import: unreadable / malformed --------------------------------------

    @Test
    fun importFromUnreadableUriShowsReadFailedMessage() {
        fragment.importPreferences(Uri.fromFile(File(context.cacheDir, "does-not-exist.json")))

        assertLatestDialogMessage(R.string.app_name, R.string.backup_import_read_failed)
    }

    @Test
    fun importMalformedJsonShowsReadFailedMessage() {
        fragment.importPreferences(writeBackup("{not valid json"))

        assertLatestDialogMessage(R.string.app_name, R.string.backup_import_read_failed)
    }

    @Test
    fun importForeignFormatShowsUnsupportedMessage() {
        fragment.importPreferences(writeBackup("""{"format":"other/app","version":1,"preferences":{}}"""))

        assertLatestDialogMessage(R.string.app_name, R.string.backup_import_unsupported)
    }

    @Test
    fun importUnsupportedVersionShowsUnsupportedMessage() {
        val json =
            """{"format":"${PreferencesBackup.FORMAT_ID}","version":99,"preferences":{}}"""
        fragment.importPreferences(writeBackup(json))

        assertLatestDialogMessage(R.string.app_name, R.string.backup_import_unsupported)
    }

    @Test
    fun importWithUncoercibleValueShowsErrorWarning() {
        val json =
            PreferencesBackup.serialize(
                mapOf(PreferenceKey.MAP_SCALE.toString() to "not-a-number"),
                appVersionCode = 1,
            )
        fragment.importPreferences(writeBackup(json))

        val dialog = latestDialog()
        assertThat(shadowOf(dialog).title.toString()).isEqualTo(context.getString(R.string.backup_import_warning_title))
        assertThat(shadowOf(dialog).message.toString())
            .contains(PreferenceKey.MAP_SCALE.toString())
    }

    // --- import: confirmation flow -------------------------------------------

    @Test
    fun importWithoutUnknownKeysAppliesAndPromptsForRestart() {
        val json =
            PreferencesBackup.serialize(
                mapOf(PreferenceKey.MAP_SCALE.toString() to 125),
                appVersionCode = 1,
            )
        fragment.importPreferences(writeBackup(json))

        val confirmation = latestDialog()
        assertThat(shadowOf(confirmation).title.toString())
            .isEqualTo(context.getString(R.string.backup_import_confirm_title))

        confirmation.clickPositive()
        assertThat(preferences.getInt(PreferenceKey.MAP_SCALE.toString(), -1)).isEqualTo(125)

        val restart = latestDialog()
        assertThat(shadowOf(restart).title.toString()).isEqualTo(context.getString(R.string.backup_restart_title))

        restart.clickPositive()
        val mainIntent = startedActivities().firstOrNull { it.component?.className == Main::class.java.name }
        assertThat(mainIntent).isNotNull()
        assertThat(mainIntent!!.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK).isNotZero()
        assertThat(shadowOf(context as Application).nextStoppedService.component?.className)
            .isEqualTo(AppService::class.java.name)
    }

    @Test
    fun importWithUnknownKeysWarnsBeforeConfirmation() {
        val json =
            PreferencesBackup.serialize(
                mapOf(
                    PreferenceKey.MAP_SCALE.toString() to 125,
                    "totally_unknown_key" to 1,
                ),
                appVersionCode = 1,
            )
        fragment.importPreferences(writeBackup(json))

        val warning = latestDialog()
        assertThat(shadowOf(warning).title.toString())
            .isEqualTo(context.getString(R.string.backup_import_warning_title))
        assertThat(shadowOf(warning).message.toString()).contains("totally_unknown_key")

        warning.clickPositive()
        val confirmation = latestDialog()
        assertThat(shadowOf(confirmation).title.toString())
            .isEqualTo(context.getString(R.string.backup_import_confirm_title))
    }

    @Test
    fun cancellingImportConfirmationLeavesPreferencesUnchanged() {
        val json =
            PreferencesBackup.serialize(
                mapOf(PreferenceKey.MAP_SCALE.toString() to 125),
                appVersionCode = 1,
            )
        fragment.importPreferences(writeBackup(json))

        val before = PreferencesBackup.snapshot(preferences)
        latestDialog().clickNegative()

        assertThat(PreferencesBackup.snapshot(preferences)).isEqualTo(before)
    }

    @Test
    fun importConfirmationListsMissingKeys() {
        val json =
            PreferencesBackup.serialize(
                mapOf(PreferenceKey.MAP_SCALE.toString() to 125),
                appVersionCode = 1,
            )
        preferences.edit(commit = true) { putString(PreferenceKey.MAP_TYPE.toString(), "SATELLITE") }

        fragment.importPreferences(writeBackup(json))

        val confirmation = latestDialog()
        assertThat(shadowOf(confirmation).message.toString())
            .contains(context.getString(R.string.backup_import_missing_keys).substringBefore(" %s"))
    }

    // --- reset ----------------------------------------------------------------

    @Test
    fun resetPreferenceClickConfirmsAndRestoresDefaults() {
        preferences.edit(commit = true) {
            putInt(PreferenceKey.MAP_SCALE.toString(), 25)
            putString(PreferenceKey.USERNAME.toString(), "user@example.com")
        }

        resetPreference().performClick()
        val confirmation = latestDialog()
        assertThat(shadowOf(confirmation).title.toString())
            .isEqualTo(context.getString(R.string.backup_reset_confirm_title))

        confirmation.clickPositive()

        assertThat(preferences.getInt(PreferenceKey.MAP_SCALE.toString(), -1)).isEqualTo(75)
        assertThat(preferences.getString(PreferenceKey.USERNAME.toString(), null)).isEmpty()

        val restart = latestDialog()
        assertThat(shadowOf(restart).title.toString()).isEqualTo(context.getString(R.string.backup_restart_title))
    }

    @Test
    fun cancellingResetConfirmationLeavesPreferencesUnchanged() {
        preferences.edit(commit = true) { putInt(PreferenceKey.MAP_SCALE.toString(), 25) }

        resetPreference().performClick()
        latestDialog().clickNegative()

        assertThat(preferences.getInt(PreferenceKey.MAP_SCALE.toString(), -1)).isEqualTo(25)
        assertThat(startedActivities().map { it.component?.className }).doesNotContain(Main::class.java.name)
    }

    @Test
    fun choosingRestartLaterDoesNotRestartApp() {
        resetPreference().performClick()
        latestDialog().clickPositive()

        val restart = latestDialog()
        restart.clickNegative()

        assertThat(startedActivities().map { it.component?.className }).doesNotContain(Main::class.java.name)
    }

    // --- helpers --------------------------------------------------------------

    private fun exportPreference(): Preference = requireNotNull(fragment.findPreference<Preference>(BACKUP_EXPORT_KEY))

    private fun importPreference(): Preference = requireNotNull(fragment.findPreference<Preference>(BACKUP_IMPORT_KEY))

    private fun resetPreference(): Preference = requireNotNull(fragment.findPreference<Preference>(BACKUP_RESET_KEY))

    private fun backupFile(name: String): File = File(context.cacheDir, name)

    private fun writeBackup(json: String): Uri {
        val file = backupFile("import-${fileCounter++}.json")
        file.writeText(json)
        return Uri.fromFile(file)
    }

    private fun latestDialog(): AlertDialog =
        requireNotNull(ShadowAlertDialog.getLatestAlertDialog()) { "expected an AlertDialog to be shown" }

    private fun AlertDialog.clickPositive() {
        getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        idleMainLooper()
    }

    private fun AlertDialog.clickNegative() {
        getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        idleMainLooper()
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun startedActivities(): List<Intent> {
        val shadow = shadowOf(context as Application)
        val intents = mutableListOf<Intent>()
        var intent = shadow.nextStartedActivity
        while (intent != null) {
            intents += intent
            intent = shadow.nextStartedActivity
        }
        return intents
    }

    private fun assertLatestDialogMessage(
        titleRes: Int,
        messageRes: Int,
    ) {
        val dialog = latestDialog()
        assertThat(shadowOf(dialog).title.toString()).isEqualTo(context.getString(titleRes))
        assertThat(shadowOf(dialog).message.toString()).isEqualTo(context.getString(messageRes))
    }

    private companion object {
        const val BACKUP_EXPORT_KEY = "backup_export"
        const val BACKUP_IMPORT_KEY = "backup_import"
        const val BACKUP_RESET_KEY = "backup_reset"
    }
}
