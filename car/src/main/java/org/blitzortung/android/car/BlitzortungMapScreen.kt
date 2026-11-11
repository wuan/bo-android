/*

   Copyright 2026 Andreas Würl

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

package org.blitzortung.android.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.blitzortung.android.alert.AlertZoomTarget
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.DataReceived

/**
 * Start screen of the car experience: a map with the alert list rendered beside
 * it. The map surface is drawn by the app via [CarMapRenderer]; this screen owns
 * the template, the map action strip, and the data wiring.
 */
class BlitzortungMapScreen(
    carContext: CarContext,
) : Screen(carContext) {
    private val dependencies = CarDependenciesProvider.dependencies

    internal val renderer: CarMapRenderer? =
        dependencies?.let { OsmdroidCarMapRenderer(carContext, it.strikeColorHandler, it.carSettings) }

    private val locationPermission = CarLocationPermission(carContext)

    internal var alertListState = CarAlertListState()

    /** Registered observer so a shading change made in settings redraws the map. */
    private val onMapShadingModeChanged: (MapShadingMode) -> Unit = { invalidate() }

    /** Data timestamp last sent to the renderer, so it is only redrawn when it changes. */
    private var dataTimeText: String = ""

    /** Reference time of the latest successful data result, or null until one arrives. */
    private var dataReferenceTime: Long? = null

    /** Whether the map has reported a usable viewport that data requests can be based on. */
    private var viewportKnown = false

    /** Whether the data scheduler is running for the current visible period. */
    private var dataUpdatesStarted = false

    /**
     * Host template pieces that never change while the screen lives. Reusing them
     * keeps the host from reloading the action icons on every template rebuild,
     * which showed up as flicker at startup.
     */
    private val settingsAction: Action by lazy {
        Action.Builder()
            .setIcon(carIcon(carContext, R.drawable.car_settings))
            .setOnClickListener {
                carContext.getCarService(ScreenManager::class.java).push(BlitzortungSettingsScreen(carContext))
            }
            .build()
    }

    private val contentActionStrip: ActionStrip by lazy {
        ActionStrip.Builder()
            .addAction(settingsAction)
            .build()
    }

    /** Centres the map on the latest location fix; placed above the zoom actions. */
    internal val locationAction: Action by lazy {
        Action.Builder()
            .setIcon(carIcon(carContext, R.drawable.car_my_location))
            .setOnClickListener { onLocationAction() }
            .build()
    }

    /**
     * Frames the latest location with the shared own-location target policy. The
     * alert state and parameters are read at press time, so a unit switch or a new
     * closest strike is reflected by the next press.
     */
    internal fun onLocationAction() {
        val activeRenderer = renderer ?: return
        val spanMeters = locationTargetSpanMeters() ?: return
        activeRenderer.goToCurrentLocation(spanMeters)
    }

    /** The own-location framing span in metres, or null when no alert parameters are available. */
    internal fun locationTargetSpanMeters(): Float? {
        val alertRepository = dependencies?.alertRepository ?: return null
        return AlertZoomTarget.framingMeters(alertListState.warning, alertRepository.getAlertParameters())
    }

    private val mapActionStrip: ActionStrip by lazy {
        ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(locationAction)
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(carContext, R.drawable.car_zoom_out))
                    .setOnClickListener { renderer?.zoomOut() }
                    .build(),
            )
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(carContext, R.drawable.car_zoom_in))
                    .setOnClickListener { renderer?.zoomIn() }
                    .build(),
            )
            .build()
    }

    private val mapController: MapController by lazy {
        MapController.Builder()
            .setMapActionStrip(mapActionStrip)
            .build()
    }

    init {
        val activeRenderer = renderer
        if (activeRenderer != null) {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(CarMapSurfaceCallback(activeRenderer))
            observeViewport(activeRenderer)
            observeData(activeRenderer)
            applyDataTime(activeRenderer)
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        activeRenderer.release()
                    }
                },
            )
        }
        observeSettings()
    }

    /**
     * Reports every map viewport change to the data layer so its grid size and
     * global/local data area follow the map. The first data request is deferred
     * until the first viewport notification, so no throwaway request is made
     * before the map is laid out.
     */
    private fun observeViewport(activeRenderer: CarMapRenderer) {
        val carDependencies = dependencies ?: return
        activeRenderer.setViewportListener { viewport ->
            carDependencies.strikeDataRepository.updateViewport(viewport)
            viewportKnown = true
            startDataUpdatesIfReady()
        }
    }

    /**
     * Starts the data schedule once the map has a viewport and the screen is
     * visible. It is called both when the viewport first becomes known and each
     * time the screen returns to STARTED, so that pushing the settings screen and
     * popping back resumes the periodic refreshes.
     */
    private fun startDataUpdatesIfReady() {
        if (dataUpdatesStarted || !viewportKnown) {
            return
        }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        dataUpdatesStarted = true
        dependencies?.strikeDataRepository?.let { repository ->
            repository.start()
            repository.updateData()
        }
    }

    /**
     * Watches the car-only preferences so a change made on the settings screen is
     * reflected on the map when the map becomes visible again.
     */
    private fun observeSettings() {
        val settings = dependencies?.carSettings ?: return
        settings.observeMapShadingMode(onMapShadingModeChanged)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    settings.removeMapShadingModeObserver(onMapShadingModeChanged)
                }
            },
        )
    }

    private fun observeData(activeRenderer: CarMapRenderer) {
        val carDependencies = dependencies ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val locationRepository = carDependencies.locationRepository
                // The car must not inherit the phone's location mode: it may be
                // network/passive, and Android Automotive OS has no network provider.
                locationRepository.useProvider(CarLocationProvider.preferred(carContext))
                try {
                    launch {
                        carDependencies.strikeDataRepository.observeDataEvents().collect { event ->
                            handleDataEvent(event, activeRenderer)
                        }
                    }
                    launch {
                        carDependencies.alertRepository.observeAlertEvents().collect { warning ->
                            val updated = alertListState.copy(warning = warning)
                            if (updated != alertListState) {
                                alertListState = updated
                                invalidate()
                            }
                        }
                    }
                    launch {
                        locationRepository.observeLocationEvents().collect { event ->
                            activeRenderer.onLocationEvent(event)
                        }
                    }

                    // Resume the refresh schedule when coming back from another
                    // screen; the collectors above are registered again by
                    // repeatOnLifecycle, but the scheduler was stopped on ON_STOP.
                    startDataUpdatesIfReady()

                    locationPermission.request { granted ->
                        // The provider may have started before the host granted access.
                        if (granted && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            locationRepository.useProvider(CarLocationProvider.preferred(carContext))
                            locationRepository.startLocationUpdates()
                        } else if (!granted) {
                            // Fall back to the configured manual/default location.
                            locationRepository.clearProviderOverride()
                            locationRepository.startLocationUpdates()
                        }
                    }

                    awaitCancellation()
                } finally {
                    dataUpdatesStarted = false
                    locationRepository.clearProviderOverride()
                    carDependencies.strikeDataRepository.stop()
                }
            }
        }
    }

    /**
     * Applies a strike-data event: feeds the shared alert computation, updates the
     * alert-list state (loaded/error flags and the data timestamp), and redraws the
     * strikes for a successful result.
     */
    internal fun handleDataEvent(
        event: DataEvent,
        activeRenderer: CarMapRenderer? = renderer,
    ) {
        dependencies?.alertRepository?.onDataEvent(event)
        val updatedState = alertListState.reduce(event)
        val stateChanged = updatedState != alertListState
        alertListState = updatedState
        if (event is DataReceived && !event.failed) {
            activeRenderer?.updateStrikes(event)
            dataReferenceTime = event.referenceTime
            applyDataTime(activeRenderer)
        }
        if (stateChanged) {
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        renderer?.setShading(currentShading())

        return MapWithContentTemplate.Builder()
            .setContentTemplate(buildAlertListTemplate())
            .setActionStrip(contentActionStrip)
            .setMapController(mapController)
            .build()
    }

    /** The shading direction for the stored mode and the current car theme. */
    internal fun currentShading(): MapShading {
        val mode = dependencies?.carSettings?.mapShadingMode ?: MapShadingMode.FOLLOW_CAR
        return mode.shading(carContext.isDarkMode)
    }

    private fun buildAlertListTemplate(): ListTemplate {
        val unitName =
            (alertListState.warning as? LocalActivity)?.let {
                carContext.getString(it.parameters.measurementSystem.unitNameString)
            } ?: ""

        val rows = CarAlertListPresenter.rows(alertListState, alertStrings(), unitName)

        val itemList =
            ItemList.Builder().apply {
                rows.forEach { row ->
                    // One line per warning: keep the detail in the title instead of
                    // adding a second text line, so more warnings fit on screen.
                    val title = if (row.subtitle.isBlank()) row.title else "${row.title}: ${row.subtitle}"
                    addItem(Row.Builder().setTitle(title).build())
                }
            }.build()

        return ListTemplate.Builder()
            .setSingleList(itemList)
            .build()
    }

    /**
     * The data timestamp shown above the legend, or an empty string before a
     * successful data result has been received.
     */
    internal fun buildDataTimeText(): String {
        val referenceTime = dataReferenceTime ?: return ""
        val formatter = SimpleDateFormat(DATA_TIME_PATTERN, Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault()
        return carContext.getString(
            R.string.car_status_data_time,
            formatter.format(Date(referenceTime)),
        )
    }

    /** Sends the data timestamp to the renderer when it changed, redrawing only the map surface. */
    private fun applyDataTime(activeRenderer: CarMapRenderer? = renderer) {
        val dataTime = buildDataTimeText()
        if (dataTime == dataTimeText) {
            return
        }
        dataTimeText = dataTime
        activeRenderer?.setDataTime(dataTime)
    }

    private fun alertStrings(): CarAlertStrings =
        CarAlertStrings(
            closestStrikeLabel = carContext.getString(R.string.car_alert_closest_strike),
            emptyStateLabel = carContext.getString(R.string.car_alert_empty_state),
            noLocationLabel = carContext.getString(R.string.car_alert_no_location),
            outlyingLabel = carContext.getString(R.string.car_alert_outlying),
            noActivityLabel = carContext.getString(R.string.car_alert_no_activity),
            errorLabel = carContext.getString(R.string.car_alert_error),
        )

    private companion object {
        /** Fixed 24-hour time of day for the data timestamp. */
        const val DATA_TIME_PATTERN = "HH:mm:ss"
    }
}
