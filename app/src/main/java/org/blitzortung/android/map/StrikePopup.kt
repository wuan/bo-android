package org.blitzortung.android.map

import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import org.blitzortung.android.app.R
import org.blitzortung.android.map.overlay.GridShape
import org.blitzortung.android.map.overlay.StrikeOverlay
import org.blitzortung.android.map.overlay.StrikeShape


fun createStrikePopUp(popUp: View, strikeOverlay: StrikeOverlay): View {
    var result = DateFormat.format("kk:mm:ss", strikeOverlay.timestamp) as String

    if (strikeOverlay.shape is GridShape) {
        result += ", #%d".format(strikeOverlay.multiplicity)
    } else if (strikeOverlay.shape is StrikeShape) {
        result += " (%.4f %.4f)".format(strikeOverlay.center.longitude, strikeOverlay.center.latitude)
    }

    with(popUp.findViewById(R.id.popup_text) as TextView) {
        setBackgroundColor(-2013265920)
        setPadding(5, 5, 5, 5)
        text = result
    }

    return popUp
}