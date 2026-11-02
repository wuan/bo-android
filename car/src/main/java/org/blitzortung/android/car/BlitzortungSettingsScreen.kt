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

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

/**
 * Car settings surface: a list of settings, starting with map dimming. New rows
 * are added here as the car experience grows.
 */
class BlitzortungSettingsScreen(carContext: CarContext) : Screen(carContext) {
    private val carSettings = CarDependenciesProvider.dependencies?.carSettings

    override fun onGetTemplate(): Template {
        val mode = carSettings?.mapDimMode ?: MapDimMode.FOLLOW_CAR

        val row =
            Row.Builder()
                .setTitle(carContext.getString(R.string.car_settings_map_dimming))
                .addText(carContext.getString(mode.labelRes))
                .setOnClickListener {
                    carContext.getCarService(ScreenManager::class.java)
                        .push(BlitzortungMapDimSettingScreen(carContext))
                }
                .build()

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.car_settings_title))
                    .setStartHeaderAction(Action.BACK)
                    .build(),
            )
            .setSingleList(ItemList.Builder().addItem(row).build())
            .build()
    }
}

/**
 * Chooses the map dimming mode: the three options are listed with the current
 * one marked, and selecting one stores it and returns to settings.
 */
class BlitzortungMapDimSettingScreen(carContext: CarContext) : Screen(carContext) {
    private val carSettings = CarDependenciesProvider.dependencies?.carSettings

    override fun onGetTemplate(): Template {
        val current = carSettings?.mapDimMode ?: MapDimMode.FOLLOW_CAR

        val itemList =
            ItemList.Builder().apply {
                MapDimMode.entries.forEach { mode ->
                    val rowBuilder =
                        Row.Builder()
                            .setTitle(carContext.getString(mode.labelRes))
                            .setOnClickListener {
                                selectMode(mode)
                                carContext.getCarService(ScreenManager::class.java).pop()
                            }
                    if (mode == current) {
                        rowBuilder.setImage(carIcon(carContext, R.drawable.car_selected))
                    }
                    addItem(rowBuilder.build())
                }
            }.build()

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.car_settings_map_dimming))
                    .setStartHeaderAction(Action.BACK)
                    .build(),
            )
            .setSingleList(itemList)
            .build()
    }

    /** Stores the selected map dimming mode. */
    internal fun selectMode(mode: MapDimMode) {
        carSettings?.mapDimMode = mode
    }
}

private val MapDimMode.labelRes: Int
    get() =
        when (this) {
            MapDimMode.FOLLOW_CAR -> R.string.car_dim_follow_car
            MapDimMode.ON -> R.string.car_dim_on
            MapDimMode.OFF -> R.string.car_dim_off
        }
