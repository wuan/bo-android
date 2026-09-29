package org.blitzortung.android.app.viewmodel

import androidx.lifecycle.ViewModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.NoData
import org.blitzortung.android.data.provider.result.RequestStarted
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @MockK
    private lateinit var strikeDataRepository: StrikeDataRepository

    @MockK
    private lateinit var locationRepository: LocationRepository

    @MockK
    private lateinit var alertRepository: AlertRepository

    private val dataEvents = MutableSharedFlow<DataEvent>(extraBufferCapacity = 8)

    private lateinit var uut: MainViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { strikeDataRepository.observeDataEvents() } returns dataEvents
        every { locationRepository.observeLocationEvents() } returns MutableSharedFlow<LocationEvent>()
        every { alertRepository.observeAlertEvents() } returns MutableSharedFlow<Warning>()

        uut = MainViewModel(strikeDataRepository, locationRepository, alertRepository)
    }

    private fun TestScope.collectDataEvents() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            uut.dataEvents.collect { }
        }
    }

    @Test
    fun noStateUpdateWithoutCollector() =
        runTest {
            dataEvents.emit(RequestStarted())

            assertThat(uut.isLoading.value).isFalse()
        }

    @Test
    fun requestStartedSetsLoading() =
        runTest {
            collectDataEvents()

            dataEvents.emit(RequestStarted())

            assertThat(uut.isLoading.value).isTrue()
        }

    @Test
    fun dataEventsAreNotConflated() =
        runTest {
            val received = mutableListOf<DataEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                uut.dataEvents.collect { received.add(it) }
            }

            dataEvents.emit(RequestStarted())
            dataEvents.emit(NoData)

            assertThat(received).hasSize(2)
            assertThat(received[0]).isInstanceOf(RequestStarted::class.java)
            assertThat(received[1]).isSameAs(NoData)
        }

    @Test
    fun successfulDataReceivedClearsLoadingAndStoresResult() =
        runTest {
            collectDataEvents()
            val result = DataReceived(parameters = Parameters(), flags = Flags(), failed = false)

            dataEvents.emit(result)

            assertThat(uut.isLoading.value).isFalse()
            assertThat(uut.hasError.value).isFalse()
            assertThat(uut.currentResult.value).isEqualTo(result)
        }

    @Test
    fun failedDataReceivedSetsErrorAndKeepsCurrentResult() =
        runTest {
            collectDataEvents()
            val success = DataReceived(parameters = Parameters(), flags = Flags(), failed = false)
            dataEvents.emit(success)

            val failure = DataReceived(parameters = Parameters(), flags = Flags(), failed = true)
            dataEvents.emit(failure)

            assertThat(uut.hasError.value).isTrue()
            assertThat(uut.currentResult.value).isEqualTo(success)
        }

    @Test
    fun otherDataEventsStopLoading() =
        runTest {
            collectDataEvents()
            dataEvents.emit(RequestStarted())
            dataEvents.emit(NoData)

            assertThat(uut.isLoading.value).isFalse()
        }

    @Test
    fun delegatesCommands() {
        every { strikeDataRepository.getMode() } returns Mode.DATA
        every { strikeDataRepository.getParameters() } returns Parameters()
        every { strikeDataRepository.getIntervalDuration() } returns 60
        every { strikeDataRepository.isRealtime() } returns true

        uut.updateData()
        uut.start()
        uut.stop()
        uut.restart()
        uut.startAnimation()
        uut.goRealtime()
        uut.setPosition(1)
        uut.historySteps()
        uut.toggleExtendedMode()
        uut.enableBackgroundLocation()
        uut.disableBackgroundLocation()

        assertThat(uut.getParameters()).isNotNull()
        assertThat(uut.getIntervalDuration()).isEqualTo(60)
        assertThat(uut.isRealtime()).isTrue()
        assertThat(uut.getMode()).isEqualTo(Mode.DATA)

        verify {
            strikeDataRepository.updateData()
            strikeDataRepository.start()
            strikeDataRepository.stop()
            strikeDataRepository.restart()
            strikeDataRepository.startAnimation()
            strikeDataRepository.goRealtime()
            strikeDataRepository.setPosition(1)
            strikeDataRepository.historySteps()
            strikeDataRepository.toggleExtendedMode()
            locationRepository.enableBackgroundMode()
            locationRepository.disableBackgroundMode()
        }
    }

    @Test
    fun onClearedStopsDataUpdates() {
        val method = ViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(uut)

        verify { strikeDataRepository.stop() }
    }

    @Test
    fun togglesClearDataRequest() {
        uut.requestClearData()
        assertThat(uut.clearDataRequested.value).isTrue()

        uut.clearDataCompleted()
        assertThat(uut.clearDataRequested.value).isFalse()
    }
}
