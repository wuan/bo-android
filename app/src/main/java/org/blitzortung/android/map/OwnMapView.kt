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

import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.graphics.Point
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import java.util.Locale
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.location.LocationHandler
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView

class OwnMapView(context: Context) : MapView(context) {
    private val gestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    init {
        minZoomLevel = 1.5
        maxZoomLevel = 15.0
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val point: Point = Point()

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

            val dialogClickListener =
                DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                            preferences.edit().apply {
                                putString(PreferenceKey.LOCATION_LONGITUDE.toString(), roundCoordinate(longitude))
                                putString(PreferenceKey.LOCATION_LATITUDE.toString(), roundCoordinate(latitude))
                                putString(PreferenceKey.LOCATION_MODE.toString(), LocationHandler.MANUAL_PROVIDER)
                                apply()
                            }
                        }

                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                }

            AlertDialog.Builder(context)
                .setMessage("$locationText: %.4f %.4f?".format(longitude, latitude))
                .setPositiveButton(android.R.string.ok, dialogClickListener)
                .setNegativeButton(android.R.string.cancel, dialogClickListener)
                .show()
        }

        private fun roundCoordinate(value: Double): String = String.format(Locale.ROOT, COORDINATE_FORMAT, value)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    val popup: View by lazy { LayoutInflater.from(context).inflate(R.layout.popup, this, false) }

    private val animatorField = MapController::class.java.getDeclaredField("mCurrentAnimator")

    fun animator(): ValueAnimator? {
        if (!isAnimating) return null

        animatorField.isAccessible = true
        val value = animatorField.get(controller)
        return value?.let { value as ValueAnimator }
    }

    companion object {
        const val DEFAULT_ZOOM_SPEED = 500L
        const val COORDINATE_FORMAT = "%.4f"
    }
}
