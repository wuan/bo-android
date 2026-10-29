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
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
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
                        if (event is DataReceived && !event.failed) {
                            activeRenderer.updateStrikes(event)
                        }
                    }
                }
                launch {
                    carDependencies.locationRepository.observeLocationEvents().collect { event ->
                        activeRenderer.onLocationEvent(event)
                    }
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

        val pane =
            Pane.Builder()
                .addRow(Row.Builder().setTitle(carContext.getString(R.string.car_status_loading)).build())
                .build()

        val content =
            PaneTemplate.Builder(pane)
                .setHeader(
                    Header.Builder()
                        .setTitle(carContext.getString(R.string.car_app_name))
                        .build(),
                )
                .build()

        return MapWithContentTemplate.Builder()
            .setContentTemplate(content)
            .setActionStrip(buildMapActionStrip())
            .build()
    }

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
