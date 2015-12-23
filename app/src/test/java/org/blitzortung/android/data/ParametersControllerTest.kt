package org.blitzortung.android.data

import org.hamcrest.core.Is.`is`
import org.junit.Assert.*
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
        assertTrue(parameters.isRealtime())
    }

    @Test
    fun testGetOffsetReturnsZeroByDefault() {
        assertThat(parameters.intervalOffset, `is`(0))
    }

    private fun update(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = parameters
        parameters = updater.invoke(parameters)
        return parameters != oldParameters
    }

    @Test
    fun testRewindInterval() {
        assertTrue(update({ parametersController.rewInterval(it) }))
                assertThat(parameters.intervalOffset, `is`(-15))

        assertTrue(update({ parametersController.rewInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(-30))

        for (i in 0..23 * 4 - 2 - 1 - 1) {
            assertTrue(update({parametersController.rewInterval(it)}))
        }
        assertThat(parameters.intervalOffset, `is`(-23 * 60 + 15))

        assertTrue(update({parametersController.rewInterval(it)}))
        assertThat(parameters.intervalOffset, `is`(-23 * 60))

        assertFalse(update({ parametersController.rewInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(-23 * 60))
    }

    @Test
    fun testRewindIntervalWithAlignment() {
        parametersController = ParametersController.withOffsetIncrement(45)

        assertTrue(update({ parametersController.rewInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(-45))

        assertTrue(update({ parametersController.rewInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(-90))

        for (i in 0..23 / 3 * 4 - 1) {
            assertTrue(update({ parametersController.rewInterval(it) }))
        }
        assertThat(parameters.intervalOffset, `is`(-23 * 60 + 30))

        assertFalse(update({ parametersController.rewInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(-23 * 60 + 30))
    }

    @Test
    fun testFastforwardInterval() {
        update({ parametersController.rewInterval(it) })

        assertTrue(update({ parametersController.ffwdInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(0))

        assertFalse(update({ parametersController.ffwdInterval(it) }))
        assertThat(parameters.intervalOffset, `is`(0))
    }

    @Test
    fun testGoRealtime() {
        assertFalse(update({ parametersController.goRealtime(it) }))

        update({ parametersController.rewInterval(it) })

        assertTrue(update({ parametersController.goRealtime(it) }))
        assertThat(parameters.intervalOffset, `is`(0))
    }
}
