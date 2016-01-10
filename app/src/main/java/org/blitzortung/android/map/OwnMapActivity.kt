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

import android.util.Log

import com.google.android.maps.MapActivity
import com.google.android.maps.Overlay

import org.blitzortung.android.app.Main
import org.blitzortung.android.map.overlay.LayerOverlay

import java.lang.reflect.Field
import java.util.ArrayList

abstract class OwnMapActivity : MapActivity() {

    private var overlays: MutableList<Overlay> = arrayListOf();

    lateinit var mapView: OwnMapView

    fun addOverlay(overlay: Overlay) {
        overlays.add(overlay)
    }

    fun updateOverlays() {
        val mapOverlays = mapView.overlays

        mapOverlays.clear()
        overlays.filter{it !is LayerOverlay || it.enabled}
                .forEach { mapOverlays.add(it)}

        mapView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            val mConfigField = MapActivity::class.java.getDeclaredField("mConfig")
            mConfigField.isAccessible = true

            val mConfig = mConfigField.get(this)
            if (mConfig != null) {
                val mConfigContextField = mConfig.javaClass.getDeclaredField("context")
                mConfigContextField.isAccessible = true
                mConfigContextField.set(mConfig, null)
                mConfigField.set(this, null)
            }
        } catch (e: Exception) {
            Log.w(Main.LOG_TAG, "OwnMapActivity.onDestroy() failed")
        }

    }

    override fun isRouteDisplayed(): Boolean {
        return false
    }
}
