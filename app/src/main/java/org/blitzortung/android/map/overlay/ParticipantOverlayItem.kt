package org.blitzortung.android.map.overlay

import com.google.android.maps.OverlayItem

import org.blitzortung.android.data.Coordsys
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Station.State

class ParticipantOverlayItem(station: Station) : OverlayItem(Coordsys.toMapCoords(station.longitude, station.latitude), station.name, "") {

    val lastDataTime: Long

    val participantState: State

    init {

        lastDataTime = station.offlineSince
        participantState = station.state
    }
}
