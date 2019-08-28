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

package org.blitzortung.android.map

import android.content.Context
import android.content.DialogInterface
import android.graphics.Point
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences
import org.osmdroid.views.MapView


class OwnMapView(context: Context) : MapView(context) {

    private val gestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    init {
        minZoomLevel = 1.5
        maxZoomLevel = 15.0
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val point : Point = Point()

        override fun onDoubleTap(event: MotionEvent): Boolean {

            this@OwnMapView.removeView(popup)
            val geoPoint = this.getPoint(event)
            this@OwnMapView.projection.toPixels(geoPoint, point)
            controller.zoomInFixing(point.x, point.y, DEFAULT_ZOOM_SPEED)
            return true
        }

        private fun getPoint(event: MotionEvent) = projection.fromPixels(event.x.toInt(), event.y.toInt())

        override fun onLongPress(event: MotionEvent) {
            val point = getPoint(event)
            val longitude = point.longitude
            val latitude = point.latitude
            Log.v(Main.LOG_TAG, "GestureListener.onLongPress() $point")
            val context = this@OwnMapView.context
            val locationText = context.resources.getString(R.string.set_manual_location)

            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        val preferences = context.defaultSharedPreferences
                        preferences.edit().apply {
                            putString(PreferenceKey.LOCATION_LONGITUDE.toString(), longitude.toString())
                            putString(PreferenceKey.LOCATION_LATITUDE.toString(), latitude.toString())
                            putString(PreferenceKey.LOCATION_MODE.toString(), LocationHandler.MANUAL_PROVIDER)
                            apply()
                        }
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }

            AlertDialog.Builder(context)
                    .setMessage("%s: %.4f %.4f?".format(locationText, longitude, latitude))
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener)
                    .show()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }



    val popup: View by lazy { LayoutInflater.from(context).inflate(R.layout.popup, this, false) }

    companion object {
        const val DEFAULT_ZOOM_SPEED = 500L
    }
}
