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
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.StatusUpdate

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
    val noActivityLabel: String,
    val errorLabel: String,
    val strikeCountFormat: String,
)

/** The reported time until the next data refresh: seconds remaining of a period. */
data class CarRefreshCountdown(
    val remainingSeconds: Int,
    val periodSeconds: Int,
) {
    companion object {
        /** Parses a `"<remaining>/<period>"` status, or returns null when malformed. */
        fun parse(status: String): CarRefreshCountdown? {
            val parts = status.split('/')
            if (parts.size != 2) {
                return null
            }
            val remaining = parts[0].toIntOrNull() ?: return null
            val period = parts[1].toIntOrNull() ?: return null
            return CarRefreshCountdown(remaining, period)
        }
    }
}

/**
 * The alert-list content state: the latest alert [Warning] together with whether
 * any strike data has arrived, whether the last fetch failed, and the reported
 * countdown to the next data refresh (null when no refresh status is known).
 */
data class CarAlertListState(
    val warning: Warning = NoData,
    val hasData: Boolean = false,
    val hasError: Boolean = false,
    val refreshCountdown: CarRefreshCountdown? = null,
)

/**
 * Applies a strike-data event to the alert-list state: a `DataReceived` updates the
 * loaded/error flags and a `StatusUpdate` carries the countdown to the next refresh.
 * Other events leave the state unchanged.
 */
internal fun CarAlertListState.reduce(event: DataEvent): CarAlertListState =
    when (event) {
        is DataReceived ->
            if (event.failed) {
                copy(hasError = true)
            } else {
                copy(hasError = false, hasData = true)
            }
        is StatusUpdate -> copy(refreshCountdown = CarRefreshCountdown.parse(event.status))
        else -> this
    }

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
        val rows = mutableListOf<CarAlertRow>()

        when {
            state.hasError -> rows.add(CarAlertRow(strings.errorLabel, ""))
            !state.hasData -> rows.add(CarAlertRow(strings.emptyStateLabel, ""))
            else -> rows.addAll(alertRows(state.warning, strings, unitName))
        }

        return rows
    }

    private fun alertRows(
        warning: Warning,
        strings: CarAlertStrings,
        unitName: String,
    ): List<CarAlertRow> =
        when (warning) {
            is LocalActivity -> localActivityRows(warning, strings, unitName)
            Outlying -> listOf(CarAlertRow(strings.outlyingLabel, ""))
            NoData -> listOf(CarAlertRow(strings.emptyStateLabel, ""))
            NoLocation -> listOf(CarAlertRow(strings.noLocationLabel, ""))
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
            rows.add(CarAlertRow(strings.noActivityLabel, ""))
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
