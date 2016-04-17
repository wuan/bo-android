package org.blitzortung.android.location.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GpsStatus
import android.location.Location
import android.location.LocationManager
import android.support.v4.content.PermissionChecker

class GPSLocationProvider(context: Context,
                          backgroundMode: Boolean,
                          locationUpdate: (Location?) -> Unit)
: ManagerLocationProvider(context, backgroundMode, locationUpdate, LocationManager.GPS_PROVIDER), GpsStatus.Listener {
    override fun start() {
        super.start()

        locationManager.addGpsStatusListener(this)
    }

    override fun shutdown() {
        locationManager.removeGpsStatusListener(this)

        super.shutdown()
    }

    override fun onGpsStatusChanged(event: Int) {
        when (event) {
            GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                val lastKnownGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownGpsLocation != null) {
                    val secondsElapsedSinceLastFix = (System.currentTimeMillis() - lastKnownGpsLocation.time) / 1000

                    if (secondsElapsedSinceLastFix < 10) {
                        if(lastKnownGpsLocation.isValid) {
                            sendLocationUpdate(lastKnownGpsLocation)
                        }
                    }
                }
            }
        }
    }

    override val minTime: Long
        get() = if(backgroundMode) 1200 else 1000

    override val isPermissionGranted: Boolean
        get() = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}