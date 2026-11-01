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
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.RequestStarted
import org.blitzortung.android.data.provider.result.StatusUpdate
import org.junit.Test

class CarAlertListReducerTest {
    @Test
    fun `a successful result marks the data as loaded`() {
        val event = DataReceived(parameters = Parameters(), flags = Flags(), failed = false)

        val state = CarAlertListState(hasError = true).reduce(event)

        assertThat(state.hasData).isTrue()
        assertThat(state.hasError).isFalse()
    }

    @Test
    fun `a failed result marks the error state`() {
        val event = DataReceived(parameters = Parameters(), flags = Flags(), failed = true)

        val state = CarAlertListState(hasData = true).reduce(event)

        assertThat(state.hasError).isTrue()
        assertThat(state.hasData).isTrue()
    }

    @Test
    fun `a status update carries the seconds to the next refresh`() {
        val state = CarAlertListState().reduce(StatusUpdate("40/60"))

        assertThat(state.refreshSecondsRemaining).isEqualTo(40)
    }

    @Test
    fun `a malformed status update clears the countdown`() {
        val state = CarAlertListState(refreshSecondsRemaining = 40).reduce(StatusUpdate("n/a"))

        assertThat(state.refreshSecondsRemaining).isNull()
    }

    @Test
    fun `other events leave the state unchanged`() {
        val initial = CarAlertListState(hasData = true, refreshSecondsRemaining = 12)

        assertThat(initial.reduce(RequestStarted())).isEqualTo(initial)
    }
}
