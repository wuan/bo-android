package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.location.LocationManager.PASSIVE_PROVIDER
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.LocationProviderRelation
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.blitzortung.android.location.LocationHandler.Companion.MANUAL_PROVIDER
import org.blitzortung.android.location.provider.ManualLocationProvider

class LocationPermissionRequester(
    private val activity: Activity,
    private val preferences: SharedPreferences,
) : PermissionRequester {
    override val name: String = "location"

    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        val (permission, requestCode) = getLocationPermission(preferences)

        return if (permission != null) {
            permissionsSupport.request(
                permission,
                requestCode,
                R.string.location_permission_required,
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
        val locationProviderRelation = LocationProviderRelation.byOrdinal[requestCode]
        val alertEnabled = preferences.get(PreferenceKey.ALERT_ENABLED, false)
        return if (locationProviderRelation != null) {
            val providerName = locationProviderRelation.providerName
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val previousValue = preferences.get(PreferenceKey.LOCATION_MODE, "n/a")
                Log.i(
                    LOG_TAG,
                    "Main.onRequestPermissionResult() $providerName permission has been granted. (code $requestCode, previous: $previousValue)",
                )
                preferences.edit {
                    put(PreferenceKey.LOCATION_MODE, providerName)
                }
            } else {
                Log.i(
                    LOG_TAG,
                    "Main.onRequestPermissionResult() $providerName permission was NOT granted. (code $requestCode)",
                )
                preferences.edit {
                    put(PreferenceKey.LOCATION_MODE, MANUAL_PROVIDER)
                }
                if (alertEnabled && ManualLocationProvider.getManualLocation(preferences) == null) {
                    Toast.makeText(activity, R.string.location_required_for_alerts, Toast.LENGTH_LONG).show()
                    preferences.edit {
                        put(PreferenceKey.ALERT_ENABLED, false)
                        put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0")
                    }
                }
            }
            true
        } else {
            false
        }
    }

    companion object {
        internal fun getLocationPermission(preferences: SharedPreferences): Pair<String?, Int> {
            val locationProviderName = preferences.get(PreferenceKey.LOCATION_MODE, PASSIVE_PROVIDER)
            val permission =
                when (locationProviderName) {
                    PASSIVE_PROVIDER, GPS_PROVIDER -> ACCESS_FINE_LOCATION
                    NETWORK_PROVIDER -> ACCESS_COARSE_LOCATION
                    else -> null
                }
            val requestCode =
                (LocationProviderRelation.Companion.byProviderName[locationProviderName]?.ordinal ?: Int.MAX_VALUE)
            return Pair(permission, requestCode)
        }
    }
}
