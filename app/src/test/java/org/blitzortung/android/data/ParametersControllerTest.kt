package org.blitzortung.android.data

import android.content.Context
import android.content.SharedPreferences
import org.assertj.core.api.assertThat
import org.blitzortung.android.app.ParametersComponent
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.test.createPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class ParametersControllerTest {

    private var defaultParameters = Parameters(intervalDuration = 60)

    private lateinit var controller: ParametersComponent

    @Before
    fun setUp() {
        val preferences = createPreferences { it.putString(PreferenceKey.HISTORIC_TIMESTEP.key, "15") }
        controller = ParametersComponent(preferences, defaultParameters)
    }

    @Test
    fun testIfRealtimeModeIsDefault() {
        assertThat(controller.parameters.isRealtime()).isTrue()
    }

    @Test
    fun testGetOffsetReturnsZeroByDefault() {
        assertThat(controller.parameters.intervalOffset).isEqualTo(0)
    }

    private fun update(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = defaultParameters
        defaultParameters = updater.invoke(defaultParameters)
        return defaultParameters != oldParameters
    }

    @Test
    fun testRewindInterval() {
        assertThat(controller.rewInterval()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-15)

        assertThat(controller.rewInterval()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-30)

        for (i in 0..23 * 4 - 2 - 1 - 1) {
            assertThat(controller.rewInterval()).isTrue()
        }
        assertThat(controller.parameters.intervalOffset).isEqualTo(-23 * 60 + 15)

        assertThat(controller.rewInterval()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-23 * 60)

        assertThat(controller.rewInterval()).isFalse()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-23 * 60)
    }

    @Test
    fun testRewindIntervalWithAlignment() {
        val preferences = createPreferences { it.putString(PreferenceKey.HISTORIC_TIMESTEP.key, "45") }
        controller = ParametersComponent(preferences, defaultParameters)

        assertThat(controller.rewInterval()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-45)

        assertThat(controller.rewInterval()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-90)

        for (i in 0..23 / 3 * 4 - 1) {
            assertThat(controller.rewInterval()).isTrue()
        }
        assertThat(controller.parameters.intervalOffset).isEqualTo(-23 * 60 + 30)

        assertThat(controller.rewInterval()).isFalse()
        assertThat(controller.parameters.intervalOffset).isEqualTo(-23 * 60 + 30)
    }

    @Test
    fun testFastforwardInterval() {
        controller.rewInterval()

        assertThat(controller.ffwdInterval()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(0)

        assertThat(controller.ffwdInterval()).isFalse()
        assertThat(controller.parameters.intervalOffset).isEqualTo(0)
    }

    @Test
    fun testGoRealtime() {
        assertThat(controller.goRealtime()).isFalse()

        controller.rewInterval()

        assertThat(controller.goRealtime()).isTrue()
        assertThat(controller.parameters.intervalOffset).isEqualTo(0)
    }
}
