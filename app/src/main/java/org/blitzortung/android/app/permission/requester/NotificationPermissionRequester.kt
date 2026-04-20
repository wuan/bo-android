package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.permission.requester.BackgroundLocationPermissionRequester.Companion.isBackgroundAlertEnabled
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put

class NotificationPermissionRequester(
    private val activity: Activity,
    val preferences: SharedPreferences,
) : PermissionRequester {
    override val name: String = "notification"

    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        val requiresNotifications = isAlertEnabled(preferences) || isBackgroundAlertEnabled(preferences)
        Log.v(LOG_TAG, "NotificationPermissionRequester.request() requiresNotifications: $requiresNotifications")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && requiresNotifications) {
            permissionsSupport.request(
                POST_NOTIFICATIONS,
                REQUEST_CODE_POST_NOTIFICATIONS,
                R.string.post_notifications_request,
            )
        } else {
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ): Boolean {
        val alertEnabled = isAlertEnabled(preferences)
        val backgroundAlertEnabled = isBackgroundAlertEnabled(preferences)

        return if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS && grantResults.isNotEmpty()) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED && (alertEnabled || backgroundAlertEnabled)) {
                Log.i(
                    LOG_TAG,
                    "Main.onRequestPermissionResult() POST_NOTIFICATIONS permission was NOT granted but is required for alerts. Disabling alerts and background queries",
                )
                Toast.makeText(activity, R.string.post_notifications_required_for_alerts, Toast.LENGTH_LONG).show()
                preferences.edit {
                    put(PreferenceKey.ALERT_ENABLED, false)
                    put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0")
                }
            }
            true
        } else {
            false
        }
    }

    companion object {
        internal fun isAlertEnabled(preferences: SharedPreferences): Boolean =
            preferences.get(PreferenceKey.ALERT_ENABLED, false)

        const val REQUEST_CODE_POST_NOTIFICATIONS = 101
    }
}
