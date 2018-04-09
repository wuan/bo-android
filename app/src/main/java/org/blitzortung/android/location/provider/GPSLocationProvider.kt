package org.blitzortung.android.location.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v4.content.PermissionChecker

class GPSLocationProvider(context: Context,
                          backgroundMode: Boolean,
                          locationUpdate: (Location?) -> Unit)
: ManagerLocationProvider(context, backgroundMode, locationUpdate, LocationManager.GPS_PROVIDER) {

    override val minTime: Long
        get() = if(isInBackground) 1200 else 1000

    override val isPermissionGranted: Boolean
        get() = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}