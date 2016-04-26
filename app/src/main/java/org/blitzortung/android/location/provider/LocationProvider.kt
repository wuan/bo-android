package org.blitzortung.android.location.provider

import android.location.Location
import android.util.Log
import org.blitzortung.android.app.Main

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
            if(this == null)
                return false

            return !java.lang.Double.isNaN(longitude) && !java.lang.Double.isNaN(latitude)
        }

    open fun start() {
        isRunning = true
        Log.v(Main.LOG_TAG, "Provider $type started" )
    }

    open fun shutdown(invalidateLocation: Boolean = true) {
        isRunning = false

        //Send the consumers a NULL location, so they know we don't have a valid location at the moment
        if(invalidateLocation)
            sendLocationUpdate(null)

        Log.v(Main.LOG_TAG, "Provider $type stopped" )
    }

    fun restart() {
        if(isRunning) {
            //We are just restarting the provider, so there is no need to invalidate the current location
            shutdown(false)
        }

        start()
    }
}