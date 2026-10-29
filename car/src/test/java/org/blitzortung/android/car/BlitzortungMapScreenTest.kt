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

import androidx.car.app.model.Action
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.testing.TestCarContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BlitzortungMapScreenTest {
    private fun createScreen(): BlitzortungMapScreen {
        val carContext = TestCarContext.createCarContext(RuntimeEnvironment.getApplication())
        return BlitzortungMapScreen(carContext)
    }

    @Test
    fun `start screen renders a map with content`() {
        val template = createScreen().onGetTemplate()

        assertThat(template).isInstanceOf(MapWithContentTemplate::class.java)
    }

    @Test
    fun `map action strip offers pan and zoom actions`() {
        val template = createScreen().onGetTemplate() as MapWithContentTemplate

        val actionTypes = template.actionStrip?.actions.orEmpty().map { it.type }

        assertThat(actionTypes).contains(Action.TYPE_PAN)
        // Two additional icon-only actions for zooming in and out.
        assertThat(actionTypes).hasSize(3)
    }
}
