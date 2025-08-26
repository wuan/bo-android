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

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.location.LocationManager.PASSIVE_PROVIDER
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.util.AndroidRuntimeException
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import dagger.android.AndroidInjection
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.app.components.ChangeLogComponent
import org.blitzortung.android.app.components.VersionComponent
import org.blitzortung.android.app.controller.ButtonColumnHandler
import org.blitzortung.android.app.controller.HistoryController
import org.blitzortung.android.app.databinding.MainBinding
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.components.StatusComponent
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.AUTO_GRID_SIZE_VALUE
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.SequenceValidator
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStartedEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.dialogs.QuickSettingsDialog
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.MapFragment
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.map.overlay.FadeOverlay
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.LogUtil
import org.blitzortung.android.util.TabletAwareView
import org.blitzortung.android.util.isAtLeast
import org.osmdroid.config.Configuration
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.util.StorageUtils
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import kotlin.math.roundToInt

class Main : FragmentActivity(), OnSharedPreferenceChangeListener {
    private var backgroundAlertEnabled: Boolean = false
    private lateinit var statusComponent: StatusComponent

    private lateinit var strikeColorHandler: StrikeColorHandler
    private lateinit var strikeListOverlay: StrikeListOverlay
    private lateinit var ownLocationOverlay: OwnLocationOverlay
    private lateinit var fadeOverlay: FadeOverlay

    private var clearData: Boolean = false

    private lateinit var buttonColumnHandler: ButtonColumnHandler<ImageButton, ButtonGroup>

    private lateinit var historyController: HistoryController

    @set:Inject
    internal lateinit var locationHandler: LocationHandler

    @set:Inject
    internal lateinit var alertHandler: AlertHandler

    @set:Inject
    internal lateinit var dataHandler: MainDataHandler

    @set:Inject
    internal lateinit var preferences: SharedPreferences

    @set:Inject
    internal lateinit var versionComponent: VersionComponent

    @set:Inject
    internal lateinit var sequenceValidator: SequenceValidator

    @set:Inject
    internal lateinit var buildVersion: BuildVersion

    @set:Inject
    internal lateinit var changeLogComponent: ChangeLogComponent

    private var currentResult: ResultEvent? = null

    private var justReturnedFromSettings = false

    private val keepZoomOnGotoOwnLocation: Boolean
        inline get() = preferences.get(PreferenceKey.KEEP_ZOOM_GOTO_OWN_LOCATION, false)

    private val dataEventConsumer: (DataEvent) -> Unit = { event ->
        when (event) {
            is RequestStartedEvent -> {
                Log.d(LOG_TAG, "Main.onDataUpdate() received request started event")
                statusComponent.startProgress()
            }

            is ResultEvent -> {

                statusComponent.indicateError(event.failed)
                if (!event.failed && sequenceValidator.isUpdate(event.sequenceNumber)) {
                        currentResult = event

                        Log.d(LOG_TAG, "Main.onDataUpdate() $event")

                        val resultParameters = event.parameters

                        clearDataIfRequested()

                        val initializeOverlay = strikeListOverlay.parameters != resultParameters
                        with(strikeListOverlay) {
                            parameters = resultParameters
                            gridParameters = event.gridParameters
                            referenceTime = event.referenceTime
                        }

                        if (event.updated >= 0 && !initializeOverlay) {
                            strikeListOverlay.expireStrikes()
                        } else {
                            strikeListOverlay.clear()
                        }

                        if (event.strikes != null) {
                            val strikes = if (event.updated > 0 && !initializeOverlay) {
                                val size = event.strikes.size
                                event.strikes.subList(size - event.updated, size)
                            } else {
                                event.strikes
                            }
                            strikeListOverlay.addStrikes(strikes)
                        }

                        binding.alertView.setColorHandler(
                            strikeColorHandler,
                            strikeListOverlay.parameters.intervalDuration
                        )

                        strikeListOverlay.refresh()
                        mapFragment.mapView.invalidate()

                        binding.legendView.requestLayout()
                        binding.timeSlider.update(event.parameters, event.history!!)

                        if (event.flags.mode == Mode.ANIMATION || !event.containsRealtimeData()) {
                            setHistoricStatusString()
                        }
                }

                statusComponent.stopProgress()

                binding.legendView.invalidate()
            }

            is StatusEvent -> {
                setStatusString(event.status)
            }
        }
    }

    private lateinit var mapFragment: MapFragment

    private lateinit var binding: MainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        binding = MainBinding.inflate(layoutInflater)

        Log.v(LOG_TAG, "Main.onCreate()")

        try {
            super.onCreate(savedInstanceState)
        } catch (e: NoClassDefFoundError) {
            Log.e(LOG_TAG, e.toString())
            Toast.makeText(baseContext, "bad android version", Toast.LENGTH_LONG).show()
        }

        initializeOsmDroid()

        setContentView(binding.root)

        if (supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            mapFragment = MapFragment()
            supportFragmentManager.beginTransaction().add(R.id.map_view, mapFragment, MAP_FRAGMENT_TAG).commit()
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        preferences.registerOnSharedPreferenceChangeListener(this)

        strikeColorHandler = StrikeColorHandler(preferences)

        statusComponent = StatusComponent(
            findViewById(R.id.warning),
            findViewById(R.id.status),
            findViewById(R.id.progress),
            findViewById(R.id.error_indicator),
            resources
        )

        buttonColumnHandler = ButtonColumnHandler(if (TabletAwareView.isTablet(this)) 75f else 55f)
        configureMenuAccess()
        historyController = HistoryController(binding, buttonColumnHandler, dataHandler)
        val historyButtons = historyController.getButtons()
        buttonColumnHandler.addAllElements(historyButtons, ButtonGroup.DATA_UPDATING)

        //setupDetailModeButton()

        buttonColumnHandler.updateButtonColumn()

        if (versionComponent.state == VersionComponent.State.FIRST_RUN) {
            openQuickSettingsDialog()
        }
        if (versionComponent.state == VersionComponent.State.FIRST_RUN_AFTER_UPDATE) {
            changeLogComponent.showChangeLogDialog(this)
        }

        binding.timeSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    val changed = dataHandler.setPosition(p1)
                    if (changed) {
                        if (dataHandler.isRealtime) {
                            dataHandler.restart()
                        } else {
                            Log.v(LOG_TAG, "TimeSlider call updateData()")
                            dataHandler.updateData()
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
    }

    private fun initializeOsmDroid() {
        val osmDroidConfig = Configuration.getInstance()
        osmDroidConfig.load(this, preferences)
        if (!StorageUtils.isWritable(osmDroidConfig.osmdroidBasePath)) {
            preferences.edit { remove("osmdroid.basePath") }
            osmDroidConfig.load(this, preferences)
            if (!StorageUtils.isWritable(osmDroidConfig.osmdroidTileCache)) {
                Toast.makeText(baseContext, R.string.osmdroid_storage_warning, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupDetailModeButton() {
        with(binding.toggleExtendedMode) {
            isEnabled = true
            visibility = View.VISIBLE

            setOnClickListener {
                dataHandler.toggleExtendedMode()
                dataHandler.updateData()
            }

            buttonColumnHandler.addElement(this, ButtonGroup.DATA_UPDATING)
        }
    }

    private val serviceIntent: Intent
        get() = Intent(this, AppService::class.java)

    private fun startService() {
        startService(serviceIntent)
    }

    private fun stopService() {
        stopService(serviceIntent)
    }

    private fun setupCustomViews() {
        with(binding.legendView) {
            strikesOverlay = this@Main.strikeListOverlay
            setAlpha(150)
            setOnClickListener { openQuickSettingsDialog() }
        }

        with(binding.alertView) {
            setColorHandler(strikeColorHandler, strikeListOverlay.parameters.intervalDuration)
            setBackgroundColor(Color.TRANSPARENT)
            setAlpha(200)
            setOnClickListener {
                val currentLocation = locationHandler.location
                if (currentLocation != null) {
                    val diameter = if (!keepZoomOnGotoOwnLocation) {
                        determineTargetZoomRadius(alertHandler)
                    } else {
                        null
                    }

                    animateToLocationAndVisibleSize(currentLocation.longitude, currentLocation.latitude, diameter)
                }
            }
            enableLongClickListener(dataHandler, alertHandler)
        }

        with(binding.histogramView) {
            mapFragment = this@Main.mapFragment
            setStrikesOverlay(strikeListOverlay)
            setOnClickListener {
                val currentResult = currentResult
                if (currentResult != null) {
                    val parameters = currentResult.parameters
                    val gridParameters = currentResult.gridParameters
                    if (!parameters.isGlobal && gridParameters != null) {
                        animateToLocationAndVisibleSize(
                            gridParameters.rectCenterLongitude,
                            gridParameters.rectCenterLatitude,
                            if (parameters.region == LOCAL_REGION) 1800f else 5000f
                        )
                    } else {
                        animateToLocationAndVisibleSize(-30.0, 0.0, 40000f)
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
                radius = (alertResult.closestStrikeDistance * 1.2f).coerceIn(50f, radius)
            }
        }
        return radius
    }

    private fun openQuickSettingsDialog() {
        val dialog = QuickSettingsDialog()
        dialog.show(supportFragmentManager, "QuickSettingsDialog")
    }

    private fun animateToLocationAndVisibleSize(longitude: Double, latitude: Double, diameter: Float?) {
        Log.d(LOG_TAG, "Main.animateAndZoomTo() %.4f, %.4f, %.0fkm".format(longitude, latitude, diameter))

        val mapView = mapFragment.mapView

        //If no diameter is provided, we keep the current zoomLevel
        val targetZoomLevel = if (diameter != null) {
            mapFragment.calculateTargetZoomLevel(diameter * 1000f) * 1.0
        } else {
            mapView.zoomLevelDouble
        }

        mapView.controller.animateTo(GeoPoint(latitude, longitude), targetZoomLevel, OwnMapView.DEFAULT_ZOOM_SPEED)
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "Main.onStart()")

        mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment

        strikeListOverlay = StrikeListOverlay(mapFragment, strikeColorHandler)
        strikeListOverlay.isEnabled = true
        setHistoricStatusString()
        mapFragment.mapView.addMapListener(strikeListOverlay)
        mapFragment.mapView.addMapListener(dataHandler)
        mapFragment.mapView.addMapListener(binding.regionView)

        fadeOverlay = FadeOverlay(strikeColorHandler)

        ownLocationOverlay = OwnLocationOverlay(this, mapFragment.mapView)
        ownLocationOverlay.isEnabled = true
        mapFragment.mapView.addMapListener(ownLocationOverlay)

        onSharedPreferenceChanged(
            preferences,
            PreferenceKey.MAP_TYPE,
            PreferenceKey.MAP_FADE,
            PreferenceKey.SHOW_LOCATION,
            PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT,
            PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT,
            PreferenceKey.DO_NOT_SLEEP,
            PreferenceKey.BACKGROUND_QUERY_PERIOD,
            PreferenceKey.GRID_SIZE,
        )

        val overlays = mapFragment.mapView.overlays

        overlays.addAll(listOf(fadeOverlay, strikeListOverlay, ownLocationOverlay))

        setupCustomViews()
    }

    override fun onRestart() {
        super.onRestart()

        Log.d(LOG_TAG, "Main.onRestart()")
    }

    override fun onResume() {
        super.onResume()

        stopService()

        Log.v(LOG_TAG, "Main.onResume() justReturnedFromSettings: $justReturnedFromSettings")

        val cameFromSettings = justReturnedFromSettings
        if (justReturnedFromSettings) {
            justReturnedFromSettings = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!cameFromSettings) {
                requestLocationPermissions(preferences)
                requestWakeupPermissions(baseContext)
                requestNotificationPermissions()
            }
        }

        mapFragment.updateForgroundColor(strikeColorHandler.lineColor)

        enableDataUpdates()

        if (locationHandler.backgroundMode) {
            locationHandler.shutdown()
            locationHandler.disableBackgroundMode()
        }

        historyController.onResume()

        locationHandler.start()

        if (mapFragment.mapView.isLaidOut) {
            startDataHandler(mapFragment.mapView)
        } else {
            mapFragment.mapView.addOnFirstLayoutListener { thisMapView, _, _, _, _ ->
                startDataHandler(thisMapView as OwnMapView)
            }
        }
    }

    private fun startDataHandler(mapView: OwnMapView) {
        dataHandler.updateGrid(mapView, dataHandler.autoGridSize)
        strikeListOverlay.onZoom(ZoomEvent(mapView, mapView.zoomLevelDouble))

        dataHandler.start()
    }

    private fun enableDataUpdates() {
        with(locationHandler) {
            requestUpdates(ownLocationOverlay.locationEventConsumer)
            requestUpdates(binding.alertView.locationEventConsumer)
            requestUpdates(dataHandler.locationEventConsumer)
        }

        with(alertHandler) {
            requestUpdates(binding.alertView.alertEventConsumer)
            requestUpdates(statusComponent.alertEventConsumer)
        }

        with(dataHandler) {
            requestUpdates(dataEventConsumer)
            requestUpdates(alertHandler.dataEventConsumer)
            requestUpdates(historyController.dataConsumer)
            requestUpdates(binding.histogramView.dataConsumer)
            requestUpdates(binding.regionView.dataConsumer)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.v(LOG_TAG, "Main.onPause()")

        disableDataUpdates()

        if (backgroundAlertEnabled) {
            startService()
            if (!locationHandler.backgroundMode) {
                locationHandler.shutdown()
                locationHandler.enableBackgroundMode()
                locationHandler.start()
            }
        } else {
            locationHandler.shutdown()
        }

        Log.v(LOG_TAG, "Main.onPause() ${LogUtil.timestamp}")

        dataHandler.stop()

        Configuration.getInstance().save(this, preferences)
    }

    private fun disableDataUpdates() {
        with(locationHandler) {
            removeUpdates(ownLocationOverlay.locationEventConsumer)
            removeUpdates(binding.alertView.locationEventConsumer)
            removeUpdates(dataHandler.locationEventConsumer)
        }

        with(alertHandler) {
            removeUpdates(binding.alertView.alertEventConsumer)
            removeUpdates(statusComponent.alertEventConsumer)
        }

        with(dataHandler) {
            removeUpdates(dataEventConsumer)
            requestUpdates(alertHandler.dataEventConsumer)
            removeUpdates(historyController.dataConsumer)
            removeUpdates(binding.histogramView.dataConsumer)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.v(LOG_TAG, "Main.onStop()")

        mapFragment.mapView.overlays.removeAll(setOf(fadeOverlay, ownLocationOverlay, strikeListOverlay))
        mapFragment.mapView.removeMapListener(binding.regionView)
        mapFragment.mapView.removeMapListener(ownLocationOverlay)
        mapFragment.mapView.removeMapListener(strikeListOverlay)
        mapFragment.mapView.removeMapListener(dataHandler)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "Main.onDestroy()")
    }

    private fun clearDataIfRequested() {
        if (clearData) {
            clearData()
        }
    }

    private fun clearData() {
        Log.v(LOG_TAG, "Main.clearData()")
        clearData = false

        strikeListOverlay.clear()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.v(
            LOG_TAG,
            "Main.onRequestPermissionsResult() $requestCode - ${permissions.joinToString()} - ${grantResults.joinToString { it.toString() }}"
        )
        val providerRelation = LocationProviderRelation.byOrdinal[requestCode]
        if (providerRelation != null) {
            val providerName = providerRelation.providerName
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "$providerName permission has now been granted.")
                preferences.edit {
                    put(PreferenceKey.LOCATION_MODE, providerName)
                }
                locationHandler.update(preferences)
            } else {
                Log.i(LOG_TAG, "$providerName permission was NOT granted.")
                locationHandler.shutdown()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(POST_NOTIFICATIONS, REQUEST_CODE_POST_NOTIFICATIONS, R.string.post_notifications_request)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestLocationPermissions(sharedPreferences: SharedPreferences) {
        val locationProviderName = sharedPreferences.get(PreferenceKey.LOCATION_MODE, PASSIVE_PROVIDER)
        val permission = when (locationProviderName) {
            PASSIVE_PROVIDER, GPS_PROVIDER -> ACCESS_FINE_LOCATION
            NETWORK_PROVIDER -> ACCESS_COARSE_LOCATION
            else -> null
        }

        if (permission != null) {
            val requiresBackgroundPermission = if (isAtLeast(Build.VERSION_CODES.Q)) {
                val backgroundLocationPermission = checkSelfPermission(ACCESS_BACKGROUND_LOCATION)
                val result = backgroundAlertEnabled && backgroundLocationPermission != PackageManager.PERMISSION_GRANTED
                "Main.requestLocationPermissions() requires background: $backgroundLocationPermission"
                result
            } else {
                false
            }

            val checkSelfPermission = checkSelfPermission(permission)
            val requiresPermission = checkSelfPermission != PackageManager.PERMISSION_GRANTED
            Log.v(
                LOG_TAG,
                "Main.requestLocationPermissions() self permission: $checkSelfPermission, required: $requiresPermission, background required: $requiresBackgroundPermission"
            )

            val requestCode = (LocationProviderRelation.byProviderName[locationProviderName]?.ordinal ?: Int.MAX_VALUE)

            if (requiresBackgroundPermission && isAtLeast(Build.VERSION_CODES.Q)) {
                Log.v(
                    LOG_TAG,
                    "Main.requestLocationPermissions() open background permission dialog"
                )
                val locationText = this.resources.getString(R.string.location_permission_background_disclosure)
                AlertDialog.Builder(this).setMessage(locationText).setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        requestPermission(
                            ACCESS_BACKGROUND_LOCATION,
                            requestCode,
                            R.string.location_permission_background_required
                        )
                    }.setNegativeButton(android.R.string.cancel) { _, _ ->
                        preferences.edit { put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0") }
                    }.show()
            }
            if (requiresPermission) {
                requestPermission(
                    permission, requestCode,
                    R.string.location_permission_required
                )
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission(permission: String, requestCode: Int, permissionRequiredStringId: Int) {
        val shouldShowPermissionRationale = shouldShowRequestPermissionRationale(permission)
        val permissionIsGranted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        Log.v(
            LOG_TAG,
            "Main.requestPermission() permission: $permission, isGranted: $permissionIsGranted, shouldShowRationale: ${!shouldShowPermissionRationale}"
        )

        if (!permissionIsGranted) {
            if (shouldShowPermissionRationale) {
                requestPermissionsAfterDialog(permissionRequiredStringId, permission, requestCode)
            } else {
                requestPermissions(arrayOf(permission), requestCode)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermissionsAfterDialog(
        dialogTextResource: Int,
        permission: String,
        requestCode: Int,
    ) {
        Log.v(
            LOG_TAG,
            "Main.requestPermissionsAfterDialog() permission: $permission, dialogResource: $dialogTextResource, requestCode: $requestCode"
        )

        val locationText = resources.getString(dialogTextResource)
        AlertDialog.Builder(this).setMessage(locationText).setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, count ->
                dialog.dismiss()
                justReturnedFromSettings = true
                requestPermissions(arrayOf(permission), requestCode)
            }.show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestWakeupPermissions(context: Context) {
        Log.v(LOG_TAG, "requestWakeupPermissions() background alerts: $backgroundAlertEnabled")

        if (backgroundAlertEnabled) {
            val pm = context.getSystemService(POWER_SERVICE)
            if (pm is PowerManager) {
                val packageName = context.packageName
                Log.v(LOG_TAG, "requestWakeupPermissions() package name $packageName")
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val locationText = context.resources.getString(R.string.open_battery_optimiziation)

                    val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                Log.v(LOG_TAG, "requestWakeupPermissions() request ignore battery optimizations")
                                val allowIgnoreBatteryOptimization =
                                    context.checkSelfPermission(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED
                                val intent = if (allowIgnoreBatteryOptimization) {
                                    justReturnedFromSettings = true
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        "package:$packageName".toUri()
                                    )
                                } else {
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                }

                                try {
                                    startActivity(intent)
                                } catch (e: AndroidRuntimeException) {
                                    Toast.makeText(baseContext, R.string.background_query_toast, Toast.LENGTH_LONG)
                                        .show()
                                    Log.e(
                                        LOG_TAG,
                                        "requestWakeupPermissions() could not open battery optimization settings",
                                        e
                                    )
                                }
                            }

                            DialogInterface.BUTTON_NEGATIVE -> {
                                preferences.edit().apply {
                                    putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), 0.toString())
                                    apply()
                                }
                            }
                        }
                    }

                    AlertDialog.Builder(this).setMessage(locationText)
                        .setPositiveButton(android.R.string.ok, dialogClickListener)
                        .setNegativeButton(android.R.string.cancel, dialogClickListener).show()
                }
            } else {
                Log.w(LOG_TAG, "requestWakeupPermissions() could not get PowerManager")
            }
        }
    }

    private enum class LocationProviderRelation(val providerName: String) {
        GPS(GPS_PROVIDER), PASSIVE(PASSIVE_PROVIDER), NETWORK(NETWORK_PROVIDER);

        companion object {
            val byProviderName: Map<String, LocationProviderRelation> =
                entries.groupBy { it.providerName }.mapValues { it.value.first() }
            val byOrdinal: Map<Int, LocationProviderRelation> =
                entries.groupBy { it.ordinal }.mapValues { it.value.first() }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Log.v(LOG_TAG, "Main.onKeyUp(KEYCODE_MENU)")
            showPopupMenu(binding.upperRow)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.COLOR_SCHEME -> {
                strikeListOverlay.refresh()
            }

            PreferenceKey.MAP_FADE -> {
                val alphaValue = (255.0f / 100.0f * sharedPreferences.get(key, 40)).roundToInt()
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

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                backgroundAlertEnabled = sharedPreferences.get(key, "0").toInt() > 0
                binding.backgroundAlerts.isVisible = backgroundAlertEnabled
            }

            PreferenceKey.GRID_SIZE -> {
                val gridSizeString = sharedPreferences.get(key, AUTO_GRID_SIZE_VALUE)
                val autoGridSize = gridSizeString == AUTO_GRID_SIZE_VALUE
                dataHandler.updateGrid(mapFragment.mapView, autoGridSize)
            }

            else -> {}
        }
    }

    private fun setHistoricStatusString() {
        val timeString = DateFormat.format("@ kk:mm", strikeListOverlay.referenceTime) as String
        setStatusString(timeString)
    }

    private fun setStatusString(runStatus: String) {
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

        if (isAtLeast(Build.VERSION_CODES.LOLLIPOP) || isAtLeast(Build.VERSION_CODES.ICE_CREAM_SANDWICH) && !config.hasPermanentMenuKey()) {
            binding.menu.visibility = View.VISIBLE
            binding.menu.setOnClickListener {
                showPopupMenu(binding.menu)
            }

            buttonColumnHandler.addElement(binding.menu)
        }
    }

    private fun showPopupMenu(anchor: View) {
        val popupMenu =
            MainPopupMenu(this, anchor, preferences, dataHandler, alertHandler, buildVersion, changeLogComponent)
        popupMenu.showPopupMenu()
    }

    companion object {
        const val LOG_TAG = "BO_ANDROID"
        const val MAP_FRAGMENT_TAG = "org.blitzortung.MAP_FRAGMENT_TAG"
        const val REQUEST_CODE_POST_NOTIFICATIONS = 101

    }
}
