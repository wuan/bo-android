package org.blitzortung.android.location

import android.location.Location

import org.blitzortung.android.protocol.Event

class LocationEvent(val location: Location? = null) : Event {
}
