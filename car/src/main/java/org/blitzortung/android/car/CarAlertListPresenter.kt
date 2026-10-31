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

import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.NoData
import org.blitzortung.android.alert.NoLocation
import org.blitzortung.android.alert.Outlying
import org.blitzortung.android.alert.Warning

/** A single row of the car alert list. */
data class CarAlertRow(
    val title: String,
    val subtitle: String,
)

/** Localized labels used to render the car alert list. */
data class CarAlertStrings(
    val closestStrikeLabel: String,
    val emptyStateLabel: String,
    val noLocationLabel: String,
    val outlyingLabel: String,
    val errorLabel: String,
    val strikeCountFormat: String,
)

/**
 * The alert-list content state: the latest alert [Warning] together with whether
 * any strike data has arrived and whether the last fetch failed.
 */
data class CarAlertListState(
    val warning: Warning = NoData,
    val hasData: Boolean = false,
    val hasError: Boolean = false,
)

/**
 * Maps alert state to the rows shown beside the car map: the closest strike's
 * distance and bearing, then a row per sector that saw strikes.
 */
object CarAlertListPresenter {
    fun rows(
        state: CarAlertListState,
        strings: CarAlertStrings,
        unitName: String,
    ): List<CarAlertRow> {
        if (state.hasError) {
            return listOf(CarAlertRow(strings.errorLabel, ""))
        }
        if (!state.hasData) {
            return listOf(CarAlertRow(strings.emptyStateLabel, ""))
        }

        return when (val warning = state.warning) {
            is LocalActivity -> localActivityRows(warning, strings, unitName)
            Outlying -> listOf(CarAlertRow(strings.outlyingLabel, ""))
            NoData -> listOf(CarAlertRow(strings.emptyStateLabel, ""))
            NoLocation -> listOf(CarAlertRow(strings.noLocationLabel, ""))
        }
    }

    private fun localActivityRows(
        activity: LocalActivity,
        strings: CarAlertStrings,
        unitName: String,
    ): List<CarAlertRow> {
        val rows = mutableListOf<CarAlertRow>()

        val closestStrike = activity.sectorWithClosestStrike
        if (closestStrike != null) {
            val subtitle = "%.0f%s %s".format(activity.closestStrikeDistance, unitName, activity.bearingName)
            rows.add(CarAlertRow(strings.closestStrikeLabel, subtitle))
        } else {
            rows.add(CarAlertRow(strings.emptyStateLabel, ""))
        }

        activity.sectors
            .map { sector -> sector.label to sector.ranges.sumOf { range -> range.strikeCount } }
            .filter { (_, count) -> count > 0 }
            .forEach { (label, count) ->
                rows.add(CarAlertRow(label, strings.strikeCountFormat.format(count)))
            }

        return rows
    }
}
