package org.blitzortung.android.data.beans

import android.graphics.Point
import android.graphics.RectF
import android.util.Log
import com.google.android.maps.Projection
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.Coordsys

data class RasterParameters(
        val longitudeStart: Float,
        val latitudeStart: Float,
        val longitudeDelta: Float,
        val latitudeDelta: Float,
        val longitudeBins: Int,
        val latitudeBins: Int,
        val info: String? = null) {

    val rectCenterLongitude: Float
        get() = longitudeStart + longitudeDelta * longitudeBins / 2f

    val rectCenterLatitude: Float
        get() = latitudeStart - latitudeDelta * latitudeBins / 2f

    fun getCenterLongitude(offset: Int): Float {
        return longitudeStart + longitudeDelta * (offset + 0.5f)
    }

    fun getCenterLatitude(offset: Int): Float {
        return latitudeStart - latitudeDelta * (offset + 0.5f)
    }

    val rectLongitudeDelta: Float
        get() = longitudeDelta * longitudeBins

    val rectLatitudeDelta: Float
        get() = latitudeDelta * latitudeBins

    fun getRect(projection: Projection): RectF {
        var leftTop = Point()
        leftTop = projection.toPixels(
                Coordsys.toMapCoords(longitudeStart, latitudeStart), leftTop)
        var bottomRight = Point()
        val longitudeEnd = longitudeStart + rectLongitudeDelta
        val latitudeEnd = latitudeStart - rectLatitudeDelta
        bottomRight = projection.toPixels(
                Coordsys.toMapCoords(longitudeEnd,
                        latitudeEnd), bottomRight)

        if (false) {
            Log.d(Main.LOG_TAG, "RasterParameters.getRect() " +
                    "$longitudeStart - $longitudeEnd ($longitudeDelta, #$longitudeBins) " +
                    "$latitudeEnd - $latitudeStart ($latitudeDelta, #$latitudeBins)")
        }
        return RectF(leftTop.x.toFloat(), leftTop.y.toFloat(), bottomRight.x.toFloat(), bottomRight.y.toFloat())
    }

    fun getLongitudeIndex(longitude: Double): Int {
        return ((longitude - longitudeStart) / longitudeDelta + 0.5).toInt()
    }

    fun getLatitudeIndex(latitude: Double): Int {
        return ((latitudeStart - latitude) / latitudeDelta + 0.5).toInt()
    }
}
