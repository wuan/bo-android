package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.Main.Companion.REQUEST_CODE_BACKGROUND_LOCATION
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.permission.requester.LocationPermissionRequester.Companion.getLocationPermission
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.blitzortung.android.util.isAtLeast
import kotlin.text.toInt

class BackgroundLocationPermissionRequester(
    private val activity: Activity,
    private val preferences: SharedPreferences
) : PermissionRequester {
    override val name: String = "background location"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        return if (isAtLeast(Build.VERSION_CODES.Q) &&
            isBackgroundAlertEnabled(preferences) &&
            activity.checkSelfPermission(ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            getLocationPermission(preferences).first != null
        ) {
            Log.v(LOG_TAG, "Main.requestLocationPermissions() open background permission dialog")
            val message = activity.resources.getString(R.string.location_permission_background_disclosure)
            AlertDialog.Builder(activity).setMessage(message).setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    permissionsSupport.request(
                        ACCESS_BACKGROUND_LOCATION,
                        REQUEST_CODE_BACKGROUND_LOCATION,
                        R.string.location_permission_background_required
                    )
                }.setNegativeButton(android.R.string.cancel) { _, _ ->
                    preferences.edit { put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0") }
                }.show()
            true
        } else {
            false
        }
    }

    companion object {
        internal fun isBackgroundAlertEnabled(preferences: SharedPreferences) : Boolean =
            preferences.get(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0")
                .toInt() > 0
        const val REQUEST_CODE_BACKGROUND_LOCATION = 102
    }
}
