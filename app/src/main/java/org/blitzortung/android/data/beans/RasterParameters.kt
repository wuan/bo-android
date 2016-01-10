/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

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
