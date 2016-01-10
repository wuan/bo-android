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

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import com.google.android.maps.ItemizedOverlay

import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Station.State
import org.blitzortung.android.map.OwnMapActivity
import org.blitzortung.android.map.components.LayerOverlayComponent
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler

import java.util.ArrayList
import java.util.EnumMap

class ParticipantsOverlay(mapActivity: OwnMapActivity, private val colorHandler: ParticipantColorHandler) : PopupOverlay<ParticipantOverlayItem>(mapActivity, ItemizedOverlay.boundCenter(ParticipantsOverlay.DefaultDrawable)), LayerOverlay {

    // VisibleForTesting
    protected val participants: ArrayList<ParticipantOverlayItem>
    private val shapes: EnumMap<State, ShapeDrawable>

    private val layerOverlayComponent: LayerOverlayComponent

    // VisibleForTesting
    private var zoomLevel: Int = 0

    init {

        shapes = EnumMap<State, ShapeDrawable>(State::class.java)
        shapes.put(State.ON, ShapeDrawable(ParticipantShape()))
        shapes.put(State.DELAYED, ShapeDrawable(ParticipantShape()))
        shapes.put(State.OFF, ShapeDrawable(ParticipantShape()))

        layerOverlayComponent = LayerOverlayComponent(mapActivity.resources.getString(R.string.participants_layer))

        participants = ArrayList<ParticipantOverlayItem>()
        populate()
    }

    override fun createItem(index: Int): ParticipantOverlayItem {
        return participants[index]
    }

    override fun size(): Int {
        return participants.size
    }

    override fun draw(canvas: Canvas?, mapView: com.google.android.maps.MapView?, shadow: Boolean) {
        if (!shadow) {
            super.draw(canvas, mapView, false)
        }
    }

    fun setParticipants(stations: List<Station>) {
        Log.v(Main.LOG_TAG, "ParticipantsOverlay.setStations() #%d".format(stations.size))
        updateShapes()

        participants.clear()
        for (station in stations) {
            val item = ParticipantOverlayItem(station)
            item.setMarker(shapes[item.participantState])
            participants.add(item)
        }
        Log.v(Main.LOG_TAG, "ParticipantsOverlay.setStations() set")
        lastFocusedIndex = -1

        populate()
        Log.v(Main.LOG_TAG, "ParticipantsOverlay.setStations() finished")
    }

    fun clear() {
        lastFocusedIndex = -1
        clearPopup()
        participants.clear()
        populate()
    }

    fun updateZoomLevel(zoomLevel: Int) {
        if (zoomLevel != this.zoomLevel) {
            this.zoomLevel = zoomLevel
            refresh()
        }
    }

    fun refresh() {
        updateShapes()

        for (item in participants) {
            item.setMarker(shapes[item.participantState])
        }
    }

    private fun updateShapes() {
        val shapeSize = Math.max(1, zoomLevel - 3).toFloat()
        colorHandler.updateTarget()

        val colors = colorHandler.colors
        updateShape(State.ON, shapeSize, colors[0])
        updateShape(State.DELAYED, shapeSize, colors[1])
        updateShape(State.OFF, shapeSize, colors[2])
    }

    private fun updateShape(state: State, shapeSize: Float, color: Int) {
        (shapes[state]?.shape as ParticipantShape).update(shapeSize, color)
    }

    override fun onTap(index: Int): Boolean {
        val item = participants[index]

        if (item.title != null) {
            var label = item.title
            if (item.participantState !== State.ON) {
                val lastDataTime = item.lastDataTime
                label += "\n" + buildTimeString(lastDataTime)
            }
            showPopup(item.point, label)
            return true
        }

        return false
    }

    private fun buildTimeString(lastDataTime: Long): String {
        val now = System.currentTimeMillis()
        var time = (now - lastDataTime).toFloat() / 1000.0f / 60.0f

        if (time < 120) {
            return "%.0f min".format(time)
        }

        time /= 60.0f

        if (time < 48) {
            return "%.1f h".format(time)
        }

        time /= 24.0f

        return "%.1f d".format(time)
    }

    override val name: String
        get() = layerOverlayComponent.name

    override var enabled: Boolean
        get() = layerOverlayComponent.enabled
        set(value) {
            layerOverlayComponent.enabled = value
        }

    override var visible: Boolean
        get() = layerOverlayComponent.visible
        set(value) {
            layerOverlayComponent.visible = value
        }

    companion object {
        private val DefaultDrawable: Drawable = ShapeDrawable(ParticipantShape())
    }
}