package org.blitzortung.android.data.repository

import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.ClusterParameters
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.provider.result.ClusterEvent
import org.blitzortung.android.data.provider.result.ClusterReceived
import org.blitzortung.android.data.provider.result.NoClusterData
import org.junit.Before
import org.junit.Test

class ClusterRepositoryTest {
    @MockK
    private lateinit var mainDataHandler: MainDataHandler

    private lateinit var uut: ClusterRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        uut = ClusterRepository(mainDataHandler)
    }

    @Test
    fun observeClusterEventsForwardsEvents() =
        runTest {
            val consumerSlot = slot<(ClusterEvent) -> Unit>()
            every { mainDataHandler.requestClusterUpdates(capture(consumerSlot)) } answers { }

            val event = ClusterReceived(parameters = ClusterParameters(), referenceTime = 7L)
            uut.observeClusterEvents().test {
                consumerSlot.captured.invoke(event)
                assertThat(awaitItem()).isEqualTo(event)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeClusterEventsRemovesConsumerOnCancellation() =
        runTest {
            val consumerSlot = slot<(ClusterEvent) -> Unit>()
            every { mainDataHandler.requestClusterUpdates(capture(consumerSlot)) } answers { }

            uut.observeClusterEvents().test {
                consumerSlot.captured.invoke(NoClusterData)
                assertThat(awaitItem()).isEqualTo(NoClusterData)
                cancelAndIgnoreRemainingEvents()
            }

            verify { mainDataHandler.removeClusterUpdates(consumerSlot.captured) }
        }
}
