package org.blitzortung.android.data

import com.google.android.maps.GeoPoint

object Coordsys {

    fun toMapCoords(longitude: Float, latitude: Float): GeoPoint {
        return GeoPoint((latitude * 1e6).toInt(), (longitude * 1e6).toInt())
    }

}
