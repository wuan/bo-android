package org.blitzortung.android.app

import android.Manifest
import android.annotation.TargetApi
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.maps.GeoPoint
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.VersionComponent
import org.blitzortung.android.app.controller.ButtonColumnHandler
import org.blitzortung.android.app.controller.HistoryController
import org.blitzortung.android.app.view.AlertView
import org.blitzortung.android.app.view.HistogramView
import org.blitzortung.android.app.view.LegendView
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.components.StatusComponent
import org.blitzortung.android.data.provider.result.*
import org.blitzortung.android.dialogs.AlertDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.dialogs.InfoDialog
import org.blitzortung.android.dialogs.QuickSettingsDialog
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.OwnMapActivity
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.map.overlay.FadeOverlay
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.ParticipantsOverlay
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.TabletAwareView
import org.blitzortung.android.util.isAtLeast

class Main : OwnMapActivity(), OnSharedPreferenceChangeListener {
    private val androidIdsForExtendedFunctionality = setOf("44095eb4f9f1a6a6", "f2be4516e5843964")

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
    private var serviceConnection: ServiceConnection? = null
    private lateinit var legendView: LegendView
    private lateinit var histogramView: HistogramView
    private lateinit var alertView: AlertView

    private var currentResult: ResultEvent? = null

    val dataEventConsumer: (DataEvent) -> Unit = { event ->
        if (event is RequestStartedEvent) {
            buttonColumnHandler.lockButtonColumn(ButtonGroup.DATA_UPDATING)
            statusComponent.startProgress()
        } else if (event is ResultEvent) {

            if (event.failed) {
                statusComponent.indicateError(true)
            } else {
                statusComponent.indicateError(false)

                if (event.parameters!!.intervalDuration != appService!!.dataHandler().intervalDuration) {
                    reloadData()
                }

                currentResult = event

                Log.d(Main.LOG_TAG, "Main.onDataUpdate() " + event)

                val resultParameters = event.parameters

                clearDataIfRequested()

                strikesOverlay.parameters = resultParameters
                strikesOverlay.rasterParameters = event.rasterParameters
                strikesOverlay.referenceTime = event.referenceTime

                if (event.incrementalData) {
                    strikesOverlay.expireStrikes()
                } else {
                    strikesOverlay.clear()
                }
                if (event.strikes != null) {
                    strikesOverlay.addStrikes(event.strikes)
                }

                alertView.setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.parameters.intervalDuration)

                strikesOverlay.refresh()
                legendView.requestLayout()

                if (!event.containsRealtimeData()) {
                    setHistoricStatusString()
                }

                var stations = event.stations
                if (stations != null) {
                    participantsOverlay.setParticipants(stations)
                    participantsOverlay.refresh()
                }
            }

            statusComponent.stopProgress()

            buttonColumnHandler.unlockButtonColumn(ButtonGroup.DATA_UPDATING)

            mapView.invalidate()
            legendView.invalidate()
        } else if (event is ClearDataEvent) {
            clearData()
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
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        strikesOverlay = StrikesOverlay(this, StrikeColorHandler(preferences))
        participantsOverlay = ParticipantsOverlay(this, ParticipantColorHandler(preferences))

        mapView.addZoomListener { zoomLevel ->
            strikesOverlay.updateZoomLevel(zoomLevel)
            participantsOverlay.updateZoomLevel(zoomLevel)
        }

        fadeOverlay = FadeOverlay(strikesOverlay.getColorHandler())
        ownLocationOverlay = OwnLocationOverlay(baseContext, mapView)

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

        buttonColumnHandler.lockButtonColumn(ButtonGroup.DATA_UPDATING)
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
        val serviceIntent = Intent(this, AppService::class.java)

        startService(serviceIntent)

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                appService = (iBinder as AppService.DataServiceBinder).service
                Log.i(Main.LOG_TAG, "Main.ServiceConnection.onServiceConnected() " + appService!!)

                setupService()
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
            }
        }

        bindService(serviceIntent, serviceConnection, 0)
    }

    private fun setupService() {
        val appService = this.appService

        if (appService != null) {
            historyController.setAppService(appService)
            appService.addDataConsumer(historyController.dataConsumer)
            appService.addDataConsumer(dataEventConsumer)

            appService.addLocationConsumer(ownLocationOverlay.locationEventConsumer)
            appService.addDataConsumer(histogramView.dataConsumer)

            appService.addLocationConsumer(alertView.locationEventConsumer)
            appService.addAlertConsumer(alertView.alertEventConsumer)

            appService.addAlertConsumer(statusComponent.alertEventConsumer)
        }
    }

    private fun setupDebugModeButton() {
        val androidId = Settings.Secure.getString(baseContext.contentResolver, Settings.Secure.ANDROID_ID)
        Log.v(Main.LOG_TAG, "AndroidId: $androidId")
        if (isDebugBuild || (androidId != null && androidIdsForExtendedFunctionality.contains(androidId))) {
            val rasterToggle = findViewById(R.id.toggleExtendedMode) as ImageButton
            rasterToggle.isEnabled = true
            rasterToggle.visibility = View.VISIBLE

            rasterToggle.setOnClickListener { v ->
                buttonColumnHandler.lockButtonColumn(ButtonGroup.DATA_UPDATING)
                appService!!.dataHandler().toggleExtendedMode()
                reloadData()
            }
            buttonColumnHandler.addElement(rasterToggle, ButtonGroup.DATA_UPDATING)
        }
    }

    private fun setupCustomViews() {
        legendView = findViewById(R.id.legend_view) as LegendView
        legendView.strikesOverlay = strikesOverlay
        legendView.setAlpha(150)
        legendView.setOnClickListener { v -> openQuickSettingsDialog() }

        alertView = findViewById(R.id.alert_view) as AlertView
        alertView.setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.parameters.intervalDuration)
        alertView.setBackgroundColor(Color.TRANSPARENT)
        alertView.setAlpha(200)
        alertView.setOnClickListener { view ->
            val alertHandler = appService!!.alertHandler
            if (alertHandler.isAlertEnabled) {
                val currentLocation = alertHandler.currentLocation
                if (currentLocation != null) {
                    var radius = determineTargetZoomRadius(alertHandler)

                    val diameter = 1.5f * 2f * radius
                    animateToLocationAndVisibleSize(currentLocation.longitude, currentLocation.latitude, diameter)
                }

            }
        }

        histogramView = findViewById(R.id.histogram_view) as HistogramView
        histogramView.setStrikesOverlay(strikesOverlay)
        histogramView.setOnClickListener { view ->
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_info -> showDialog(R.id.info_dialog)

            R.id.menu_alarms -> showDialog(R.id.alarm_dialog)

            R.id.menu_preferences -> startActivity(Intent(this, Preferences::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        setupService()

        Log.d(Main.LOG_TAG, "Main.onStart() service: " + appService)
    }

    override fun onRestart() {
        super.onRestart()

        Log.d(Main.LOG_TAG, "Main.onStart() service: " + appService)
    }

    override fun onResume() {
        super.onResume()

        Log.d(Main.LOG_TAG, "Main.onResume() service: " + appService)
    }

    override fun onPause() {
        super.onPause()
        Log.v(Main.LOG_TAG, "Main.onPause()")
    }

    override fun onStop() {
        super.onStop()

        val appService = appService
        if (appService != null) {
            Log.v(Main.LOG_TAG, "Main.onStop() remove listeners")

            historyController.setAppService(null)
            appService.removeDataConsumer(historyController.dataConsumer)
            appService.removeDataConsumer(dataEventConsumer)

            appService.removeLocationConsumer(ownLocationOverlay.locationEventConsumer)
            appService.removeDataConsumer(histogramView.dataConsumer)

            appService.removeLocationConsumer(alertView.locationEventConsumer)
            appService.removeAlertListener(alertView.alertEventConsumer)

            appService.removeAlertListener(statusComponent.alertEventConsumer)
        } else {
            Log.i(LOG_TAG, "Main.onStop()")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "Main: onDestroy() unbind service")

        unbindService(serviceConnection)
    }

    override fun isRouteDisplayed(): Boolean {
        return false
    }

    private fun reloadData() {
        appService!!.reloadData()
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

    override fun onCreateDialog(id: Int): Dialog? {
        return when (id) {
            R.id.info_dialog -> InfoDialog(this, versionComponent)

            R.id.alarm_dialog ->
                appService?.let { appService ->
                    AlertDialog(this, appService, AlertDialogColorHandler(PreferenceManager.getDefaultSharedPreferences(this)))
                }

            else -> throw RuntimeException("unhandled dialog with id $id")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.v(LOG_TAG, "Main.onRequestPermissionsResult() $requestCode - $permissions - $grantResults")
        if (requestCode == REQUEST_GPS) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.
            Log.i(LOG_TAG, "Received response for Camera permission request.")

            // Check if the only required permission has been granted
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Log.i(LOG_TAG, "CAMERA permission has now been granted. Showing preview.")
            } else {
                Log.i(LOG_TAG, "CAMERA permission was NOT granted.")
            }
            // END_INCLUDE(permission_result)

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        appService?.let {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            it.updateLocationHandler(preferences)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions(sharedPreferences: SharedPreferences) {

        val newProvider = LocationHandler.Provider.fromString(sharedPreferences.getString(PreferenceKey.LOCATION_MODE.toString(), LocationHandler.Provider.PASSIVE.type))
        if (newProvider === LocationHandler.Provider.PASSIVE || newProvider === LocationHandler.Provider.GPS) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Main.REQUEST_GPS)
            }
        }
        if (newProvider === LocationHandler.Provider.NETWORK) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Main.REQUEST_GPS)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, vararg keys: PreferenceKey) {
        keys.forEach { onSharedPreferenceChanged(sharedPreferences, it) }
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.MAP_TYPE -> {
                val mapTypeString = sharedPreferences.getString(key.toString(), "SATELLITE")
                mapView.isSatellite = mapTypeString == "SATELLITE"
                strikesOverlay.refresh()
                participantsOverlay.refresh()
            }

            PreferenceKey.SHOW_PARTICIPANTS -> {
                val showParticipants = sharedPreferences.getBoolean(key.toString(), true)
                participantsOverlay.enabled = showParticipants
                updateOverlays()
            }

            PreferenceKey.COLOR_SCHEME -> {
                strikesOverlay.refresh()
                participantsOverlay.refresh()
            }

            PreferenceKey.MAP_FADE -> {
                val alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.getInt(key.toString(), 40))
                fadeOverlay.setAlpha(alphaValue)
            }

            PreferenceKey.DO_NOT_SLEEP -> {
                val doNotSleep = sharedPreferences.getBoolean(key.toString(), false)
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
            val referenceTime = strikesOverlay.referenceTime + strikesOverlay.parameters.intervalOffset * 60 * 1000
            val timeString = DateFormat.format("@ kk:mm", referenceTime) as String
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

        if (isAtLeast(Build.VERSION_CODES.ICE_CREAM_SANDWICH) &&
                !config.hasPermanentMenuKey()) {
            val menuButton = findViewById(R.id.menu) as ImageButton
            menuButton.visibility = View.VISIBLE
            menuButton.setOnClickListener { v -> openOptionsMenu() }
            buttonColumnHandler.addElement(menuButton)
        }
    }

    private fun hideActionBar() {
        if (isAtLeast(Build.VERSION_CODES.HONEYCOMB)) {
            actionBar?.hide()
        }
    }

    companion object {

        val LOG_TAG = "BO_ANDROID"

        val REQUEST_GPS = 1


    }
}