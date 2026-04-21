package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.blitzortung.android.util.isAtLeast

class BackgroundLocationDisclosureRequester(
    private val activity: Activity,
    private val preferences: SharedPreferences,
) : PermissionRequester {
    override val name: String = "background location disclosure"

    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        return if (isAtLeast(Build.VERSION_CODES.Q) &&
            BackgroundLocationPermissionRequester.isBackgroundAlertEnabled(preferences) &&
            !wasDisclosureShown(preferences) &&
            activity.checkSelfPermission(ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            hasForegroundLocation()
        ) {
            Log.v(LOG_TAG, "Main.requestBackgroundLocationDisclosure() show disclosure")
            showProminentDisclosure()
            true
        } else {
            false
        }
    }

    private fun wasDisclosureShown(preferences: SharedPreferences): Boolean =
        preferences.get(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, false)

    private fun hasForegroundLocation(): Boolean =
        activity.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        activity.checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun showProminentDisclosure() {
        val message = activity.getString(R.string.location_permission_background_disclosure)
        AlertDialog.Builder(activity)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.i_understand) { _, _ ->
                preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true) }
            }
            .setNegativeButton(R.string.no_thanks) { _, _ ->
                preferences.edit {
                    put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0")
                    put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true)
                }
            }
            .show()
    }
}
