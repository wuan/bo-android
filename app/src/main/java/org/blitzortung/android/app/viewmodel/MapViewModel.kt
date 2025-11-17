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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

/**
 * MapViewModel manages map state and configuration.
 * Handles zoom level, center position, and map type preferences.
 */
class MapViewModel
    @Inject
    constructor() : ViewModel() {
        // Map state
        private val _zoomLevel = MutableStateFlow(3.0)
        val zoomLevel: StateFlow<Double> = _zoomLevel.asStateFlow()

        private val _centerPosition = MutableStateFlow<GeoPoint?>(null)
        val centerPosition: StateFlow<GeoPoint?> = _centerPosition.asStateFlow()

        private val _mapType = MutableStateFlow("OpenStreetMap")
        val mapType: StateFlow<String> = _mapType.asStateFlow()

        private val _isMapReady = MutableStateFlow(false)
        val isMapReady: StateFlow<Boolean> = _isMapReady.asStateFlow()

        /**
         * Update zoom level
         */
        fun updateZoomLevel(zoom: Double) {
            _zoomLevel.value = zoom
        }

        /**
         * Update center position
         */
        fun updateCenterPosition(position: GeoPoint) {
            _centerPosition.value = position
        }

        /**
         * Update map type
         */
        fun updateMapType(type: String) {
            _mapType.value = type
        }

        /**
         * Mark map as ready
         */
        fun setMapReady(ready: Boolean) {
            _isMapReady.value = ready
        }

        /**
         * Save current map state for restoration
         */
        fun saveMapState(
            zoom: Double,
            center: GeoPoint,
        ) {
            _zoomLevel.value = zoom
            _centerPosition.value = center
        }
    }
