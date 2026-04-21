package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.permission.requester.LocationPermissionRequester.Companion.getLocationPermission
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.util.isAtLeast

class BackgroundLocationPermissionRequester(
    private val activity: Activity,
    private val preferences: SharedPreferences,
) : PermissionRequester {
    override val name: String = "background location"

    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        return if (isAtLeast(Build.VERSION_CODES.Q) &&
            isBackgroundAlertEnabled(preferences) &&
            wasDisclosureShown(preferences) &&
            activity.checkSelfPermission(ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            getLocationPermission(preferences).first != null
        ) {
            Log.v(LOG_TAG, "Main.requestBackgroundLocationPermissions() request background permission")
            permissionsSupport.request(
                ACCESS_BACKGROUND_LOCATION,
                REQUEST_CODE_BACKGROUND_LOCATION,
                R.string.location_permission_background_required,
            )
            true
        } else {
            false
        }
    }

    private fun wasDisclosureShown(preferences: SharedPreferences): Boolean =
        preferences.get(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, false)

    companion object {
        internal fun isBackgroundAlertEnabled(preferences: SharedPreferences): Boolean =
            preferences.get(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0")
                .toInt() > 0

        const val REQUEST_CODE_BACKGROUND_LOCATION = 102
    }
}
