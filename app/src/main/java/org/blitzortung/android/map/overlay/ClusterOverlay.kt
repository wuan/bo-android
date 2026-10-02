/*

   Copyright 2026 Andreas Würl

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
import android.graphics.Paint
import org.blitzortung.android.data.ClusterParameters
import org.blitzortung.android.data.beans.Cluster
import org.blitzortung.android.map.components.LayerOverlayComponent
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws all cluster polygon outlines, colored by cluster age, on top of the
 * strike/grid view. The current (newest) clusters are filled with a translucent
 * tint and labelled with their strike count; no popups or animation.
 */
class ClusterOverlay(private val colorHandler: StrikeColorHandler) : Overlay(), LayerOverlay {
    private val shapes = mutableListOf<ClusterShape>()

    private val layerOverlayComponent: LayerOverlayComponent = LayerOverlayComponent()

    var parameters: ClusterParameters? = null
    var referenceTime: Long = 0
    var intervalDuration: Int = 0

    override fun draw(
        canvas: Canvas?,
        mapView: MapView?,
        shadow: Boolean,
    ) {
        if (shadow || canvas == null || mapView == null) {
            return
        }

        val strokeWidth = (mapView.zoomLevelDouble * 0.5 + 0.5).toFloat()
        val paint = Paint()
        paint.isAntiAlias = true
        shapes.forEach { it.draw(canvas, mapView, paint, strokeWidth) }
    }

    fun setClusters(
        clusters: List<Cluster>,
        resultParameters: ClusterParameters,
        resultReferenceTime: Long,
        resultIntervalDuration: Int,
    ) {
        clear()

        parameters = resultParameters
        referenceTime = resultReferenceTime
        intervalDuration = resultIntervalDuration

        colorHandler.updateTarget()

        // A cluster response spans `intervalCount` intervals, so the age-to-color scale
        // must cover that whole range to match the legend instead of a single interval.
        val colorIntervalDuration = resultIntervalDuration * resultParameters.intervalCount
        val newestTimestamp = clusters.maxOfOrNull { it.timestamp }

        clusters.forEach { cluster ->
            val section = colorHandler.getColorSection(resultReferenceTime, cluster.timestamp, colorIntervalDuration)
            val shape = ClusterShape(cluster.shape)
            shape.update(
                color = colorHandler.getColor(section),
                textColor = colorHandler.textColor,
                strikeCount = cluster.strikeCount,
                current = cluster.timestamp == newestTimestamp,
            )
            shapes.add(shape)
        }
    }

    fun clear() {
        shapes.clear()
    }

    override var visible: Boolean
        get() = layerOverlayComponent.visible
        set(value) {
            layerOverlayComponent.visible = value
        }
}
