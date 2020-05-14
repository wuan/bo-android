package org.blitzortung.android.location.provider

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.PermissionChecker
import javax.inject.Singleton

@Singleton
class NetworkLocationProvider(context: Context,
                              backgroundMode: Boolean,
                              locationUpdate: (Location?) -> Unit)
: ManagerLocationProvider(context, backgroundMode, locationUpdate, LocationManager.NETWORK_PROVIDER) {

    override val isPermissionGranted: Boolean
        get() = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED
}