package org.blitzortung.android.map

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlin.math.min
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.helper.ViewHelper
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
    private lateinit var mCopyrightOverlay: CopyrightOverlay
    private lateinit var mScaleBarOverlay: ScaleBarOverlay

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mapView = OwnMapView(inflater.context)

        mapView.tileProvider.tileCache.apply {
            protectedTileComputers.clear()
            setAutoEnsureCapacity(false)
        }

        return mapView
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = this.requireContext()
        val dm = context.resources.displayMetrics

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val bottomOffset = ViewHelper.pxFromDp(context, 64f + 2f).toInt()

        mCopyrightOverlay = CopyrightOverlay(context)
        mCopyrightOverlay.setTextSize(7)
        val copyrightOffset = dm.widthPixels / 2 - dm.widthPixels / 10
        mCopyrightOverlay.setOffset(copyrightOffset, bottomOffset)
        mapView.overlays.add(mCopyrightOverlay)
        mScaleBarOverlay = ScaleBarOverlay(mapView)
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, bottomOffset + ViewHelper.pxFromDp(context, 10f).toInt())
        mScaleBarOverlay.setCentred(true)
        mScaleBarOverlay.setAlignBottom(true)
        mScaleBarOverlay.setEnableAdjustLength(true)
        val centered = mScaleBarOverlay.javaClass.getDeclaredField("centred")
        centered.isAccessible = true
        centered.setBoolean(mScaleBarOverlay, true)
        mapView.overlays.add(this.mScaleBarOverlay)

        // disable built in zoom controls
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        // enable pinch zoom
        mapView.setMultiTouchControls(true)

        // scales tiles to the current screen's DPI, helps with readability of labels
        mapView.isTilesScaledToDpi = true

        // the rest of this is restoring the last map location the user looked at
        val zoomLevel = mPrefs.getFloat(PREFS_ZOOM_LEVEL_DOUBLE, 3.0f)
        mapView.controller.setZoom(zoomLevel.toDouble())
        mapView.setMapOrientation(0f, false)
        val latitudeString = mPrefs.getString(PREFS_LATITUDE_STRING, null)
        val longitudeString = mPrefs.getString(PREFS_LONGITUDE_STRING, null)
        if (latitudeString != null && longitudeString != null) { // case handled for historical reasons only
            val latitude = latitudeString.toDouble()
            val longitude = longitudeString.toDouble()
            mapView.setExpectedCenter(GeoPoint(latitude, longitude))
        }
        mapView.invalidate()

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_SCALE)
    }

    fun updateForgroundColor(fgcolor: Int) {
        mScaleBarOverlay.barPaint = mScaleBarOverlay.barPaint.apply { color = fgcolor }
        mScaleBarOverlay.textPaint = mScaleBarOverlay.textPaint.apply { color = fgcolor }
        mapView.postInvalidate()
    }

    override fun onPause() {
        // save the current location
        mPrefs.edit {
            putString(PREFS_TILE_SOURCE, mapView.tileProvider.tileSource.name())
            putString(PREFS_LATITUDE_STRING, mapView.mapCenter.latitude.toString())
            putString(PREFS_LONGITUDE_STRING, mapView.mapCenter.longitude.toString())
            putFloat(PREFS_ZOOM_LEVEL_DOUBLE, mapView.zoomLevelDouble.toFloat())
            commit()
        }

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

    val zoomLevel get() = mapView.zoomLevelDouble

    fun calculateTargetZoomLevel(widthInMeters: Float): Double {
        val equatorLength = 40075004.0 // in meters
        val widthInPixels = min(mapView.height, mapView.width).toDouble()
        var metersPerPixel = equatorLength / 256
        var zoomLevel = 0.0
        while ((metersPerPixel * widthInPixels) > widthInMeters) {
            metersPerPixel /= 2.7
            ++zoomLevel
        }
        return zoomLevel - 1.0
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: PreferenceKey,
    ) {
        when (key) {
            PreferenceKey.MAP_TYPE -> {
                val mapTypeString = sharedPreferences.get(key, "SATELLITE")
                mapView.setTileSource(
                    if (mapTypeString == "SATELLITE") TileSourceFactory.DEFAULT_TILE_SOURCE else TileSourceFactory.MAPNIK,
                )
            }

            PreferenceKey.MAP_SCALE -> {
                val scaleFactor = sharedPreferences.get(key, 100) / 100f
                Log.v(LOG_TAG, "MapFragment scale $scaleFactor")
                mapView.tilesScaleFactor = scaleFactor
            }

            else -> {}
        }
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================

        const val PREFS_NAME = "org.andnav.osm.prefs"
        const val PREFS_TILE_SOURCE = "tilesource"
        const val PREFS_LATITUDE_STRING = "latitudeString"
        const val PREFS_LONGITUDE_STRING = "longitudeString"
        const val PREFS_ZOOM_LEVEL_DOUBLE = "zoomLevelDouble"
    }
}
