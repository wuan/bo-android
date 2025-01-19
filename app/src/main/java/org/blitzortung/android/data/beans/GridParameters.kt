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
import org.blitzortung.android.data.Coordsys
import org.osmdroid.views.Projection
import java.io.Serializable

data class GridParameters(
    val longitudeStart: Double,
    val latitudeStart: Double,
    val longitudeDelta: Double,
    val latitudeDelta: Double,
    val longitudeBins: Int,
    val latitudeBins: Int,
    val size: Int? = null
) : Serializable {

    val rectCenterLongitude: Double = longitudeStart + longitudeDelta * longitudeBins / 2.0

    val rectCenterLatitude: Double = latitudeStart - latitudeDelta * latitudeBins / 2.0

    val longitudeEnd: Double = longitudeStart + longitudeDelta * longitudeBins

    val latitudeEnd: Double = latitudeStart - latitudeDelta * latitudeBins

    fun getCenterLongitude(offset: Int): Double {
        return longitudeStart + longitudeDelta * (offset + 0.5)
    }

    fun getCenterLatitude(offset: Int): Double {
        return latitudeStart - latitudeDelta * (offset + 0.5)
    }

    val longitudeInterval: Double = longitudeDelta * longitudeBins

    val latitudeInterval: Double = latitudeDelta * latitudeBins

    val isGlobal = longitudeInterval > 350 && latitudeInterval > 170

    fun getRect(projection: Projection): RectF {
        var leftTop = Point()
        leftTop = projection.toPixels(
            Coordsys.toMapCoords(longitudeStart, latitudeStart), leftTop
        )
        var bottomRight = Point()
        val longitudeEnd = longitudeStart + longitudeInterval
        val latitudeEnd = latitudeStart - latitudeInterval
        bottomRight = projection.toPixels(
            Coordsys.toMapCoords(
                longitudeEnd,
                latitudeEnd
            ), bottomRight
        )

        // Log.d(Main.LOG_TAG, "GridParameters.getRect() $longitudeStart - $longitudeEnd ($longitudeDelta, #$longitudeBins) $latitudeEnd - $latitudeStart ($latitudeDelta, #$latitudeBins)")
        return RectF(leftTop.x.toFloat(), leftTop.y.toFloat(), bottomRight.x.toFloat(), bottomRight.y.toFloat())
    }

    fun contains(longitude: Double, latitude: Double, inset: Double = 0.0): Boolean {
        val inLongitude = longitude in longitudeStart + inset..longitudeEnd - inset
        val inLatitude = latitude in latitudeStart - latitudeDelta * latitudeBins + inset .. latitudeStart - inset
        return inLongitude && inLatitude
    }
}
