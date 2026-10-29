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
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.blitzortung.android.alert.LocalActivity
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
        dependencies?.let { OsmdroidCarMapRenderer(carContext, it.strikeColorHandler) }

    private val locationPermission = CarLocationPermission(carContext)

    private var alertListState = CarAlertListState()

    init {
        val activeRenderer = renderer
        if (activeRenderer != null) {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(CarMapSurfaceCallback(activeRenderer))
            observeData(activeRenderer)
        }
    }

    private fun observeData(activeRenderer: CarMapRenderer) {
        val carDependencies = dependencies ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    carDependencies.strikeDataRepository.observeDataEvents().collect { event ->
                        if (event is DataReceived) {
                            if (event.failed) {
                                alertListState = alertListState.copy(hasError = true)
                            } else {
                                alertListState = alertListState.copy(hasError = false, hasData = true)
                                activeRenderer.updateStrikes(event)
                            }
                            invalidate()
                        }
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

    override fun onGetTemplate(): Template {
        renderer?.setDarkMode(carContext.isDarkMode)

        return MapWithContentTemplate.Builder()
            .setContentTemplate(buildAlertListTemplate())
            .setActionStrip(buildMapActionStrip())
            .build()
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
                    .setTitle(carContext.getString(R.string.car_app_name))
                    .build(),
            )
            .setSingleList(itemList)
            .build()
    }

    private fun alertStrings(): CarAlertStrings =
        CarAlertStrings(
            closestStrikeLabel = carContext.getString(R.string.car_alert_closest_strike),
            emptyStateLabel = carContext.getString(R.string.car_alert_empty_state),
            noLocationLabel = carContext.getString(R.string.car_alert_no_location),
            outlyingLabel = carContext.getString(R.string.car_alert_outlying),
            errorLabel = carContext.getString(R.string.car_alert_error),
            strikeCountFormat = carContext.getString(R.string.car_alert_strike_count),
        )

    private fun buildMapActionStrip(): ActionStrip {
        val activeRenderer = renderer
        return ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(
                Action.Builder()
                    .setIcon(icon(R.drawable.car_zoom_out))
                    .setOnClickListener { activeRenderer?.zoomOut() }
                    .build(),
            )
            .addAction(
                Action.Builder()
                    .setIcon(icon(R.drawable.car_zoom_in))
                    .setOnClickListener { activeRenderer?.zoomIn() }
                    .build(),
            )
            .build()
    }

    private fun icon(resourceId: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resourceId)).build()
}
