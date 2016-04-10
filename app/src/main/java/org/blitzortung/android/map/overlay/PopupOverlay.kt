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

        with(popUp.findViewById(R.id.popup_text) as TextView) {
            setBackgroundColor(-2013265920)
            setPadding(5, 5, 5, 5)
            setText(text)
        }

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
