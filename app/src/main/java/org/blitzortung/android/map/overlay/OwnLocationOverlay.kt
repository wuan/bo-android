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

package org.blitzortung.android.map.overlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.getAndConvert
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.components.LayerOverlayComponent
import org.blitzortung.android.util.TabletAwareView
import org.osmdroid.api.IMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedOverlay

class OwnLocationOverlay(
        context: Context,
        private val mapView: MapView
) : ItemizedOverlay<OwnLocationOverlayItem>(OwnLocationOverlay.DEFAULT_DRAWABLE),
        SharedPreferences.OnSharedPreferenceChangeListener,
        LayerOverlay, MapListener {
    private val layerOverlayComponent: LayerOverlayComponent

    private var item: OwnLocationOverlayItem? = null

    private var symbolSize: Float = 1.0f

    private val sizeFactor: Float

    private var zoomLevel: Double = 0.0

    val locationEventConsumer: (LocationEvent) -> Unit = { event ->
        val location = event.location

        if (isEnabled) {
            item = location?.run { OwnLocationOverlayItem(location) }

            populate()
            refresh()
        }
    }

    init {
        layerOverlayComponent = LayerOverlayComponent(context.resources.getString(R.string.own_location_layer))

        item = null

        populate()

        sizeFactor = ViewHelper.pxFromDp(context, 1.0f) * TabletAwareView.sizeFactor(context)

        val preferences = BOApplication.sharedPreferences
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_LOCATION)
        onSharedPreferenceChanged(preferences, PreferenceKey.OWN_LOCATION_SIZE)

        refresh()
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (!shadow) {
            super.draw(canvas, mapView, false)
        }
    }

    private fun refresh() {
        item?.run { setMarker(ShapeDrawable(OwnLocationShape((sizeFactor * zoomLevel * symbolSize).toFloat()))) }

        //Redraw when the OwnLocation is refreshed
        mapView.invalidate()
    }

    override fun createItem(i: Int): OwnLocationOverlayItem {
        return item!!
    }

    override fun size(): Int {
        return if (item == null) 0 else 1
    }

    fun enableOwnLocation() {
        setEnabled(true)
        refresh()
    }

    fun disableOwnLocation() {
        item = null
        setEnabled(false)
        refresh()
    }

    override fun onSnapToItem(x: Int, y: Int, snapPoint: Point?, mapView: IMapView?): Boolean {
        Log.v(LOG_TAG, "OwnLocationOverlay.onSnapToItem($x, $y, $snapPoint, $mapView)")
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        if (key == PreferenceKey.SHOW_LOCATION) {
            val showLocation = sharedPreferences.get(key, false)

            if (showLocation) {
                enableOwnLocation()
            } else {
                disableOwnLocation()
            }
        } else if (key == PreferenceKey.OWN_LOCATION_SIZE) {
            symbolSize = sharedPreferences.getAndConvert(PreferenceKey.OWN_LOCATION_SIZE, 100) {
                it.toFloat() / 100
            }
        }
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return false;
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        if (event != null) {
            val zoomLevel = event.zoomLevel
            if (zoomLevel != this.zoomLevel) {
                this.zoomLevel = zoomLevel
                refresh()
            }
        }
        return false;
    }

    override val name: String
        get() = layerOverlayComponent.name

    override var visible: Boolean
        get() = layerOverlayComponent.visible
        set(value) {
            layerOverlayComponent.visible = value
        }

    companion object {

        private val DEFAULT_DRAWABLE: Drawable

        init {
            val shape = OwnLocationShape(1f)
            DEFAULT_DRAWABLE = ShapeDrawable(shape)
        }
    }
}
