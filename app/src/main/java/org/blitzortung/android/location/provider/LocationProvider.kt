package org.blitzortung.android.location.provider

import android.location.Location
import android.util.Log
import org.blitzortung.android.app.Main

abstract class LocationProvider(protected val locationUpdate: (Location?) -> Unit) {
    protected var location = Location("")

    var isRunning: Boolean = false
        private set

    abstract val isEnabled: Boolean

    protected fun sendLocationUpdate() {
        locationUpdate(location)
    }

    abstract val type: String

    protected val Location.isValid: Boolean
        get() = !java.lang.Double.isNaN(longitude) && !java.lang.Double.isNaN(latitude)

    protected fun invalidateLocationAndSendLocationUpdate() {
        locationUpdate(null)

        invalidateLocation()
    }

    protected  fun invalidateLocation() {
        location = Location("")
    }

    open fun start() {
        isRunning = true
        Log.v(Main.LOG_TAG, "Provider $type started" )
    }

    open fun shutdown() {
        isRunning = false
        invalidateLocationAndSendLocationUpdate()
        Log.v(Main.LOG_TAG, "Provider $type stopped" )
    }
}