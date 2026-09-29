package org.blitzortung.android.data.repository

import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.cache.CacheSize
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStarted
import org.junit.Before
import org.junit.Test

class StrikeDataRepositoryTest {
    @MockK
    private lateinit var mainDataHandler: MainDataHandler

    private lateinit var uut: StrikeDataRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        uut = StrikeDataRepository(mainDataHandler)
    }

    @Test
    fun observeDataEventsForwardsEvents() =
        runTest {
            val consumerSlot = slot<(DataEvent) -> Unit>()
            every { mainDataHandler.requestUpdates(capture(consumerSlot)) } answers { }

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
            every { mainDataHandler.requestUpdates(capture(consumerSlot)) } answers { }

            uut.observeDataEvents().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify { mainDataHandler.removeUpdates(consumerSlot.captured) }
        }

    @Test
    fun delegatesDataCommands() {
        uut.updateData()
        uut.start()
        uut.stop()
        uut.restart()
        uut.startAnimation()
        uut.toggleExtendedMode()

        verify {
            mainDataHandler.updateData()
            mainDataHandler.start()
            mainDataHandler.stop()
            mainDataHandler.restart()
            mainDataHandler.startAnimation()
            mainDataHandler.toggleExtendedMode()
        }
    }

    @Test
    fun delegatesParameterCommands() {
        every { mainDataHandler.goRealtime() } returns true
        every { mainDataHandler.setPosition(any()) } returns true
        every { mainDataHandler.historySteps() } returns 5
        every { mainDataHandler.isRealtime } returns true
        every { mainDataHandler.intervalDuration } returns 60

        assertThat(uut.goRealtime()).isTrue()
        assertThat(uut.setPosition(3)).isTrue()
        assertThat(uut.historySteps()).isEqualTo(5)
        assertThat(uut.isRealtime()).isTrue()
        assertThat(uut.getIntervalDuration()).isEqualTo(60)
        assertThat(uut.getParameters()).isEqualTo(mainDataHandler.parameters)
    }

    @Test
    fun delegatesCacheSize() {
        val cacheSize = mockk<CacheSize>()
        every { mainDataHandler.calculateTotalCacheSize() } returns cacheSize

        assertThat(uut.calculateTotalCacheSize()).isSameAs(cacheSize)
    }
}
