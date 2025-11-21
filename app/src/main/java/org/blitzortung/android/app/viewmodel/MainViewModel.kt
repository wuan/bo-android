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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationRepository

/**
 * MainViewModel manages UI state and business logic for the Main Activity.
 * This ViewModel provides reactive data streams using StateFlow and coordinates
 * between the StrikeDataRepository, LocationRepository, and AlertRepository.
 */
class MainViewModel
    @Inject
    constructor(
        private val strikeDataRepository: StrikeDataRepository,
        private val locationRepository: LocationRepository,
        private val alertRepository: AlertRepository,
    ) : ViewModel() {
        // UI State
        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _hasError = MutableStateFlow(false)
        val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

        private val _clearDataRequested = MutableStateFlow(false)
        val clearDataRequested: StateFlow<Boolean> = _clearDataRequested.asStateFlow()

        // Current result state
        private val _currentResult = MutableStateFlow<ResultEvent?>(null)
        val currentResult: StateFlow<ResultEvent?> = _currentResult.asStateFlow()

        // Data events flow
        val dataEvents: StateFlow<DataEvent?> =
            strikeDataRepository
                .observeDataEvents()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        // Location events flow
        val locationEvents: StateFlow<LocationEvent?> =
            locationRepository
                .observeLocationEvents()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        // Alert events flow
        val alertEvents: StateFlow<AlertEvent?> =
            alertRepository
                .observeAlertEvents()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        init {
            observeDataEvents()
        }

        private fun observeDataEvents() {
            viewModelScope.launch {
                dataEvents.collect { event ->
                    when (event) {
                        is org.blitzortung.android.data.provider.result.RequestStartedEvent -> {
                            _isLoading.value = true
                        }
                        is ResultEvent -> {
                            _isLoading.value = false
                            _hasError.value = event.failed
                            if (!event.failed) {
                                _currentResult.value = event
                            }
                        }
                        else -> {
                            _isLoading.value = false
                        }
                    }
                }
            }
        }

        // Strike Data Operations
        fun updateData() {
            strikeDataRepository.updateData()
        }

        fun getParameters(): Parameters = strikeDataRepository.getParameters()

        fun getIntervalDuration(): Int = strikeDataRepository.getIntervalDuration()

        fun isRealtime(): Boolean = strikeDataRepository.isRealtime()

        fun goRealtime(): Boolean = strikeDataRepository.goRealtime()

        fun setPosition(position: Int): Boolean = strikeDataRepository.setPosition(position)

        fun historySteps(): Int = strikeDataRepository.historySteps()

        fun start() {
            strikeDataRepository.start()
        }

        fun stop() {
            strikeDataRepository.stop()
        }

        fun restart() {
            strikeDataRepository.restart()
        }

        fun startAnimation() {
            strikeDataRepository.startAnimation()
        }

        fun toggleExtendedMode() {
            strikeDataRepository.toggleExtendedMode()
        }

        // Location Operations
        fun enableBackgroundLocation() {
            locationRepository.enableBackgroundMode()
        }

        fun disableBackgroundLocation() {
            locationRepository.disableBackgroundMode()
        }

        // UI State Operations
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
