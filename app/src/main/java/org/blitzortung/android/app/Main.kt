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
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.text.format.DateFormat
import android.util.AndroidRuntimeException
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import kotlinx.android.synthetic.main.main.*
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
import org.blitzortung.android.map.MapFragment
import org.blitzortung.android.map.overlay.FadeOverlay
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.LogUtil
import org.blitzortung.android.util.TabletAwareView
import org.blitzortung.android.util.isAtLeast
import org.jetbrains.anko.intentFor
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class Main : FragmentActivity(), OnSharedPreferenceChangeListener {
    private lateinit var statusComponent: StatusComponent
    private lateinit var versionComponent: VersionComponent

    private lateinit var strikeColorHandler: StrikeColorHandler
    private lateinit var strikeListOverlay: StrikeListOverlay
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

    private lateinit var serviceConnection: ServiceConnection

    private var currentResult: ResultEvent? = null

    private val keepZoomOnGotoOwnLocation: Boolean
        inline get() = BOApplication.sharedPreferences.get(PreferenceKey.KEEP_ZOOM_GOTO_OWN_LOCATION, false)

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

                val initializeOverlay = strikeListOverlay.parameters != resultParameters
                with(strikeListOverlay) {
                    parameters = resultParameters
                    rasterParameters = event.rasterParameters
                    referenceTime = event.referenceTime
                }

                if (event.incrementalData && !initializeOverlay) {
                    strikeListOverlay.expireStrikes()
                } else {
                    strikeListOverlay.clear()
                }

                if (initializeOverlay && event.totalStrikes != null) {
                    strikeListOverlay.addStrikes(event.totalStrikes)
                } else if (event.strikes != null) {
                    strikeListOverlay.addStrikes(event.strikes)
                }

                alert_view.setColorHandler(strikeColorHandler, strikeListOverlay.parameters.intervalDuration)

                strikeListOverlay.refresh()
                mapFragment.mapView.invalidate()

                legend_view.requestLayout()

                if (!event.containsRealtimeData()) {
                    setHistoricStatusString()
                }
            }

            statusComponent.stopProgress()

            //mapView.invalidate()
            legend_view.invalidate()
        } else if (event is StatusEvent) {
            setStatusString(event.status)
        }
    }

    private lateinit var mapFragment: MapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
        } catch (e: NoClassDefFoundError) {
            Log.e(Main.LOG_TAG, e.toString())
            Toast.makeText(baseContext, "bad android version", Toast.LENGTH_LONG).show()
        }
        setContentView(R.layout.main)

        Configuration.getInstance().userAgentValue = packageName

        if (supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            mapFragment = MapFragment()
            supportFragmentManager.beginTransaction().add(R.id.map_view, mapFragment, MAP_FRAGMENT_TAG).commit()
        }

        Log.v(LOG_TAG, "Main.onCreate()")

        versionComponent = VersionComponent(this.applicationContext)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        preferences.registerOnSharedPreferenceChangeListener(this)

        strikeColorHandler = StrikeColorHandler(preferences)

        statusComponent = StatusComponent(this)

        hideActionBar()

        buttonColumnHandler = ButtonColumnHandler(if (TabletAwareView.isTablet(this)) 75f else 55f)
        configureMenuAccess()
        historyController = HistoryController(this, buttonColumnHandler)

        buttonColumnHandler.addAllElements(historyController.getButtons(), ButtonGroup.DATA_UPDATING)

        //setupDetailModeButton()

        buttonColumnHandler.updateButtonColumn()

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

    private fun setupCustomViews() {
        with(legend_view) {
            strikesOverlay = this@Main.strikeListOverlay
            setAlpha(150)
            setOnClickListener { openQuickSettingsDialog() }
        }

        with(alert_view) {
            setColorHandler(strikeColorHandler, strikeListOverlay.parameters.intervalDuration)
            setBackgroundColor(Color.TRANSPARENT)
            setAlpha(200)
            setOnClickListener {
                if (alertHandler.alertEnabled) {
                    val currentLocation = alertHandler.currentLocation
                    if (currentLocation != null) {
                        val diameter = if (!keepZoomOnGotoOwnLocation) {
                            var radius = determineTargetZoomRadius(alertHandler)

                            //Calculate the new diameter
                            1.5f * 2f * radius
                        } else {
                            //User doesn't want to zoom, so we do not provide a diameter
                            null
                        }

                        animateToLocationAndVisibleSize(currentLocation.longitude, currentLocation.latitude, diameter)
                    }
                }
            }
        }

        with(histogram_view) {
            setStrikesOverlay(strikeListOverlay)
            setOnClickListener { _ ->
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

    private fun animateToLocationAndVisibleSize(longitude: Double, latitude: Double, diameter: Float?) {
        Log.d(Main.LOG_TAG, "Main.animateAndZoomTo() %.4f, %.4f, %.0fkm".format(longitude, latitude, diameter))

        val mapView = mapFragment.mapView
        val controller = mapView.controller

        //If no diameter is provided, we keep the current zoomLevel
        val targetZoomLevel = if (diameter != null) {
            mapFragment.calculateTargetZoomLevel(diameter * 1000f) * 1.0
        } else {
            mapView.zoomLevelDouble
        }

        controller.zoomTo(targetZoomLevel)
        controller.animateTo(GeoPoint(latitude, longitude))
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
        Log.v(LOG_TAG, "Main.onStart()")

        mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment

        strikeListOverlay = StrikeListOverlay(mapFragment, strikeColorHandler)
        strikeListOverlay.isEnabled = true
        setHistoricStatusString()
        mapFragment.mapView.addMapListener(strikeListOverlay)


        fadeOverlay = FadeOverlay(strikeColorHandler)

        ownLocationOverlay = OwnLocationOverlay(this, mapFragment.mapView)
        ownLocationOverlay.isEnabled = true
        mapFragment.mapView.addMapListener(ownLocationOverlay)

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP)

        val overlays = mapFragment.mapView.overlays
        Log.v(LOG_TAG, "Main.onStart() # of overlays ${overlays.size}")

        overlays.addAll(listOf(fadeOverlay, strikeListOverlay, ownLocationOverlay))

        setupCustomViews()

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
        Log.v(LOG_TAG, "Main.onResume()")

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestLocationPermissions(preferences)
            requestStoragePermissions()
            requestWakeupPermissions(preferences, baseContext)
        }

        mapFragment.updateForgroundColor(strikeColorHandler.lineColor)

        enableDataUpdates()

        Log.d(Main.LOG_TAG, "Main.onResume() ${LogUtil.timestamp} service: " + appService)
    }

    override fun onPause() {
        super.onPause()
        Log.v(LOG_TAG, "Main.onPause()")

        disableDataUpdates()

        Log.v(Main.LOG_TAG, "Main.onPause() ${LogUtil.timestamp}")
    }

    override fun onStop() {
        super.onStop()
        Log.v(LOG_TAG, "Main.onStop()")

        mapFragment.mapView.overlays.removeAll(listOf(fadeOverlay, ownLocationOverlay, strikeListOverlay))
        mapFragment.mapView.removeMapListener(ownLocationOverlay)
        mapFragment.mapView.removeMapListener(strikeListOverlay)

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

        strikeListOverlay.clear()
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
                editor.apply()
            } else {
                Log.i(LOG_TAG, "$providerName permission was NOT granted.")
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        locationHandler.update(preferences)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestLocationPermissions(sharedPreferences: SharedPreferences) {
        val locationProviderName = sharedPreferences.get(PreferenceKey.LOCATION_MODE, LocationManager.PASSIVE_PROVIDER)
        val permission = when (locationProviderName) {
            LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER -> Manifest.permission.ACCESS_FINE_LOCATION
            LocationManager.NETWORK_PROVIDER -> Manifest.permission.ACCESS_COARSE_LOCATION
            else -> null
        }

        if (permission is String && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), LocationProviderRelation.byProviderName[locationProviderName]?.ordinal
                    ?: Int.MIN_VALUE)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestStoragePermissions() {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 50)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestWakeupPermissions(sharedPreferences: SharedPreferences, context: Context) {
        val backgroundPeriod = sharedPreferences.get(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0").toInt()
        Log.v(LOG_TAG, "requestWakeupPermissions() background period: $backgroundPeriod")

        if (backgroundPeriod > 0) {
            val pm = context.getSystemService(Context.POWER_SERVICE)
            if (pm is PowerManager) {
                val packageName = context.packageName
                Log.v(LOG_TAG, "requestWakeupPermissions() package name $packageName")
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val locationText = context.resources.getString(R.string.open_battery_optimiziation)

                    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                Toast.makeText(baseContext, R.string.background_query_toast, Toast.LENGTH_LONG).show()

                                Log.v(LOG_TAG, "requestWakeupPermissions() request ignore battery optimizations")
                                val intent = Intent()
                                intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                try {
                                    context.startActivity(intent)
                                } catch (e: AndroidRuntimeException) {
                                    Log.e(LOG_TAG, "requestWakeupPermissions() could not open battery optimization settings", e)
                                }
                            }

                            DialogInterface.BUTTON_NEGATIVE -> {
                                val preferences = BOApplication.sharedPreferences
                                preferences.edit().apply {
                                    putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), 0.toString())
                                    apply()
                                }
                            }
                        }
                    }

                    AlertDialog.Builder(this)
                            .setMessage(locationText)
                            .setPositiveButton(android.R.string.yes, dialogClickListener)
                            .setNegativeButton(android.R.string.no, dialogClickListener)
                            .show()
                }
            } else {
                Log.w(LOG_TAG, "requestWakeupPermissions() could not get PowerManager")
            }
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
            return true
        }
        return super.onKeyUp(keyCode, event)
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
            PreferenceKey.COLOR_SCHEME -> {
                strikeListOverlay.refresh()
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
        if (!strikeListOverlay.hasRealtimeData()) {
            val timeString = DateFormat.format("@ kk:mm", strikeListOverlay.referenceTime) as String
            setStatusString(timeString)
        }
    }

    protected fun setStatusString(runStatus: String) {
        val numberOfStrikes = strikeListOverlay.totalNumberOfStrikes
        var statusText = resources.getQuantityString(R.plurals.strike, numberOfStrikes, numberOfStrikes)
        statusText += "/"
        val intervalDuration = strikeListOverlay.parameters.intervalDuration
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
        val MAP_FRAGMENT_TAG = "org.blitzortung.MAP_FRAGMENT_TAG"
    }
}
