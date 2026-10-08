package org.blitzortung.android.data.repository

import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.ServiceDataHandler
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStarted
import org.blitzortung.android.location.LocationEvent
import org.junit.Before
import org.junit.Test

class ServiceStrikeDataRepositoryTest {
    @MockK
    private lateinit var serviceDataHandler: ServiceDataHandler

    private lateinit var uut: ServiceStrikeDataRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        uut = ServiceStrikeDataRepository(serviceDataHandler)
    }

    @Test
    fun observeDataEventsForwardsEvents() =
        runTest {
            val consumerSlot = slot<(DataEvent) -> Unit>()
            every { serviceDataHandler.requestUpdates(capture(consumerSlot)) } answers { }

            val event = RequestStarted()
            uut.observeDataEvents().test {
                consumerSlot.captured.invoke(event)
                assertThat(awaitItem()).isEqualTo(event)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeDataEventsRemovesConsumerOnCancellation() =
        runTest {
            val consumerSlot = slot<(DataEvent) -> Unit>()
            every { serviceDataHandler.requestUpdates(capture(consumerSlot)) } answers { }

            uut.observeDataEvents().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify { serviceDataHandler.removeUpdates(consumerSlot.captured) }
        }

    @Test
    fun delegatesUpdateData() {
        uut.updateData()

        verify { serviceDataHandler.updateData() }
    }

    @Test
    fun exposesLocationEventConsumer() {
        val consumer: (LocationEvent) -> Unit = {}
        every { serviceDataHandler.locationEventConsumer } returns consumer

        assertThat(uut.locationEventConsumer).isSameAs(consumer)
    }
}
