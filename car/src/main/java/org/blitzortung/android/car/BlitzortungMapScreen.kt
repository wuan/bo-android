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
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate

/**
 * Start screen of the car experience: a map with the alert list rendered beside
 * it. The map surface is drawn by the app (see the rendering tasks); this screen
 * owns the template and the map action strip.
 */
class BlitzortungMapScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
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

        val actionStrip = ActionStrip.Builder().addAction(Action.PAN).build()

        return MapWithContentTemplate.Builder()
            .setContentTemplate(content)
            .setActionStrip(actionStrip)
            .build()
    }
}
