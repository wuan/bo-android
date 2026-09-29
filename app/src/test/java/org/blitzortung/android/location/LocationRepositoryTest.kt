package org.blitzortung.android.location

import android.location.Location
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class LocationRepositoryTest {
    @MockK
    private lateinit var locationHandler: LocationHandler

    private lateinit var uut: LocationRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        uut = LocationRepository(locationHandler)
    }

    @Test
    fun observeLocationEventsForwardsEvents() =
        runTest {
            val consumerSlot = slot<(LocationEvent) -> Unit>()
            every { locationHandler.requestUpdates(capture(consumerSlot)) } answers { }

            uut.observeLocationEvents().test {
                consumerSlot.captured.invoke(NoLocation)
                assertThat(awaitItem()).isEqualTo(NoLocation)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeLocationEventsRemovesConsumerOnCancellation() =
        runTest {
            val consumerSlot = slot<(LocationEvent) -> Unit>()
            every { locationHandler.requestUpdates(capture(consumerSlot)) } answers { }

            uut.observeLocationEvents().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify { locationHandler.removeUpdates(consumerSlot.captured) }
        }

    @Test
    fun exposesCurrentLocationAndBackgroundMode() {
        val location: Location = Location("test")
        every { locationHandler.location } returns location

        assertThat(uut.getCurrentLocation()).isSameAs(location)

        uut.enableBackgroundMode()
        uut.disableBackgroundMode()

        verify {
            locationHandler.enableBackgroundMode()
            locationHandler.disableBackgroundMode()
        }
    }
}
