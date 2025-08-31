package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.Main.Companion.REQUEST_CODE_POST_NOTIFICATIONS
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.permission.requester.BackgroundLocationPermissionRequester.Companion.isBackgroundAlertEnabled
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get

class NotificationPermissionRequester(
    val preferences: SharedPreferences
) : PermissionRequester {
    override val name: String = "notification"

    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        val requiresNotifications = isAlertEnabled(preferences) || isBackgroundAlertEnabled(preferences)
        Log.v(LOG_TAG, "NotificationPermissionRequester.request() requiresNotifications: $requiresNotifications")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && requiresNotifications) {
            permissionsSupport.request(POST_NOTIFICATIONS, REQUEST_CODE_POST_NOTIFICATIONS, R.string.post_notifications_request)
        } else {
            false
        }
    }

    companion object {
        internal fun isAlertEnabled(preferences: SharedPreferences) : Boolean =
            preferences.get(PreferenceKey.ALERT_ENABLED, false)
    }

}
