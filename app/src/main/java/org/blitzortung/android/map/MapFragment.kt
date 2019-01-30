// Created by plusminus on 00:23:14 - 03.10.2008
package org.blitzortung.android.map

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay

class MapFragment : Fragment(), OnSharedPreferenceChangeListener {

    private lateinit var mPrefs: SharedPreferences
    lateinit var mapView: OwnMapView
        private set
    private lateinit var mScaleBarOverlay: ScaleBarOverlay

    private val preferences = BOApplication.sharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mapView = OwnMapView(inflater.context)

        return mapView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = this.activity!!
        val dm = context.resources.displayMetrics

        preferences.registerOnSharedPreferenceChangeListener(this)

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        mScaleBarOverlay = ScaleBarOverlay(mapView)
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 20)
        mScaleBarOverlay.setCentred(true)
        mScaleBarOverlay.setAlignBottom(true)
        val centered = mScaleBarOverlay.javaClass.getDeclaredField("centred")
        centered.isAccessible = true
        centered.setBoolean(mScaleBarOverlay, true)
        mapView.overlays.add(this.mScaleBarOverlay)

        //built in zoom controls
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

        //needed for pinch zooms
        mapView.setMultiTouchControls(true)

        //scales tiles to the current screen's DPI, helps with readability of labels
        mapView.isTilesScaledToDpi = true

        //the rest of this is restoring the last map location the user looked at
        val zoomLevel = mPrefs.getFloat(PREFS_ZOOM_LEVEL_DOUBLE, mPrefs.getInt(PREFS_ZOOM_LEVEL, 0).toFloat())
        mapView.controller.setZoom(zoomLevel.toDouble())
        mapView.setMapOrientation(0f, false)
        val latitudeString = mPrefs.getString(PREFS_LATITUDE_STRING, null)
        val longitudeString = mPrefs.getString(PREFS_LONGITUDE_STRING, null)
        if (latitudeString == null || longitudeString == null) { // case handled for historical reasons only
            val scrollX = mPrefs.getInt(PREFS_SCROLL_X, 0)
            val scrollY = mPrefs.getInt(PREFS_SCROLL_Y, 0)
            mapView.scrollTo(scrollX, scrollY)
        } else {
            val latitude = latitudeString.toDouble()
            val longitude = longitudeString.toDouble()
            mapView.setExpectedCenter(GeoPoint(latitude, longitude))
        }

        setHasOptionsMenu(true)
        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE)
    }

    fun updateForgroundColor(fgcolor: Int) {
        mScaleBarOverlay.barPaint = mScaleBarOverlay.barPaint.apply { color = fgcolor }
        mScaleBarOverlay.textPaint = mScaleBarOverlay.textPaint.apply { color = fgcolor }
        mapView.postInvalidate()
    }

    override fun onPause() {
        //save the current location
        val edit = mPrefs.edit()
        edit.putString(PREFS_TILE_SOURCE, mapView.tileProvider.tileSource.name())
        edit.putString(PREFS_LATITUDE_STRING, mapView.mapCenter.latitude.toString())
        edit.putString(PREFS_LONGITUDE_STRING, mapView.mapCenter.longitude.toString())
        edit.putFloat(PREFS_ZOOM_LEVEL_DOUBLE, mapView.zoomLevelDouble.toFloat())
        edit.apply()

        mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }

    override fun onResume() {
        super.onResume()

        mapView.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        // Put overlay items first
        mapView.overlayManager.onCreateOptionsMenu(menu, MENU_LAST_ID, mapView)

        // Put "About" menu item last
        menu!!.add(0, MENU_ABOUT, Menu.CATEGORY_SECONDARY,
                R.string.copyright).setIcon(
                R.drawable.ic_menu_compass)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(pMenu: Menu?) {
        mapView.overlayManager.onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mapView)
        super.onPrepareOptionsMenu(pMenu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mapView.overlayManager.onOptionsItemSelected(item, MENU_LAST_ID, mapView)) {
            return true
        }

        when (item!!.itemId) {
            MENU_ABOUT -> {
                val builder = AlertDialog.Builder(activity)
                        .setTitle(R.string.app_name).setMessage(R.string.about_message)
                        .setIcon(R.drawable.icon)
                        .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, whichButton ->
                            //
                        }
                        )
                builder.create().show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun calculateTargetZoomLevel(widthInMeters: Float): Double {
        val equatorLength = 40075004.0 // in meters
        val widthInPixels = Math.min(mapView.height, mapView.width).toDouble()
        var metersPerPixel = equatorLength / 256
        var zoomLevel = 0.0
        while ((metersPerPixel * widthInPixels) > widthInMeters) {
            metersPerPixel /= 2.7
            ++zoomLevel
        }
        return zoomLevel - 1.0
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.MAP_TYPE -> {
                val mapTypeString = sharedPreferences.get(key, "SATELLITE")
                mapView.setTileSource(if (mapTypeString == "SATELLITE") TileSourceFactory.USGS_SAT else TileSourceFactory.MAPNIK)
            }
        }
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================

        private val MENU_SAMPLES = Menu.FIRST + 1
        private val MENU_ABOUT = MENU_SAMPLES + 1

        private val MENU_LAST_ID = MENU_ABOUT + 1 // Always set to last unused id

        const val PREFS_NAME = "org.andnav.osm.prefs"
        const val PREFS_TILE_SOURCE = "tilesource"
        const val PREFS_SCROLL_X = "scrollX"
        const val PREFS_SCROLL_Y = "scrollY"
        const val PREFS_LATITUDE_STRING = "latitudeString"
        const val PREFS_LONGITUDE_STRING = "longitudeString"
        const val PREFS_ZOOM_LEVEL = "zoomLevel"
        const val PREFS_ZOOM_LEVEL_DOUBLE = "zoomLevelDouble"
    }
}
