package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.SharedPreferences
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.location.LocationManager.PASSIVE_PROVIDER
import android.os.Build
import androidx.annotation.RequiresApi
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.LocationProviderRelation
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get

class LocationPermissionRequester(
    private val sharedPreferences: SharedPreferences
) : PermissionRequester {
    override val name: String = "location"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        val (permission, requestCode) = getLocationPermission(sharedPreferences)

        return if (permission != null) {
            permissionsSupport.requestPermission(
                permission, requestCode, R.string.location_permission_required
            )
        } else {
            false
        }
    }

    companion object {
        internal fun getLocationPermission(sharedPreferences: SharedPreferences): Pair<String?, Int> {
            val locationProviderName = sharedPreferences.get(PreferenceKey.LOCATION_MODE, PASSIVE_PROVIDER)
            val permission = when (locationProviderName) {
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
