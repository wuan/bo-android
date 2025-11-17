/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.History
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.RequestStartedEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var strikeDataRepository: StrikeDataRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var alertRepository: AlertRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        strikeDataRepository = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        alertRepository = mockk(relaxed = true)

        // Set up default flows
        every { strikeDataRepository.observeDataEvents() } returns flowOf()
        every { locationRepository.observeLocationEvents() } returns flowOf()
        every { alertRepository.observeAlertEvents() } returns flowOf()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading and has no error`() {
        viewModel = createViewModel()

        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.hasError.value).isFalse()
        assertThat(viewModel.currentResult.value).isNull()
    }

    @Test
    fun `loading state updates on request started event`() =
        runTest {
            val event = RequestStartedEvent()
            every { strikeDataRepository.observeDataEvents() } returns flowOf(event)

            viewModel = createViewModel()

            viewModel.isLoading.test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `result updates current result and clears loading`() =
        runTest {
            val resultEvent =
                ResultEvent(
                    parameters = Parameters(),
                    flags = Flags(mode = Mode.DATA),
                    failed = false,
                )
            every { strikeDataRepository.observeDataEvents() } returns flowOf(resultEvent)

            viewModel = createViewModel()

            viewModel.currentResult.test {
                val result = awaitItem()
                assertThat(result).isEqualTo(resultEvent)
            }

            viewModel.isLoading.test {
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `failed result sets error state`() =
        runTest {
            val resultEvent =
                ResultEvent(
                    parameters = Parameters(),
                    flags = Flags(mode = Mode.DATA),
                    failed = true,
                )
            every { strikeDataRepository.observeDataEvents() } returns flowOf(resultEvent)

            viewModel = createViewModel()

            viewModel.hasError.test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `updateData delegates to repository`() {
        viewModel = createViewModel()

        viewModel.updateData()

        verify { strikeDataRepository.updateData() }
    }

    @Test
    fun `goRealtime delegates to repository`() {
        every { strikeDataRepository.goRealtime() } returns true

        viewModel = createViewModel()

        val result = viewModel.goRealtime()

        assertThat(result).isTrue()
        verify { strikeDataRepository.goRealtime() }
    }

    @Test
    fun `setPosition delegates to repository`() {
        every { strikeDataRepository.setPosition(5) } returns true

        viewModel = createViewModel()

        val result = viewModel.setPosition(5)

        assertThat(result).isTrue()
        verify { strikeDataRepository.setPosition(5) }
    }

    @Test
    fun `start and stop delegate to repository`() {
        viewModel = createViewModel()

        viewModel.start()
        verify { strikeDataRepository.start() }

        viewModel.stop()
        verify { strikeDataRepository.stop() }
    }

    @Test
    fun `clearData request updates state`() {
        viewModel = createViewModel()

        viewModel.requestClearData()

        assertThat(viewModel.clearDataRequested.value).isTrue()

        viewModel.clearDataCompleted()

        assertThat(viewModel.clearDataRequested.value).isFalse()
    }

    @Test
    fun `onCleared stops data updates`() {
        viewModel = createViewModel()

        // Trigger onCleared via reflection since it's protected
        val onClearedMethod = MainViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)

        verify { strikeDataRepository.stop() }
    }

    private fun createViewModel(): MainViewModel =
        MainViewModel(
            strikeDataRepository,
            locationRepository,
            alertRepository,
        )
}
