package org.blitzortung.android.location.provider

import android.location.Location
import android.util.Log
import org.blitzortung.android.app.Main
import java.lang.Double.isNaN

abstract class LocationProvider(protected val locationUpdate: (Location?) -> Unit) {
    var isRunning: Boolean = false
        private set

    abstract val isEnabled: Boolean

    protected fun sendLocationUpdate(location: Location?) {
        locationUpdate(location)
    }

    abstract val type: String

    protected val Location?.isValid: Boolean
        get() {
            if (this == null) {
                return false
            }

            return !isNaN(longitude) && !isNaN(latitude)
        }

    open fun start() {
        isRunning = true

        Log.v(Main.LOG_TAG, "LocationProvider.start() type: $type")
    }

    open fun shutdown() {
        isRunning = false

        //Invalidate the current location, when the provider is stopped
        sendLocationUpdate(null)

        Log.v(Main.LOG_TAG, "LocationProvider.shutdown() type: $type")
    }

    abstract fun reconfigureProvider(isInBackground: Boolean)
}
