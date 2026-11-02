package org.blitzortung.android.location.provider

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.PermissionChecker
import javax.inject.Singleton

@Singleton
class GPSLocationProvider(
    context: Context,
    backgroundMode: Boolean,
    locationUpdate: (Location?) -> Unit,
) : ManagerLocationProvider(context, backgroundMode, locationUpdate, LocationManager.GPS_PROVIDER) {
    override val minTime: Long
        get() = if (isInBackground) 60000 else 1000

    override val isPermissionGranted: Boolean
        get() =
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PermissionChecker.PERMISSION_GRANTED
}
