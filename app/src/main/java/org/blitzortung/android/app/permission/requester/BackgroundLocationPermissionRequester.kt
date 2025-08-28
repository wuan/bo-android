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
import org.blitzortung.android.app.permission.PermissionsHelper
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.util.isAtLeast

class BackgroundLocationPermissionRequester(
    private val permissionsManager: PermissionsHelper,
    private val activity: Activity,
    private val preferences: SharedPreferences,
    private val backgroundAlertEnabled: Boolean
) : PermissionRequester {
    override val name: String = "background location"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun request(): Boolean {
        return if (isAtLeast(Build.VERSION_CODES.Q) && backgroundAlertEnabled && activity.checkSelfPermission(
                ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.v(LOG_TAG, "Main.requestLocationPermissions() open background permission dialog")
            val locationText = activity.resources.getString(R.string.location_permission_background_disclosure)
            AlertDialog.Builder(activity).setMessage(locationText).setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    permissionsManager.requestPermission(
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
}
