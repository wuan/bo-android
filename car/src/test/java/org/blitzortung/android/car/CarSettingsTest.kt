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

import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CarSettingsTest {
    @Test
    fun `an unset mode defaults to follow car`() {
        assertThat(CarSettings(context()).mapDimMode).isEqualTo(MapDimMode.FOLLOW_CAR)
    }

    @Test
    fun `each mode round-trips through the store`() {
        val settings = CarSettings(context())

        MapDimMode.entries.forEach { mode ->
            settings.mapDimMode = mode
            assertThat(settings.mapDimMode).isEqualTo(mode)
        }
    }

    @Test
    fun `an unknown stored value falls back to follow car`() {
        val context = context()
        context
            .getSharedPreferences(CarSettings.PREFS_NAME, 0)
            .edit()
            .putString(CarSettings.KEY_MAP_DIM_MODE, "SOMETHING_ELSE")
            .commit()

        assertThat(CarSettings(context).mapDimMode).isEqualTo(MapDimMode.FOLLOW_CAR)
    }

    @Test
    fun `the setting is not written to the default preferences`() {
        val context = context()

        CarSettings(context).mapDimMode = MapDimMode.ON

        assertThat(PreferenceManager.getDefaultSharedPreferences(context).contains(CarSettings.KEY_MAP_DIM_MODE)).isFalse()
    }

    @Test
    fun `writing a mode notifies a registered observer`() {
        val settings = CarSettings(context())
        val observed = mutableListOf<MapDimMode>()
        settings.observeMapDimMode { observed.add(it) }

        settings.mapDimMode = MapDimMode.OFF

        assertThat(observed).containsExactly(MapDimMode.OFF)
    }

    @Test
    fun `a removed observer is no longer notified`() {
        val settings = CarSettings(context())
        val observed = mutableListOf<MapDimMode>()
        val observer: (MapDimMode) -> Unit = { observed.add(it) }
        settings.observeMapDimMode(observer)
        settings.removeMapDimModeObserver(observer)

        settings.mapDimMode = MapDimMode.ON

        assertThat(observed).isEmpty()
    }

    @Test
    fun `mode maps to a dimmed state`() {
        assertThat(MapDimMode.FOLLOW_CAR.isDimmed(carIsDark = true)).isTrue()
        assertThat(MapDimMode.FOLLOW_CAR.isDimmed(carIsDark = false)).isFalse()
        assertThat(MapDimMode.ON.isDimmed(carIsDark = false)).isTrue()
        assertThat(MapDimMode.OFF.isDimmed(carIsDark = true)).isFalse()
    }

    @Test
    fun `no camera is stored by default`() {
        assertThat(CarSettings(context()).camera).isNull()
    }

    @Test
    fun `a stored camera round-trips through the store`() {
        val settings = CarSettings(context())

        settings.storeCamera(CarCamera(latitude = 52.5, longitude = 13.25, zoom = 7.5))

        val camera = settings.camera
        assertThat(camera).isNotNull
        assertThat(camera!!.latitude).isCloseTo(52.5, within(0.0001))
        assertThat(camera.longitude).isCloseTo(13.25, within(0.0001))
        assertThat(camera.zoom).isCloseTo(7.5, within(0.0001))
    }

    @Test
    fun `a partial camera is treated as not stored`() {
        val context = context()
        context
            .getSharedPreferences(CarSettings.PREFS_NAME, 0)
            .edit()
            .putFloat(CarSettings.KEY_CAMERA_LATITUDE, 1f)
            .putFloat(CarSettings.KEY_CAMERA_LONGITUDE, 2f)
            .commit()

        assertThat(CarSettings(context).camera).isNull()
    }

    @Test
    fun `the camera is not written to the default preferences`() {
        val context = context()

        CarSettings(context).storeCamera(CarCamera(1.0, 2.0, 3.0))

        val defaults = PreferenceManager.getDefaultSharedPreferences(context)
        assertThat(defaults.contains(CarSettings.KEY_CAMERA_LATITUDE)).isFalse()
        assertThat(defaults.contains(CarSettings.KEY_CAMERA_LONGITUDE)).isFalse()
        assertThat(defaults.contains(CarSettings.KEY_CAMERA_ZOOM)).isFalse()
    }

    private fun context() = RuntimeEnvironment.getApplication()
}
