package org.blitzortung.android.data

import org.assertj.core.api.KotlinAssertions.Companion.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParametersControllerTest {

    private lateinit var parameters: Parameters

    private lateinit var controller: ParametersController

    @Before
    fun setUp() {
        parameters = Parameters(intervalDuration = 60)
        controller = ParametersController.withOffsetIncrement(15)
    }

    @Test
    fun testIfRealtimeModeIsDefault() {
        assertThat(parameters.isRealtime()).isTrue()
    }

    @Test
    fun testGetOffsetReturnsZeroByDefault() {
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }

    private fun update(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = parameters
        parameters = updater.invoke(parameters)
        return parameters != oldParameters
    }

    @Test
    fun testRewindInterval() {
        assertThat(update({ controller.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-15)

        assertThat(update({ controller.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-30)

        for (i in 0..23 * 4 - 2 - 1 - 1) {
            assertThat(update({ controller.rewInterval(it) })).isTrue()
        }
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60 + 15)

        assertThat(update({ controller.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60)

        assertThat(update({ controller.rewInterval(it) })).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60)
    }

    @Test
    fun testRewindIntervalWithAlignment() {
        controller = ParametersController.withOffsetIncrement(45)

        assertThat(update({ controller.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-45)

        assertThat(update({ controller.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-90)

        for (i in 0..23 / 3 * 4 - 1) {
            assertThat(update({ controller.rewInterval(it) })).isTrue()
        }
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60 + 30)

        assertThat(update({ controller.rewInterval(it) })).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60 + 30)
    }

    @Test
    fun testFastforwardInterval() {
        update({ controller.rewInterval(it) })

        assertThat(update({ controller.ffwdInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(0)

        assertThat(update({ controller.ffwdInterval(it) })).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }

    @Test
    fun testGoRealtime() {
        assertThat(update({ controller.goRealtime(it) })).isFalse()

        update({ controller.rewInterval(it) })

        assertThat(update({ controller.goRealtime(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }
}
