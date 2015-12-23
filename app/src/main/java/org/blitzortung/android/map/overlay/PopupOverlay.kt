package org.blitzortung.android.map.overlay

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.maps.GeoPoint
import com.google.android.maps.ItemizedOverlay
import com.google.android.maps.MapView
import com.google.android.maps.OverlayItem
import org.blitzortung.android.app.R
import org.blitzortung.android.map.OwnMapActivity

abstract class PopupOverlay<Item : OverlayItem>(val activity: OwnMapActivity, defaultMarker: Drawable) : ItemizedOverlay<Item>(defaultMarker) {
    internal var popupShown: Boolean = false

    init {
        popupShown = false
    }

    protected fun showPopup(location: GeoPoint, text: String) {

        val map = activity.mapView
        val popUp = map.popup
        map.removeView(popUp)

        val statusText = popUp.findViewById(R.id.popup_text) as TextView
        statusText.setBackgroundColor(-2013265920)
        statusText.setPadding(5, 5, 5, 5)
        statusText.text = text

        val mapParams = MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                location, 0, 0, MapView.LayoutParams.BOTTOM_CENTER)

        map.addView(popUp, mapParams)

        popupShown = true
    }

    fun clearPopup(): Boolean {
        val map = activity.mapView
        val popUp = map.popup

        map.removeView(popUp)

        val popupShownStatus = popupShown
        popupShown = false
        return popupShownStatus
    }

    override fun onTap(arg0: GeoPoint?, arg1: MapView?): Boolean {
        val eventHandled = super.onTap(arg0, arg1)

        if (!eventHandled) {
            clearPopup()
        }

        return eventHandled
    }
}
