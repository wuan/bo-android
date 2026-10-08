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

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import dagger.android.AndroidInjection
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.app.components.ChangeLogComponent
import org.blitzortung.android.app.components.VersionComponent
import org.blitzortung.android.app.controller.ButtonColumnHandler
import org.blitzortung.android.app.controller.HistoryController
import org.blitzortung.android.app.docoverlay.BalloonTailSide
import org.blitzortung.android.app.databinding.MainBinding
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.permission.requester.BackgroundLocationDisclosureRequester
import org.blitzortung.android.app.permission.requester.BackgroundLocationPermissionRequester
import org.blitzortung.android.app.permission.requester.LocationPermissionRequester
import org.blitzortung.android.app.permission.requester.NotificationPermissionRequester
import org.blitzortung.android.app.permission.requester.WakeupPermissionRequester
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.components.StatusComponent
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.viewmodel.MainViewModel
import org.blitzortung.android.data.AUTO_GRID_SIZE_VALUE
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.SequenceValidator
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.NoData
import org.blitzortung.android.data.provider.result.StatusUpdate
import org.blitzortung.android.dialogs.QuickSettingsDialog
import org.blitzortung.android.location.LocationEvent
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

class Main : FragmentActivity(), OnSharedPreferenceChangeListener {
    private var backgroundAlertEnabled: Boolean = false
    private lateinit var statusComponent: StatusComponent

    @set:Inject
    internal lateinit var strikeColorHandler: StrikeColorHandler

    private lateinit var strikeListOverlay: StrikeListOverlay
    private lateinit var ownLocationOverlay: OwnLocationOverlay
    private lateinit var fadeOverlay: FadeOverlay

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

    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MainViewModel by viewModels { viewModelFactory }

    private lateinit var permissionRequesters: Array<PermissionRequester>

    private var currentResult: DataReceived? = null

    private val keepZoomOnGotoOwnLocation: Boolean
        inline get() = preferences.get(PreferenceKey.KEEP_ZOOM_GOTO_OWN_LOCATION, false)

    private fun handleDataReceived(event: DataReceived) {
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

            event.strikes?.let { strikes ->
                val strikesToAdd =
                    if (event.updated > 0 && !initializeOverlay) {
                        strikes.subList(strikes.size - event.updated, strikes.size)
                    } else {
                        strikes
                    }
                strikeListOverlay.addStrikes(strikesToAdd)
            }

            binding.alertView.setColorHandler(
                strikeColorHandler,
                strikeListOverlay.parameters.intervalDuration,
            )

            strikeListOverlay.refresh()
            mapFragment.mapView.invalidate()

            binding.legendView.requestLayout()
            binding.timeSlider.update(event.parameters, event.history!!)

            if (event.flags.mode == Mode.ANIMATION || !event.containsRealtimeData()) {
                setHistoricStatusString()
            }
        }

        binding.legendView.invalidate()
    }

    private lateinit var mapFragment: MapFragment

    private lateinit var binding: MainBinding
    private lateinit var docOverlayController: DocOverlayController

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        binding = MainBinding.inflate(layoutInflater)

        Log.v(LOG_TAG, "Main.onCreate()")

        WindowCompat.setDecorFitsSystemWindows(window, false)

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

        statusComponent =
            StatusComponent(
                findViewById(R.id.warning),
                findViewById(R.id.status),
                findViewById(R.id.progress),
                findViewById(R.id.error_indicator),
                this,
            )

        buttonColumnHandler = ButtonColumnHandler(if (TabletAwareView.isTablet(this)) 75f else 55f)
        configureMenuAccess()
        historyController = HistoryController(binding, buttonColumnHandler, viewModel)
        val historyButtons = historyController.getButtons()
        buttonColumnHandler.addAllElements(historyButtons, ButtonGroup.DATA_UPDATING)

        observeViewModels()

        // setupDetailModeButton()

        buttonColumnHandler.updateButtonColumn()

        if (versionComponent.state == VersionComponent.State.FIRST_RUN) {
            openQuickSettingsDialog()
        }
        if (versionComponent.state == VersionComponent.State.FIRST_RUN_AFTER_UPDATE) {
            changeLogComponent.showChangeLogDialog(this)
        }

        binding.timeSlider.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    p0: SeekBar?,
                    p1: Int,
                    p2: Boolean,
                ) {
                    if (p2) {
                        val changed = viewModel.setPosition(p1)
                        if (changed) {
                            if (viewModel.isRealtime()) {
                                viewModel.restart()
                            } else {
                                Log.v(LOG_TAG, "TimeSlider call updateData()")
                                viewModel.updateData()
                            }
                        }
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            },
        )

        permissionRequesters =
            arrayOf(
                BackgroundLocationDisclosureRequester(this, preferences, ::ensurePermissions),
                LocationPermissionRequester(this, preferences),
                NotificationPermissionRequester(this, preferences),
                BackgroundLocationPermissionRequester(this, preferences),
                WakeupPermissionRequester(this, preferences),
            )

        docOverlayController = DocOverlayController(binding.root, layoutInflater, preferences, ::createDocTargets)
        docOverlayController.showIfNotShownBefore()
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
                viewModel.toggleExtendedMode()
                viewModel.updateData()
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
            setOnClickListener {
                val currentLocation = locationHandler.location
                if (currentLocation != null) {
                    val diameter =
                        if (!keepZoomOnGotoOwnLocation) {
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
                            if (parameters.region == LOCAL_REGION) 1800f else 5000f,
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

        val warning = alertHandler.alertEvent
        if (warning is LocalActivity) {
            radius = (warning.closestStrikeDistance * 1.2f).coerceIn(50f, radius)
        }
        return radius
    }

    private fun openQuickSettingsDialog() {
        val dialog = QuickSettingsDialog()
        dialog.show(supportFragmentManager, "QuickSettingsDialog")
    }

    private fun animateToLocationAndVisibleSize(
        longitude: Double,
        latitude: Double,
        diameter: Float?,
    ) {
        Log.d(LOG_TAG, "Main.animateAndZoomTo() %.4f, %.4f, %.0fkm".format(longitude, latitude, diameter))

        val mapView = mapFragment.mapView

        // If no diameter is provided, we keep the current zoomLevel
        val targetZoomLevel =
            if (diameter != null) {
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

    private fun ensurePermissions() {
        PermissionsSupport.ensure(this, *permissionRequesters)
    }

    override fun onRestart() {
        super.onRestart()

        Log.d(LOG_TAG, "Main.onRestart()")
    }

    override fun onResume() {
        super.onResume()

        stopService()

        Log.v(LOG_TAG, "Main.onResume()")

        ensurePermissions()

        mapFragment.updateForgroundColor(strikeColorHandler.lineColor)

        dataHandler.setLocationUpdatesEnabled(true)

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

        viewModel.start()
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            statusComponent.startProgress()
                        } else {
                            statusComponent.stopProgress()
                        }
                    }
                }
                launch {
                    viewModel.hasError.collect { hasError ->
                        statusComponent.indicateError(hasError)
                    }
                }
                viewModel.currentResult.value?.let { handleDataReceived(it) }
                launch {
                    viewModel.dataEvents.collect { event ->
                        dispatchDataEvent(event)
                    }
                }
                launch {
                    viewModel.locationEvents.collect { event ->
                        event?.let { dispatchLocationEvent(it) }
                    }
                }
                launch {
                    viewModel.alertEvents.collect { warning ->
                        warning?.let { dispatchAlertEvent(it) }
                    }
                }
            }
        }
    }

    private fun dispatchDataEvent(event: DataEvent) {
        when (event) {
            is DataReceived -> handleDataReceived(event)
            is StatusUpdate -> setStatusString(event.status)
            NoData -> setStatusString("?")
            else -> {}
        }

        alertHandler.dataEventConsumer(event)
        historyController.dataConsumer(event)
        binding.histogramView.dataConsumer(event)
        binding.regionView.dataConsumer(event)
    }

    private fun dispatchLocationEvent(event: LocationEvent) {
        ownLocationOverlay.locationEventConsumer(event)
        binding.alertView.locationEventConsumer(event)
        dataHandler.locationEventConsumer(event)
    }

    private fun dispatchAlertEvent(warning: Warning) {
        binding.alertView.alertEventConsumer(warning)
        statusComponent.alertEventConsumer(warning)
    }

    override fun onPause() {
        super.onPause()
        Log.v(LOG_TAG, "Main.onPause()")

        dataHandler.setLocationUpdatesEnabled(false)

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

        viewModel.stop()

        Configuration.getInstance().save(this, preferences)
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
        if (viewModel.clearDataRequested.value) {
            clearData()
        }
    }

    private fun clearData() {
        Log.v(LOG_TAG, "Main.clearData()")
        viewModel.clearDataCompleted()

        strikeListOverlay.clear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        Log.v(
            LOG_TAG,
            "Main.onRequestPermissionResult() permissions: ${permissions.joinToString(",")}, requestCode: $requestCode",
        )
        for (requester in permissionRequesters) {
            val handled = requester.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (handled) return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Log.v(LOG_TAG, "Main.onKeyUp(KEYCODE_MENU)")
            showPopupMenu(binding.upperRow)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: PreferenceKey,
    ) {
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

            PreferenceKey.ALERT_ENABLED -> {
                val alertEnabled = sharedPreferences.get(key, false)
                if (!alertEnabled) {
                    backgroundAlertEnabled = false
                }
            }

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                val alertEnabled = sharedPreferences.get(PreferenceKey.ALERT_ENABLED, false)
                backgroundAlertEnabled = alertEnabled && sharedPreferences.get(key, "0").toInt() > 0
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
        statusText += " $runStatus"

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
        popupMenu.onShowDocOverlay = docOverlayController::show
        popupMenu.showPopupMenu()
    }

    /**
     * Describes the main screen elements for the quick documentation overlay. Only views that
     * are currently visible are used; [DocOverlayController] filters the rest out.
     */
    private fun createDocTargets(): List<DocTarget> {
        val buttonColumn = listOf(binding.menu, binding.startStopAnimation, binding.goRealtime, binding.toggleExtendedMode)
            .firstOrNull { it.isVisible }
        return listOfNotNull(
            DocTarget(binding.status, getString(R.string.doc_overlay_status), BalloonTailSide.TOP),
            DocTarget(binding.warning, getString(R.string.doc_overlay_alert), BalloonTailSide.BOTTOM),
            buttonColumn?.let { DocTarget(it, getString(R.string.doc_overlay_buttons), BalloonTailSide.RIGHT) },
            DocTarget(binding.histogramView, getString(R.string.doc_overlay_histogram), BalloonTailSide.BOTTOM),
            DocTarget(binding.legendView, getString(R.string.doc_overlay_legend), BalloonTailSide.TOP),
            DocTarget(binding.timeSlider, getString(R.string.doc_overlay_time_slider), BalloonTailSide.BOTTOM),
        )
    }

    companion object {
        const val LOG_TAG = "BO_ANDROID"
        const val MAP_FRAGMENT_TAG = "org.blitzortung.MAP_FRAGMENT_TAG"
    }
}
