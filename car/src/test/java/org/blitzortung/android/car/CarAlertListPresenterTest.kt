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

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Test

class CarAlertListPresenterTest {
    private val strings =
        CarAlertStrings(
            closestStrikeLabel = "Closest strike",
            emptyStateLabel = "Waiting for lightning data",
            noLocationLabel = "Location unknown",
            outlyingLabel = "No strikes in range",
            errorLabel = "Lightning data unavailable",
            strikeCountFormat = "%d strikes",
        )

    @Test
    fun `reports the closest strike distance and bearing for a local activity`() {
        val state = CarAlertListState(warning = localActivity(), hasData = true)

        val rows = CarAlertListPresenter.rows(state, strings, "km")

        assertThat(rows.first().title).isEqualTo("Closest strike")
        assertThat(rows.first().subtitle).isEqualTo("8km NE")
    }

    @Test
    fun `reports the strike count per sector`() {
        val state = CarAlertListState(warning = localActivity(), hasData = true)

        val rows = CarAlertListPresenter.rows(state, strings, "km")

        assertThat(rows).contains(CarAlertRow("NE", "5 strikes"))
    }

    @Test
    fun `shows the empty state until strike data arrives`() {
        val rows = CarAlertListPresenter.rows(CarAlertListState(hasData = false), strings, "km")

        assertThat(rows).containsExactly(CarAlertRow("Waiting for lightning data", ""))
    }

    @Test
    fun `shows the error state when a fetch failed`() {
        val state = CarAlertListState(hasError = true, hasData = false)

        val rows = CarAlertListPresenter.rows(state, strings, "km")

        assertThat(rows).containsExactly(CarAlertRow("Lightning data unavailable", ""))
    }

    @Test
    fun `shows the no location state`() {
        val state = CarAlertListState(warning = org.blitzortung.android.alert.NoLocation, hasData = true)

        val rows = CarAlertListPresenter.rows(state, strings, "km")

        assertThat(rows).containsExactly(CarAlertRow("Location unknown", ""))
    }

    private fun localActivity(): LocalActivity {
        val parameters =
            AlertParameters(
                alarmInterval = 10 * 60 * 1000L,
                rangeSteps = listOf(10f, 25f),
                sectorLabels = listOf("N", "NE", "E"),
                measurementSystem = MeasurementSystem.METRIC,
            )
        val sector =
            AlertSector(
                label = "NE",
                minimumSectorBearing = 45f,
                maximumSectorBearing = 90f,
                ranges =
                    listOf(
                        AlertSectorRange(rangeMinimum = 0f, rangeMaximum = 10f, strikeCount = 3, latestStrikeTimestamp = 0L),
                        AlertSectorRange(rangeMinimum = 10f, rangeMaximum = 25f, strikeCount = 2, latestStrikeTimestamp = 0L),
                    ),
                closestStrikeDistance = 8f,
            )
        return LocalActivity(sectors = listOf(sector), parameters = parameters, referenceTime = 0L)
    }
}
