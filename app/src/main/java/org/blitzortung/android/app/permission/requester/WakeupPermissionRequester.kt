package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.app.Activity
import android.content.Context.POWER_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.AndroidRuntimeException
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.view.PreferenceKey

class WakeupPermissionRequester(
    private val activity: Activity,
    private val preferences: SharedPreferences,
): PermissionRequester {
    override val name: String = "wakeup"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        val backgroundAlertEnabled = BackgroundLocationPermissionRequester.isBackgroundAlertEnabled(preferences)

        Log.v(LOG_TAG, "requestWakeupPermissions() background alerts: $backgroundAlertEnabled")

        if (backgroundAlertEnabled) {
            val pm = activity.getSystemService(POWER_SERVICE)
            if (pm is PowerManager) {
                val packageName = activity.packageName
                Log.v(LOG_TAG, "requestWakeupPermissions() package name $packageName")
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val locationText = activity.resources.getString(R.string.open_battery_optimiziation)

                    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                disableBatteryOptimisation(packageName)
                            }

                            DialogInterface.BUTTON_NEGATIVE -> {
                                preferences.edit {
                                    putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), 0.toString())
                                    apply()
                                }
                            }
                        }
                    }

                    AlertDialog.Builder(activity).setMessage(locationText)
                        .setPositiveButton(android.R.string.ok, dialogClickListener)
                        .setNegativeButton(android.R.string.cancel, dialogClickListener).show()
                    return true
                }
            } else {
                Log.w(LOG_TAG, "requestWakeupPermissions() could not get PowerManager")
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun disableBatteryOptimisation(packageName: String?) {
        Log.v(LOG_TAG, "requestWakeupPermissions() request ignore battery optimizations")
        val allowIgnoreBatteryOptimization =
            activity.checkSelfPermission(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED
        val intent = if (allowIgnoreBatteryOptimization) {
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:$packageName".toUri()
            )
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }

        try {
            activity.startActivity(intent)
        } catch (e: AndroidRuntimeException) {
            showErrorMessage(e)
        }
    }

    private fun showErrorMessage(e: AndroidRuntimeException) {
        Toast.makeText(activity.baseContext, R.string.background_query_toast, Toast.LENGTH_LONG)
            .show()
        Log.e(
            LOG_TAG,
            "requestWakeupPermissions() could not open battery optimization settings",
            e
        )
    }
}
