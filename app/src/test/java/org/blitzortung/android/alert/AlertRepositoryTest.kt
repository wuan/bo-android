package org.blitzortung.android.alert

import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.handler.AlertHandler
import org.junit.Before
import org.junit.Test

class AlertRepositoryTest {
    @MockK
    private lateinit var alertHandler: AlertHandler

    private lateinit var uut: AlertRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        uut = AlertRepository(alertHandler)
    }

    @Test
    fun observeAlertEventsForwardsWarnings() =
        runTest {
            val consumerSlot = slot<(Warning) -> Unit>()
            every { alertHandler.requestUpdates(capture(consumerSlot)) } answers { }

            uut.observeAlertEvents().test {
                consumerSlot.captured.invoke(NoData)
                assertThat(awaitItem()).isEqualTo(NoData)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAlertEventsRemovesConsumerOnCancellation() =
        runTest {
            val consumerSlot = slot<(Warning) -> Unit>()
            every { alertHandler.requestUpdates(capture(consumerSlot)) } answers { }

            uut.observeAlertEvents().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify { alertHandler.removeUpdates(consumerSlot.captured) }
        }

    @Test
    fun exposesAlertParameters() {
        val parameters = mockk<AlertParameters>()
        every { alertHandler.alertParameters } returns parameters

        assertThat(uut.getAlertParameters()).isSameAs(parameters)
    }
}
