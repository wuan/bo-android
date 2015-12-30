package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParametersControllerTest {

    private lateinit var parameters: Parameters

    private lateinit var parametersController: ParametersController

    @Before
    fun setUp() {
        parameters = Parameters(intervalDuration = 60)
        parametersController = ParametersController.withOffsetIncrement(15)
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
        assertThat(update({ parametersController.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-15)

        assertThat(update({ parametersController.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-30)

        for (i in 0..23 * 4 - 2 - 1 - 1) {
            assertThat(update({parametersController.rewInterval(it)})).isTrue()
        }
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60 + 15)

        assertThat(update({parametersController.rewInterval(it)})).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60)

        assertThat(update({ parametersController.rewInterval(it) })).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60)
    }

    @Test
    fun testRewindIntervalWithAlignment() {
        parametersController = ParametersController.withOffsetIncrement(45)

        assertThat(update({ parametersController.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-45)

        assertThat(update({ parametersController.rewInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(-90)

        for (i in 0..23 / 3 * 4 - 1) {
            assertThat(update({ parametersController.rewInterval(it) })).isTrue()
        }
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60 + 30)

        assertThat(update({ parametersController.rewInterval(it) })).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(-23 * 60 + 30)
    }

    @Test
    fun testFastforwardInterval() {
        update({ parametersController.rewInterval(it) })

        assertThat(update({ parametersController.ffwdInterval(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(0)

        assertThat(update({ parametersController.ffwdInterval(it) })).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }

    @Test
    fun testGoRealtime() {
        assertThat(update({ parametersController.goRealtime(it) })).isFalse()

        update({ parametersController.rewInterval(it) })

        assertThat(update({ parametersController.goRealtime(it) })).isTrue()
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }
}
