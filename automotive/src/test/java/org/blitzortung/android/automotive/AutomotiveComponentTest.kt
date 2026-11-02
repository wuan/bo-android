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

package org.blitzortung.android.automotive

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AutomotiveComponentTest {
    @Test
    fun `resolves the car dependencies from the embedded graph`() {
        val application = RuntimeEnvironment.getApplication()

        val component =
            DaggerAutomotiveComponent
                .builder()
                .appModule(AppModule(application))
                .serviceModule(ServiceModule())
                .build()

        val dependencies = component.carDependencies()

        assertThat(dependencies.strikeDataRepository).isNotNull()
        assertThat(dependencies.locationRepository).isNotNull()
        assertThat(dependencies.alertRepository).isNotNull()
        assertThat(dependencies.strikeColorHandler).isNotNull()
    }
}
