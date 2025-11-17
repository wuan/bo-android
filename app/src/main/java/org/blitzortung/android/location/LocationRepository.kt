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

package org.blitzortung.android.location

import android.location.Location
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository layer that wraps LocationHandler and provides Flow-based API
 * for reactive location access in ViewModels.
 */
@Singleton
class LocationRepository
    @Inject
    constructor(
        private val locationHandler: LocationHandler,
    ) {
        /**
         * Observe location events as a Flow
         */
        fun observeLocationEvents(): Flow<LocationEvent> =
            callbackFlow {
                val consumer: (LocationEvent) -> Unit = { event ->
                    trySend(event)
                }

                locationHandler.requestUpdates(consumer)

                awaitClose {
                    locationHandler.removeUpdates(consumer)
                }
            }

        /**
         * Get the current location if available
         */
        fun getCurrentLocation(): Location? = locationHandler.location

        /**
         * Enable background mode for location updates
         */
        fun enableBackgroundMode() {
            locationHandler.enableBackgroundMode()
        }

        /**
         * Disable background mode for location updates
         */
        fun disableBackgroundMode() {
            locationHandler.disableBackgroundMode()
        }
    }
