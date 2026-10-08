package org.blitzortung.android.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import dagger.android.support.AndroidSupportInjection
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.MessageListPreference
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.viewmodel.SettingsViewModel
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.location.LocationHandler

class SettingsFragment : PreferenceFragmentCompat() {
    @set:Inject
    internal lateinit var preferences: SharedPreferences

    @set:Inject
    internal lateinit var packageInfo: PackageInfo

    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory }

    private val originalSummaries = mutableMapOf<PreferenceKey, Int>()

    // Modern Activity Result API for ringtone picker
    private val ringtonePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let { data ->
                val ringtone = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                if (::preferences.isInitialized) {
                    preferences.edit(commit = true) {
                        putString(
                            PreferenceKey.ALERT_SOUND_SIGNAL.toString(),
                            ringtone?.toString() ?: "",
                        )
                    }
                } else {
                    Log.e(LOG_TAG, "SharedPreferences not initialized when handling ringtone result")
                }
            }
        }

    private val exportPreferencesLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { exportPreferences(it) }
        }

    private val importPreferencesLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importPreferences(it) }
        }

    override fun onAttach(context: Context) {
        Log.v(LOG_TAG, "SettingsFragment.onAttach()")
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        Log.v(LOG_TAG, "SettingsFragment.onCreatePreferences()")

        addPreferencesFromResource(R.xml.preferences)

        if (::preferences.isInitialized) {
            configureDataSourcePreferences()
            configureLocationProviderPreferences()
            configureOwnLocationSizePreference()
            configureAlertEnabledPreference()
            configureBackupPreferences()

            // Initial summary update for EditTextPreferences
            updateEditTextPreferenceSummaries()
        } else {
            Log.e(LOG_TAG, "SharedPreferences not initialized in SettingsFragment.onCreatePreferences")
        }
    }

    private fun updateEditTextPreferenceSummaries() {
        listOf(
            PreferenceKey.USERNAME,
            PreferenceKey.PASSWORD,
            PreferenceKey.SERVICE_URL,
            PreferenceKey.LOCATION_LONGITUDE,
            PreferenceKey.LOCATION_LATITUDE,
            PreferenceKey.ALERT_SOUND_SIGNAL,
        ).forEach { key ->
            findPreference<Preference>(key)?.let { updatePreferenceSummary(it) }
        }
    }

    private fun updatePreferenceSummary(preference: Preference) {
        val keyString = preference.key
        val preferenceKey = PreferenceKey.fromString(keyString)

        val currentValue = preferences.getString(preference.key, null)?.let { extractURITitle(it) }
        val originalSummary = originalSummaries[preferenceKey]?.let { getString(it) } ?: ""
        val undefinedSummary = context?.getString(R.string.undefined)

        if (preferenceKey == PreferenceKey.PASSWORD) {
            preference.summary =
                if (!currentValue.isNullOrEmpty()) {
                    "********"
                } else {
                    originalSummary
                }
        } else if (originalSummary.contains("%s")) {
            preference.summary = String.format(originalSummary, currentValue ?: "")
        } else {
            if (!currentValue.isNullOrEmpty()) {
                val summary = if (originalSummary.isEmpty()) "" else "$originalSummary: "
                preference.summary = "$summary$currentValue"
            } else {
                preference.summary = originalSummary.ifEmpty { undefinedSummary }
            }
        }
    }

    private fun extractURITitle(value: String): String =
        if (RingtoneManager.isDefault(value.toUri())) {
            context?.getString(R.string.default_alarm_signal).toString()
        } else {
            value.toUri().getQueryParameter("title") ?: value
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = listView
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observePreferenceChanges()
    }

    private fun observePreferenceChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.preferenceChanged.collect { key ->
                    handlePreferenceChange(key)
                }
            }
        }
    }

    private fun handlePreferenceChange(key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE -> configureDataSourcePreferences()
            PreferenceKey.LOCATION_MODE -> {
                val provider = configureLocationProviderPreferences()
                val context = this.context
                if (context != null && provider != LocationHandler.MANUAL_PROVIDER &&
                    !(context.getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(provider)
                ) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }

            PreferenceKey.SHOW_LOCATION -> configureOwnLocationSizePreference()
            PreferenceKey.ALERT_ENABLED -> configureAlertEnabledPreference()
            PreferenceKey.USERNAME,
            PreferenceKey.PASSWORD,
            PreferenceKey.SERVICE_URL,
            PreferenceKey.LOCATION_LONGITUDE,
            PreferenceKey.LOCATION_LATITUDE,
            PreferenceKey.ALERT_SOUND_SIGNAL,
                -> {
                findPreference<Preference>(key)?.let { updatePreferenceSummary(it) }
            }

            else -> {
                // No action needed for other keys
            }
        }
    }

    private fun configureAlertEnabledPreference() {
        enableNotifications(viewModel.getBooleanPreference(PreferenceKey.ALERT_ENABLED, false))
    }

    private fun configureOwnLocationSizePreference() {
        findPreference<SeekBarPreference>(PreferenceKey.OWN_LOCATION_SIZE.toString())?.isEnabled =
            viewModel.getBooleanPreference(PreferenceKey.SHOW_LOCATION, false)
    }

    private fun configureDataSourcePreferences(): DataProviderType {
        val providerTypeString =
            viewModel.getStringPreference(PreferenceKey.DATA_SOURCE, DataProviderType.HTTP.toString())
        val providerType = DataProviderType.valueOf(providerTypeString.uppercase(Locale.getDefault()))

        when (providerType) {
            DataProviderType.HTTP -> enableBlitzortungHttpMode()
            DataProviderType.RPC -> enableAppServiceMode()
        }
        return providerType
    }

    private fun configureLocationProviderPreferences(): String {
        val locationProvider =
            viewModel.getStringPreference(PreferenceKey.LOCATION_MODE, LocationManager.NETWORK_PROVIDER)
        enableManualLocationMode(locationProvider == LocationHandler.MANUAL_PROVIDER)
        return locationProvider
    }

    private fun enableAppServiceMode() {
        findPreference<ListPreference>(PreferenceKey.GRID_SIZE)?.isEnabled = true
        findPreference<EditTextPreference>(PreferenceKey.SERVICE_URL)?.isEnabled = true
        findPreference<EditTextPreference>(PreferenceKey.USERNAME)?.isEnabled = false
        findPreference<EditTextPreference>(PreferenceKey.PASSWORD)?.isEnabled = false
    }

    private fun enableBlitzortungHttpMode() {
        findPreference<ListPreference>(PreferenceKey.GRID_SIZE)?.isEnabled = false
        findPreference<EditTextPreference>(PreferenceKey.SERVICE_URL)?.isEnabled = false
        findPreference<EditTextPreference>(PreferenceKey.USERNAME)?.isEnabled = true
        findPreference<EditTextPreference>(PreferenceKey.PASSWORD)?.isEnabled = true
    }

    private fun enableManualLocationMode(enabled: Boolean) {
        findPreference<EditTextPreference>(PreferenceKey.LOCATION_LONGITUDE)?.isEnabled = enabled
        findPreference<EditTextPreference>(PreferenceKey.LOCATION_LATITUDE)?.isEnabled = enabled
    }

    private fun enableNotifications(enabled: Boolean) {
        findPreference<MessageListPreference>(PreferenceKey.BACKGROUND_QUERY_PERIOD)?.isEnabled = enabled
        findPreference<ListPreference>(PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT)?.isEnabled = enabled
        findPreference<ListPreference>(PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT)?.isEnabled = enabled
        findPreference<Preference>(PreferenceKey.ALERT_SOUND_SIGNAL)?.isEnabled = enabled
        findPreference<SeekBarPreference>(PreferenceKey.ALERT_VIBRATION_SIGNAL)?.isEnabled = enabled
        findPreference<ListPreference>(PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME)?.isEnabled = enabled
    }

    private fun configureBackupPreferences() {
        findPreference<Preference>(BACKUP_EXPORT_KEY)?.setOnPreferenceClickListener {
            exportPreferencesLauncher.launch(getString(R.string.backup_export_file_name))
            true
        }
        findPreference<Preference>(BACKUP_IMPORT_KEY)?.setOnPreferenceClickListener {
            importPreferencesLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            true
        }
        findPreference<Preference>(BACKUP_RESET_KEY)?.setOnPreferenceClickListener {
            confirmReset()
            true
        }
    }

    internal fun exportPreferences(uri: Uri) {
        val context = context ?: return
        val json =
            PreferencesBackup.serialize(
                PreferencesBackup.snapshot(preferences),
                PackageInfoCompat.getLongVersionCode(packageInfo).toInt(),
            )
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("no output stream for $uri")
            Toast.makeText(context, R.string.backup_export_success, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "failed to export preferences", e)
            Toast.makeText(context, R.string.backup_export_failed, Toast.LENGTH_LONG).show()
        }
    }

    internal fun importPreferences(uri: Uri) {
        val context = context ?: return
        val json =
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "failed to read backup file", e)
                null
            }

        if (json == null) {
            showMessage(getString(R.string.backup_import_read_failed))
            return
        }

        when (val parsed = PreferencesBackup.parse(json)) {
            is BackupParseResult.Failure -> {
                val messageRes =
                    when (parsed.reason) {
                        BackupFailureReason.MALFORMED_JSON,
                        BackupFailureReason.INVALID_PREFERENCES,
                        -> R.string.backup_import_read_failed

                        BackupFailureReason.NOT_A_BACKUP,
                        BackupFailureReason.UNSUPPORTED_VERSION,
                        -> R.string.backup_import_unsupported
                    }
                showMessage(getString(messageRes))
            }

            is BackupParseResult.Success -> {
                when (val planResult = PreferencesBackup.planImport(parsed.preferences, PreferencesBackup.snapshot(preferences))) {
                    is ImportPlanResult.Failure ->
                        showMessage(getString(R.string.backup_import_invalid_value, planResult.key), asError = true)

                    is ImportPlanResult.Success -> confirmImport(planResult.plan)
                }
            }
        }
    }

    private fun confirmImport(plan: ImportPlan) {
        if (plan.unknownKeys.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_import_warning_title)
                .setMessage(
                    getString(R.string.backup_import_unknown_keys, plan.unknownKeys.joinToString(", ")),
                )
                .setNegativeButton(R.string.backup_cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ -> showImportConfirmation(plan) }
                .show()
        } else {
            showImportConfirmation(plan)
        }
    }

    private fun showImportConfirmation(plan: ImportPlan) {
        val message =
            buildString {
                append(getString(R.string.backup_import_confirm_message))
                if (plan.missingKeys.isNotEmpty()) {
                    append("\n\n")
                    append(getString(R.string.backup_import_missing_keys, plan.missingKeys.joinToString(", ")))
                }
            }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_import_confirm_title)
            .setMessage(message)
            .setNegativeButton(R.string.backup_cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val context = context ?: return@setPositiveButton
                PreferencesBackup.applyImport(context, preferences, plan)
                promptRestart()
            }
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_reset_confirm_title)
            .setMessage(R.string.backup_reset_confirm_message)
            .setNegativeButton(R.string.backup_cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val context = context ?: return@setPositiveButton
                PreferencesBackup.resetToDefaults(context, preferences)
                promptRestart()
            }
            .show()
    }

    private fun promptRestart() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_restart_title)
            .setMessage(R.string.backup_restart_message)
            .setCancelable(false)
            .setNegativeButton(R.string.backup_restart_later, null)
            .setPositiveButton(R.string.backup_restart_now) { _, _ -> restartApp() }
            .show()
    }

    private fun restartApp() {
        val context = context ?: return
        context.stopService(Intent(context, AppService::class.java))
        val intent =
            Intent(context, Main::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        context.startActivity(intent)
        activity?.finish()
    }

    private fun showMessage(
        message: CharSequence,
        asError: Boolean = false,
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(if (asError) R.string.backup_import_warning_title else R.string.app_name)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean =
        if (preference.key == PreferenceKey.ALERT_SOUND_SIGNAL.toString()) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)

            val existingValue: String? =
                if (::preferences.isInitialized) {
                    preferences.getString(PreferenceKey.ALERT_SOUND_SIGNAL, null)
                } else {
                    Log.e(LOG_TAG, "SharedPreferences not initialized in onPreferenceTreeClick")
                    null
                }

            if (existingValue != null) {
                if (existingValue.isEmpty()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingValue.toUri())
                }
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            }

            ringtonePickerLauncher.launch(intent)
            true
        } else {
            super.onPreferenceTreeClick(preference)
        }

    companion object {
        private const val BACKUP_EXPORT_KEY = "backup_export"
        private const val BACKUP_IMPORT_KEY = "backup_import"
        private const val BACKUP_RESET_KEY = "backup_reset"
    }
}

fun <T : Preference> PreferenceFragmentCompat.findPreference(key: PreferenceKey): T? = findPreference<T>(key.toString())

fun SharedPreferences.getString(
    key: PreferenceKey,
    defValue: String?,
): String? = getString(key.toString(), defValue)

fun SharedPreferences.getInt(
    key: PreferenceKey,
    defValue: Int,
): Int = getInt(key.toString(), defValue)

fun SharedPreferences.Editor.putString(
    key: PreferenceKey,
    value: String?,
): SharedPreferences.Editor = putString(key.toString(), value)

fun SharedPreferences.Editor.putInt(
    key: PreferenceKey,
    value: Int,
): SharedPreferences.Editor = putInt(key.toString(), value)
