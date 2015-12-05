package org.blitzortung.android.map.overlay

import android.location.Location

import com.google.android.maps.OverlayItem

import org.blitzortung.android.data.Coordsys

class OwnLocationOverlayItem(location: Location, val radius: Float) : OverlayItem(Coordsys.toMapCoords(location.longitude.toFloat(), location.latitude.toFloat()), "", "")
