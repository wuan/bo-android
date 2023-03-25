package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.blitzortung.android.app.Main.Companion.LOG_TAG

abstract class ManagerLocationProvider(
    protected val context: Context,
    protected var isInBackground: Boolean,
    locationUpdate: (Location?) -> Unit,
    override val type: String
) : LocationProvider(locationUpdate), LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    protected open val minTime: Long
        get() = if (isInBackground) 120000 else 20000

    protected open val minDistance: Float
        get() = if (isInBackground) 200f else 50f

    override fun start() {
        Log.v(LOG_TAG, "ManagerLocationProvider.start()")
        //Don't start the LocationProvider if we dont have any permissions
        if (!this.isPermissionGranted) {
            Log.d(LOG_TAG, "Tried to start provider '$type' without permission granted")
            return
        }

        super.start()

        Log.v(
            LOG_TAG,
            "ManagerLocationProvider.start() background: $isInBackground, type: $type, minTime: $minTime, minDistance: $minDistance"
        )
        if (locationManager.allProviders.contains(type)) {
            try {
                enableLocationManager()
            } catch (securityException: SecurityException) {
                Toast.makeText(context, failedToEnableMessage, Toast.LENGTH_LONG).show()
                Log.e(LOG_TAG, failedToEnableMessage, securityException)
            }
        } else {
            val message = "location provider $type is not available"
            Toast.makeText(context, "Warning:\n$message", Toast.LENGTH_LONG).show()
            Log.w(LOG_TAG, message)
        }
    }

    @Throws(SecurityException::class)
    private fun enableLocationManager() {
        Log.v(LOG_TAG, "enableLocationmanager() $type")
        updateToLastKnown()
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

    override fun onLocationChanged(location: Location) {
        sendLocationUpdate(location)
    }

    override fun onProviderDisabled(provider: String) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override val isEnabled: Boolean
        get() = locationManager.isProviderEnabled(type)


    override fun shutdown() {
        locationManager.removeUpdates(this)

        super.shutdown()
    }

    override fun reconfigureProvider(isInBackground: Boolean) {
        this.isInBackground = isInBackground

        if (!isRunning) {
            Log.w(LOG_TAG, "Provider MUST NOT be reconfigured when its not running")

            return
        }

        Log.d(LOG_TAG, "ManagerLocationProvider: Reconfigure provider, background: ${this.isInBackground}")

        locationManager.removeUpdates(this)
        try {
            updateToLastKnown()
            locationManager.requestLocationUpdates(type, minTime, minDistance, this)
        } catch (securityException: SecurityException) {
            Toast.makeText(context, failedToEnableMessage, Toast.LENGTH_LONG).show()
            Log.e(LOG_TAG, failedToEnableMessage, securityException)
        } catch (runtimeException: RuntimeException) {
            Toast.makeText(context, failedToReconfigureMessage, Toast.LENGTH_LONG).show()
            Log.e(LOG_TAG, failedToReconfigureMessage, runtimeException)
        }
    }

    private val failedToReconfigureMessage
        get() = "could not reconfigure location manager $type "

    private val failedToEnableMessage
        get() = "could not enable location manager $type"

    private fun updateToLastKnown() {
        val location = try {
            locationManager.getLastKnownLocation(type)
        } catch (securityException: SecurityException) {
            null
        }
        Log.v(LOG_TAG, "ManagerLocationProvider: last known $location")
        if (location != null) {
            onLocationChanged(location)
        }
    }

    abstract val isPermissionGranted: Boolean
}
