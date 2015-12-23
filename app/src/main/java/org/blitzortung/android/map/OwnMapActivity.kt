package org.blitzortung.android.map

import android.util.Log

import com.google.android.maps.MapActivity
import com.google.android.maps.Overlay

import org.blitzortung.android.app.Main
import org.blitzortung.android.map.overlay.LayerOverlay

import java.lang.reflect.Field
import java.util.ArrayList

abstract class OwnMapActivity : MapActivity() {

    internal var overlays: MutableList<Overlay> = ArrayList()
    lateinit var mapView: OwnMapView

    fun addOverlay(overlay: Overlay) {
        overlays.add(overlay)
    }

    fun updateOverlays() {
        val mapOverlays = mapView!!.overlays

        mapOverlays.clear()

        for (overlay in overlays) {
            var enabled = true

            if (overlay is LayerOverlay) {
                enabled = overlay.enabled
            }

            if (enabled) {
                mapOverlays.add(overlay)
            }
        }

        mapView!!.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            val mConfigField = MapActivity::class.java.getDeclaredField("mConfig")
            mConfigField.isAccessible = true

            val mConfig = mConfigField.get(this)
            if (null != mConfig) {
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
