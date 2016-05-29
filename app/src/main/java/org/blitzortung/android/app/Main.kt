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

package org.blitzortung.android.app

import android.Manifest
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.maps.GeoPoint
import kotlinx.android.synthetic.main.map_overlay.*
import kotlinx.android.synthetic.main.overlay_top.*
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.VersionComponent
import org.blitzortung.android.app.controller.ButtonColumnHandler
import org.blitzortung.android.app.controller.HistoryController
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.components.StatusComponent
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStartedEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.dialogs.QuickSettingsDialog
import org.blitzortung.android.map.OwnMapActivity
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.map.overlay.FadeOverlay
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.ParticipantsOverlay
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.LogUtil
import org.blitzortung.android.util.TabletAwareView
import org.blitzortung.android.util.isAtLeast
import org.jetbrains.anko.intentFor

class Main : OwnMapActivity(), OnSharedPreferenceChangeListener {
    private val androidIdsForExtendedFunctionality = setOf("f0f71d2b06703e28")

    private lateinit var statusComponent: StatusComponent
    private lateinit var versionComponent: VersionComponent

    private lateinit var strikesOverlay: StrikesOverlay
    private lateinit var participantsOverlay: ParticipantsOverlay
    private lateinit var ownLocationOverlay: OwnLocationOverlay
    private lateinit var fadeOverlay: FadeOverlay

    private var clearData: Boolean = false
    private lateinit var buttonColumnHandler: ButtonColumnHandler<ImageButton, ButtonGroup>

    private lateinit var historyController: HistoryController
    private var appService: AppService? = null

    private val locationHandler = BOApplication.locationHandler
    private val alertHandler = BOApplication.alertHandler
    private val dataHandler = BOApplication.dataHandler

    private val preferences = BOApplication.sharedPreferences

    private var serviceConnection: ServiceConnection? = null

    private var currentResult: ResultEvent? = null

    val dataEventConsumer: (DataEvent) -> Unit = { event ->
        if (event is RequestStartedEvent) {
            Log.d(Main.LOG_TAG, "Main.onDataUpdate() received request started event")
            statusComponent.startProgress()
        } else if (event is ResultEvent) {

            statusComponent.indicateError(event.failed)
            if (!event.failed) {
                currentResult = event

                Log.d(Main.LOG_TAG, "Main.onDataUpdate() " + event)

                val resultParameters = event.parameters

                clearDataIfRequested()

                val initializeOverlay = strikesOverlay.parameters != resultParameters
                with(strikesOverlay) {
                    parameters = resultParameters
                    rasterParameters = event.rasterParameters
                    referenceTime = event.referenceTime
                }

                if (event.incrementalData && !initializeOverlay) {
                    strikesOverlay.expireStrikes()
                } else {
                    strikesOverlay.clear()
                }

                if (initializeOverlay && event.totalStrikes != null) {
                    strikesOverlay.addStrikes(event.totalStrikes)
                } else if (event.strikes != null) {
                    strikesOverlay.addStrikes(event.strikes)
                }

                alert_view.setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.parameters.intervalDuration)

                strikesOverlay.refresh()

                legend_view.requestLayout()

                if (!event.containsRealtimeData()) {
                    setHistoricStatusString()
                }

                event.stations?.run {
                    participantsOverlay.setParticipants(this)
                    participantsOverlay.refresh()
                }
            }

            statusComponent.stopProgress()

            mapView.invalidate()
            legend_view.invalidate()
        } else if (event is StatusEvent) {
            setStatusString(event.status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
        } catch (e: NoClassDefFoundError) {
            Log.e(Main.LOG_TAG, e.toString())
            Toast.makeText(baseContext, "bad android version", Toast.LENGTH_LONG).show()
        }

        Log.v(LOG_TAG, "Main.onCreate()")

        versionComponent = VersionComponent(this.applicationContext)

        setContentView(if (isDebugBuild) R.layout.main_debug else R.layout.main)

        val mapView = findViewById(R.id.mapview) as OwnMapView
        mapView.setBuiltInZoomControls(true)
        this.mapView = mapView

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        preferences.registerOnSharedPreferenceChangeListener(this)

        strikesOverlay = StrikesOverlay(this, StrikeColorHandler(preferences))
        participantsOverlay = ParticipantsOverlay(this, ParticipantColorHandler(preferences))

        mapView.addZoomListener { zoomLevel ->
            strikesOverlay.updateZoomLevel(zoomLevel)
            participantsOverlay.updateZoomLevel(zoomLevel)
        }

        fadeOverlay = FadeOverlay(strikesOverlay.getColorHandler())
        ownLocationOverlay = OwnLocationOverlay(this, mapView)

        addOverlay(fadeOverlay)
        addOverlay(strikesOverlay)
        addOverlay(participantsOverlay)
        addOverlay(ownLocationOverlay)
        updateOverlays()

        statusComponent = StatusComponent(this)
        setHistoricStatusString()

        hideActionBar()

        buttonColumnHandler = ButtonColumnHandler<ImageButton, ButtonGroup>(if (TabletAwareView.isTablet(this)) 75f else 55f)
        configureMenuAccess()
        historyController = HistoryController(this, buttonColumnHandler)

        buttonColumnHandler.addAllElements(historyController.getButtons(), ButtonGroup.DATA_UPDATING)

        setupDebugModeButton()

        buttonColumnHandler.updateButtonColumn()

        setupCustomViews()

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP, PreferenceKey.SHOW_PARTICIPANTS)

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(preferences)
        }

        createAndBindToDataService()

        if (versionComponent.state == VersionComponent.State.FIRST_RUN) {
            openQuickSettingsDialog()
        }
    }

    private fun createAndBindToDataService() {
        val serviceIntent = intentFor<AppService>()

        startService(serviceIntent)

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                appService = (iBinder as AppService.DataServiceBinder).service
                Log.i(Main.LOG_TAG, "Main.ServiceConnection.onServiceConnected() " + appService)

                enableDataUpdates()
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
            }
        }

        bindService(serviceIntent, serviceConnection, 0)
    }

    private fun setupDebugModeButton() {
        val androidId = Settings.Secure.getString(baseContext.contentResolver, Settings.Secure.ANDROID_ID)
        Log.v(Main.LOG_TAG, "AndroidId: $androidId")
        if ((androidId != null && androidIdsForExtendedFunctionality.contains(androidId))) {
            with(toggleExtendedMode) {
                isEnabled = true
                visibility = View.VISIBLE

                setOnClickListener { v ->
                    BOApplication.dataHandler.toggleExtendedMode()
                    reloadData()
                }

                buttonColumnHandler.addElement(this, ButtonGroup.DATA_UPDATING)
            }
        }
    }

    private fun setupCustomViews() {
        with(legend_view) {
            strikesOverlay = this@Main.strikesOverlay
            setAlpha(150)
            setOnClickListener { v -> openQuickSettingsDialog() }
        }

        with(alert_view) {
            setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.parameters.intervalDuration)
            setBackgroundColor(Color.TRANSPARENT)
            setAlpha(200)
            setOnClickListener { view ->
                if (alertHandler.alertEnabled) {
                    val currentLocation = alertHandler.currentLocation
                    if (currentLocation != null) {
                        var radius = determineTargetZoomRadius(alertHandler)

                        val diameter = 1.5f * 2f * radius
                        animateToLocationAndVisibleSize(currentLocation.longitude, currentLocation.latitude, diameter)
                    }
                }
            }
        }

        with(histogram_view) {
            setStrikesOverlay(strikesOverlay)
            setOnClickListener { view ->
                val currentResult = currentResult
                if (currentResult != null) {
                    val rasterParameters = currentResult.rasterParameters
                    if (rasterParameters != null) {
                        animateToLocationAndVisibleSize(rasterParameters.rectCenterLongitude.toDouble(), rasterParameters.rectCenterLatitude.toDouble(), 5000f)
                    } else {
                        animateToLocationAndVisibleSize(0.0, 0.0, 20000f)
                    }
                }
            }
        }
    }

    private fun determineTargetZoomRadius(alertHandler: AlertHandler): Float {
        var radius = alertHandler.maxDistance

        val alertEvent = alertHandler.alertEvent
        if (alertEvent is AlertResultEvent) {
            val alertResult = alertEvent.alertResult
            if (alertResult != null) {
                radius = Math.max(Math.min(alertResult.closestStrikeDistance * 1.2f, radius), 50f)
            }
        }
        return radius
    }

    private fun openQuickSettingsDialog() {
        if (isAtLeast(Build.VERSION_CODES.HONEYCOMB)) {
            val dialog = QuickSettingsDialog()
            dialog.show(fragmentManager, "QuickSettingsDialog")
        }
    }

    private fun animateToLocationAndVisibleSize(longitude: Double, latitude: Double, diameter: Float) {
        Log.d(Main.LOG_TAG, "Main.animateAndZoomTo() %.4f, %.4f, %.0fkm".format(longitude, latitude, diameter))

        val mapView = mapView
        val controller = mapView.controller

        val startZoomLevel = mapView.zoomLevel
        val targetZoomLevel = mapView.calculateTargetZoomLevel(diameter * 1000f)

        controller.animateTo(GeoPoint((latitude * 1e6).toInt(), (longitude * 1e6).toInt()), {
            if (startZoomLevel != targetZoomLevel) {
                val zoomOut = targetZoomLevel - startZoomLevel < 0
                val zoomCount = Math.abs(targetZoomLevel - startZoomLevel)
                val handler = Handler()
                var delay: Long = 0
                for (i in 0..zoomCount - 1) {
                    handler.postDelayed({
                        if (zoomOut) {
                            controller.zoomOut()
                        } else {
                            controller.zoomIn()
                        }
                    }, delay)
                    delay += 150
                }
            }
        })
    }

    val isDebugBuild: Boolean
        get() {
            var dbg = false
            try {
                val pm = packageManager
                val pi = pm.getPackageInfo(packageName, 0)

                dbg = ((pi.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            } catch (ignored: Exception) {
            }

            return dbg
        }

    override fun onStart() {
        super.onStart()

        Log.d(Main.LOG_TAG, "Main.onStart() service: " + appService)
    }

    private fun enableDataUpdates() {
        appService?.let { appService ->
            with(locationHandler) {
                requestUpdates(ownLocationOverlay.locationEventConsumer)
                requestUpdates(alert_view.locationEventConsumer)
            }

            with(alertHandler) {
                requestUpdates(alert_view.alertEventConsumer)
                requestUpdates(statusComponent.alertEventConsumer)
            }

            with(dataHandler) {
                requestUpdates(dataEventConsumer)
                requestUpdates(historyController.dataConsumer)
                requestUpdates(histogram_view.dataConsumer)
            }

            historyController.setAppService(appService)
        }
    }

    override fun onRestart() {
        super.onRestart()

        Log.d(Main.LOG_TAG, "Main.onRestart() ${LogUtil.timestamp} service: " + appService)
    }

    override fun onResume() {
        super.onResume()

        enableDataUpdates()

        Log.d(Main.LOG_TAG, "Main.onResume() ${LogUtil.timestamp} service: " + appService)
    }

    override fun onPause() {
        super.onPause()

        disableDataUpdates()

        Log.v(Main.LOG_TAG, "Main.onPause() ${LogUtil.timestamp}")
    }

    override fun onStop() {
        super.onStop()

        Log.v(Main.LOG_TAG, "Main.onStop() ${LogUtil.timestamp}")
    }

    private fun disableDataUpdates() {
        with(locationHandler) {
            removeUpdates(ownLocationOverlay.locationEventConsumer)
            removeUpdates(alert_view.locationEventConsumer)
        }

        with(alertHandler) {
            removeUpdates(alert_view.alertEventConsumer)
            removeUpdates(statusComponent.alertEventConsumer)
        }

        with(dataHandler) {
            removeUpdates(dataEventConsumer)
            removeUpdates(historyController.dataConsumer)
            removeUpdates(histogram_view.dataConsumer)
        }

        appService?.apply {
            Log.v(LOG_TAG, "Main.stopDataUpdates() remove listeners")

            historyController.setAppService(null)
        } ?: Log.i(LOG_TAG, "Main.stopDataUpdates()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "Main: onDestroy() ${LogUtil.timestamp} unbind service")

        unbindService(serviceConnection)
    }

    override fun isRouteDisplayed(): Boolean {
        return false
    }

    private fun reloadData() {
        appService?.run { this.reloadData() }
    }

    private fun clearDataIfRequested() {
        if (clearData) {
            clearData()
        }
    }

    private fun clearData() {
        Log.v(Main.LOG_TAG, "Main.clearData()")
        clearData = false

        strikesOverlay.clear()
        participantsOverlay.clear()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.v(LOG_TAG, "Main.onRequestPermissionsResult() $requestCode - $permissions - $grantResults")
        val providerRelation = LocationProviderRelation.byOrdinal[requestCode]
        if (providerRelation != null) {
            val providerName = providerRelation.providerName
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "$providerName permission has now been granted.")
                val editor = preferences.edit()
                editor.put(PreferenceKey.LOCATION_MODE, providerName)
                editor.commit()
            } else {
                Log.i(LOG_TAG, "$providerName permission was NOT granted.")
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        locationHandler.update(preferences)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions(sharedPreferences: SharedPreferences) {
        val locationProviderName = sharedPreferences.get(PreferenceKey.LOCATION_MODE, LocationManager.PASSIVE_PROVIDER)
        val permission = when (locationProviderName) {
            LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER -> Manifest.permission.ACCESS_FINE_LOCATION
            LocationManager.NETWORK_PROVIDER -> Manifest.permission.ACCESS_COARSE_LOCATION
            else -> null
        }

        if (permission is String && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), LocationProviderRelation.byProviderName[locationProviderName]?.ordinal ?: Int.MIN_VALUE)
        }
    }

    private enum class LocationProviderRelation(val providerName: String) {
        GPS(LocationManager.GPS_PROVIDER), PASSIVE(LocationManager.PASSIVE_PROVIDER), NETWORK(LocationManager.NETWORK_PROVIDER);

        companion object {
            val byProviderName: Map<String, LocationProviderRelation> = LocationProviderRelation.values().groupBy { it.providerName }.mapValues { it.value.first() }
            val byOrdinal: Map<Int, LocationProviderRelation> = LocationProviderRelation.values().groupBy { it.ordinal }.mapValues { it.value.first() }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Log.v(Main.LOG_TAG, "Main.onKeyUp(KEYCODE_MENU)")
            showPopupMenu(upper_row)
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, vararg keys: PreferenceKey) {
        keys.forEach { onSharedPreferenceChanged(sharedPreferences, it) }
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.MAP_TYPE -> {
                val mapTypeString = sharedPreferences.get(key, "SATELLITE")
                mapView.isSatellite = mapTypeString == "SATELLITE"
                strikesOverlay.refresh()
                participantsOverlay.refresh()
            }

            PreferenceKey.SHOW_PARTICIPANTS -> {
                val showParticipants = sharedPreferences.get(key, true)
                participantsOverlay.enabled = showParticipants
                updateOverlays()
            }

            PreferenceKey.COLOR_SCHEME -> {
                strikesOverlay.refresh()
                participantsOverlay.refresh()
            }

            PreferenceKey.MAP_FADE -> {
                val alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.get(key, 40))
                fadeOverlay.setAlpha(alphaValue)
            }

            PreferenceKey.DO_NOT_SLEEP -> {
                val doNotSleep = sharedPreferences.get(key, false)
                val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

                if (doNotSleep) {
                    window.addFlags(flag)
                } else {
                    window.clearFlags(flag)
                }
            }
        }
    }

    protected fun setHistoricStatusString() {
        if (!strikesOverlay.hasRealtimeData()) {
            val timeString = DateFormat.format("@ kk:mm", strikesOverlay.referenceTime) as String
            setStatusString(timeString)
        }
    }

    protected fun setStatusString(runStatus: String) {
        val numberOfStrikes = strikesOverlay.totalNumberOfStrikes
        var statusText = resources.getQuantityString(R.plurals.strike, numberOfStrikes, numberOfStrikes)
        statusText += "/"
        val intervalDuration = strikesOverlay.parameters.intervalDuration
        statusText += resources.getQuantityString(R.plurals.minute, intervalDuration, intervalDuration)
        statusText += " " + runStatus

        statusComponent.setText(statusText)
    }

    private fun configureMenuAccess() {
        val config = ViewConfiguration.get(this)

        if (isAtLeast(Build.VERSION_CODES.LOLLIPOP) ||
                isAtLeast(Build.VERSION_CODES.ICE_CREAM_SANDWICH) &&
                        !config.hasPermanentMenuKey()) {
            menu.visibility = View.VISIBLE
            menu.setOnClickListener {
                showPopupMenu(menu)
            }

            buttonColumnHandler.addElement(menu)
        }
    }

    private fun showPopupMenu(anchor: View) {
        val popupMenu = MainPopupMenu(this, anchor)
        popupMenu.showPopupMenu()
    }

    private fun hideActionBar() {
        if (isAtLeast(Build.VERSION_CODES.HONEYCOMB)) {
            actionBar?.hide()
        }
    }

    companion object {
        val LOG_TAG = "BO_ANDROID"
    }
}
