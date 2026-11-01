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
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
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

    /** Registered observer so a dimming change made in settings redraws the map. */
    private val onMapDimModeChanged: (MapDimMode) -> Unit = { invalidate() }

    init {
        val activeRenderer = renderer
        if (activeRenderer != null) {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(CarMapSurfaceCallback(activeRenderer))
            observeData(activeRenderer)
        }
        observeSettings()
    }

    /**
     * Watches the car-only preferences so a change made on the settings screen is
     * reflected on the map when the map becomes visible again.
     */
    private fun observeSettings() {
        val settings = dependencies?.carSettings ?: return
        settings.observeMapDimMode(onMapDimModeChanged)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    settings.removeMapDimModeObserver(onMapDimModeChanged)
                }
            },
        )
    }

    private fun observeData(activeRenderer: CarMapRenderer) {
        val carDependencies = dependencies ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    carDependencies.strikeDataRepository.observeDataEvents().collect { event ->
                        handleDataEvent(event, activeRenderer)
                    }
                }
                launch {
                    carDependencies.alertRepository.observeAlertEvents().collect { warning ->
                        alertListState = alertListState.copy(warning = warning)
                        invalidate()
                    }
                }
                launch {
                    carDependencies.locationRepository.observeLocationEvents().collect { event ->
                        activeRenderer.onLocationEvent(event)
                    }
                }

                locationPermission.request {
                    // On denial the configured default/manual location stays the fallback.
                }

                carDependencies.strikeDataRepository.start()
                carDependencies.strikeDataRepository.updateData()

                try {
                    awaitCancellation()
                } finally {
                    carDependencies.strikeDataRepository.stop()
                }
            }
        }
    }

    /**
     * Applies a strike-data event: feeds the shared alert computation, updates the
     * alert-list state (loaded/error flags and the refresh countdown), and redraws
     * the strikes for a successful result.
     */
    internal fun handleDataEvent(
        event: DataEvent,
        activeRenderer: CarMapRenderer? = renderer,
    ) {
        dependencies?.alertRepository?.onDataEvent(event)
        alertListState = alertListState.reduce(event)
        if (event is DataReceived && !event.failed) {
            activeRenderer?.updateStrikes(event)
        }
        invalidate()
    }

    override fun onGetTemplate(): Template {
        renderer?.setDimmed(isMapDimmed())

        return MapWithContentTemplate.Builder()
            .setContentTemplate(buildAlertListTemplate())
            .setActionStrip(buildMapActionStrip())
            .build()
    }

    /** Whether the map surface should be dimmed for the stored mode and car theme. */
    internal fun isMapDimmed(): Boolean {
        val mode = dependencies?.carSettings?.mapDimMode ?: MapDimMode.FOLLOW_CAR
        return mode.isDimmed(carContext.isDarkMode)
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
                    val rowBuilder = Row.Builder().setTitle(row.title)
                    if (row.subtitle.isNotBlank()) {
                        rowBuilder.addText(row.subtitle)
                    }
                    addItem(rowBuilder.build())
                }
            }.build()

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(buildHeaderTitle())
                    .addEndHeaderAction(settingsAction())
                    .build(),
            )
            .setSingleList(itemList)
            .build()
    }

    private fun settingsAction(): Action =
        Action.Builder()
            .setIcon(carIcon(carContext, R.drawable.car_settings))
            .setOnClickListener {
                carContext.getCarService(ScreenManager::class.java).push(BlitzortungSettingsScreen(carContext))
            }
            .build()

    /**
     * The header line: the app name, followed by the compact refresh countdown when a
     * refresh status is known.
     */
    private fun buildHeaderTitle(): String {
        val appName = carContext.getString(R.string.car_app_name)
        val countdown = alertListState.refreshCountdown ?: return appName
        val countdownText =
            carContext.getString(
                R.string.car_status_next_refresh,
                countdown.remainingSeconds,
                countdown.periodSeconds,
            )
        return carContext.getString(R.string.car_status_header, appName, countdownText)
    }

    private fun alertStrings(): CarAlertStrings =
        CarAlertStrings(
            closestStrikeLabel = carContext.getString(R.string.car_alert_closest_strike),
            emptyStateLabel = carContext.getString(R.string.car_alert_empty_state),
            noLocationLabel = carContext.getString(R.string.car_alert_no_location),
            outlyingLabel = carContext.getString(R.string.car_alert_outlying),
            noActivityLabel = carContext.getString(R.string.car_alert_no_activity),
            errorLabel = carContext.getString(R.string.car_alert_error),
            strikeCountFormat = carContext.getString(R.string.car_alert_strike_count),
        )

    private fun buildMapActionStrip(): ActionStrip {
        val activeRenderer = renderer
        return ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(carContext, R.drawable.car_zoom_out))
                    .setOnClickListener { activeRenderer?.zoomOut() }
                    .build(),
            )
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(carContext, R.drawable.car_zoom_in))
                    .setOnClickListener { activeRenderer?.zoomIn() }
                    .build(),
            )
            .build()
    }
}
