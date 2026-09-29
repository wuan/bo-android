/*

   Copyright 2026 Andreas Würl

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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.RequestStarted
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationRepository

/**
 * MainViewModel manages UI state and business logic for the Main Activity.
 * It coordinates [StrikeDataRepository], [LocationRepository] and [AlertRepository]
 * and exposes reactive state through StateFlow.
 */
@Suppress("TooManyFunctions")
class MainViewModel
    @Inject
    constructor(
        private val strikeDataRepository: StrikeDataRepository,
        private val locationRepository: LocationRepository,
        private val alertRepository: AlertRepository,
    ) : ViewModel() {
        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _hasError = MutableStateFlow(false)
        val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

        private val _clearDataRequested = MutableStateFlow(false)
        val clearDataRequested: StateFlow<Boolean> = _clearDataRequested.asStateFlow()

        private val _currentResult = MutableStateFlow<DataReceived?>(null)
        val currentResult: StateFlow<DataReceived?> = _currentResult.asStateFlow()

        val dataEvents: SharedFlow<DataEvent> =
            strikeDataRepository
                .observeDataEvents()
                .onEach(::reduceDataEvent)
                .shareIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(),
                    replay = 0,
                )

        val locationEvents: StateFlow<LocationEvent?> =
            locationRepository
                .observeLocationEvents()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        val alertEvents: StateFlow<Warning?> =
            alertRepository
                .observeAlertEvents()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        private fun reduceDataEvent(event: DataEvent) {
            when (event) {
                is RequestStarted -> {
                    _isLoading.value = true
                }

                is DataReceived -> {
                    _isLoading.value = false
                    _hasError.value = event.failed
                    if (!event.failed) {
                        _currentResult.value = event
                    }
                }

                else -> Unit
            }
        }

        fun updateData() = strikeDataRepository.updateData()

        fun getParameters(): Parameters = strikeDataRepository.getParameters()

        fun getIntervalDuration(): Int = strikeDataRepository.getIntervalDuration()

        fun isRealtime(): Boolean = strikeDataRepository.isRealtime()

        fun getMode(): Mode = strikeDataRepository.getMode()

        fun goRealtime(): Boolean = strikeDataRepository.goRealtime()

        fun setPosition(position: Int): Boolean = strikeDataRepository.setPosition(position)

        fun historySteps(): Int = strikeDataRepository.historySteps()

        fun start() = strikeDataRepository.start()

        fun stop() = strikeDataRepository.stop()

        fun restart() = strikeDataRepository.restart()

        fun startAnimation() = strikeDataRepository.startAnimation()

        fun toggleExtendedMode() = strikeDataRepository.toggleExtendedMode()

        fun enableBackgroundLocation() = locationRepository.enableBackgroundMode()

        fun disableBackgroundLocation() = locationRepository.disableBackgroundMode()

        fun requestClearData() {
            _clearDataRequested.value = true
        }

        fun clearDataCompleted() {
            _clearDataRequested.value = false
        }

        override fun onCleared() {
            super.onCleared()
            stop()
        }
    }
