package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.jetbrains.anko.locationManager

abstract class ManagerLocationProvider(
        protected val context: Context,
        protected var isInBackground: Boolean = true,
        locationUpdate: (Location?) -> Unit,
        override val type: String
) : LocationProvider(locationUpdate), LocationListener {

    protected val locationManager = context.locationManager

    abstract val isPermissionGranted: Boolean

    protected open val minTime: Long
        get() = if (isInBackground) 120000 else 20000

    protected open val minDistance: Float
        get() = if (isInBackground) 200f else 50f

    override fun start() {
        //Don't start the LocationProvider if we dont have any permissions
        if (!this.isPermissionGranted) {
            Log.d(Main.LOG_TAG, "Tried to start provider '$type' without permission granted")
            return
        }

        super.start()

        Log.v(Main.LOG_TAG, "ManagerLocationProvider.start() background: $isInBackground, type: $type, minTime: $minTime, minDistance: $minDistance")
        if (locationManager.allProviders.contains(type)) {
            try {
                enableLocationManager()
            } catch (securityException: SecurityException) {
                Toast.makeText(context, "could not enable location manager $type", Toast.LENGTH_LONG).show()
                Log.e(LOG_TAG, "could not enable location manager $type", securityException)
            }
        } else {
            val message = "location provider $type is not available"
            Toast.makeText(context, "Warning:\n$message", Toast.LENGTH_LONG).show()
            Log.w(Main.LOG_TAG, message)
        }
    }

    @Throws(SecurityException::class)
    private fun enableLocationManager() {
        locationManager.requestLocationUpdates(type, minTime, minDistance, this)

        //Now try to get the last known location from the current provider
        val lastKnownLocation = locationManager.getLastKnownLocation(type)
        if (lastKnownLocation != null) {
            val secondsElapsedSinceLastFix = (System.currentTimeMillis() - lastKnownLocation.time) / 1000

            if (secondsElapsedSinceLastFix < 30 && lastKnownLocation.isValid) {
                sendLocationUpdate(lastKnownLocation)
            }
        }
    }

    override fun onLocationChanged(location: Location?) {
        //Don't send NULL locations to the listeners
        if (location is Location) {
            sendLocationUpdate(location)
        }
    }

    override fun onProviderDisabled(provider: String) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override val isEnabled: Boolean
        get() = locationManager.isProviderEnabled(type)


    override fun shutdown() {
        locationManager.removeUpdates(this)

        super.shutdown()
    }

    override fun reconfigureProvider(isInBackground: Boolean) {
        this.isInBackground = isInBackground

        if (!isRunning) {
            Log.w(Main.LOG_TAG, "Provider MUST NOT be reconfigured when its not running")

            return
        }

        Log.d(Main.LOG_TAG, "ManagerLocationProvider: Reconfigure provider, background: ${this.isInBackground}")

        locationManager.removeUpdates(this)
        try {
            locationManager.requestLocationUpdates(type, minTime, minDistance, this)
        } catch (securityException: SecurityException) {
            Toast.makeText(context, "could not enable location manager $type", Toast.LENGTH_LONG).show()
            Log.e(LOG_TAG, "could not enable location manager $type", securityException)
        } catch (runtimeException: RuntimeException) {
            Toast.makeText(context, "could not reconfigure location manager $type ", Toast.LENGTH_LONG).show()
            Log.e(LOG_TAG, "could not reconfigure location manager $type", runtimeException)
        }
    }
}
