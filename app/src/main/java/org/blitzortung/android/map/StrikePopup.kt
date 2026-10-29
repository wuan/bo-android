package org.blitzortung.android.map

import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.blitzortung.android.app.R
import org.blitzortung.android.map.overlay.GridShape
import org.blitzortung.android.map.overlay.StrikeOverlay
import org.blitzortung.android.map.overlay.StrikeShape
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView

fun createStrikePopUp(
    popUp: View,
    strikeOverlay: StrikeOverlay,
): View {
    var result = DateFormat.format("kk:mm:ss", strikeOverlay.timestamp) as String

    if (strikeOverlay.shape is GridShape) {
        result += ", #%d".format(strikeOverlay.multiplicity)
    } else if (strikeOverlay.shape is StrikeShape) {
        result += " (%.4f %.4f)".format(strikeOverlay.center.longitude, strikeOverlay.center.latitude)
    }

    with(popUp.findViewById<TextView>(R.id.popup_text)!!) {
        setBackgroundColor(-2013265920)
        setPadding(5, 5, 5, 5)
        text = result
    }

    return popUp
}

/**
 * Strike tap handler for the phone map: shows the strike details in the
 * [OwnMapView] popup. Returns whether the tap was handled.
 */
fun showStrikePopup(
    mapView: MapView,
    strikeOverlay: StrikeOverlay,
    point: IGeoPoint,
): Boolean {
    val ownMapView = mapView as? OwnMapView ?: return false

    val popup = ownMapView.popup
    ownMapView.removeView(popup)

    val newPopup = createStrikePopUp(popup, strikeOverlay)
    val mapParams =
        MapView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            point,
            0,
            0,
            MapView.LayoutParams.BOTTOM_CENTER,
        )
    ownMapView.addView(newPopup, mapParams)

    return true
}
