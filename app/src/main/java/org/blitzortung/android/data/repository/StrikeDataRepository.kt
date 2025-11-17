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

package org.blitzortung.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.cache.CacheSize
import org.blitzortung.android.data.provider.result.DataEvent

/**
 * Repository layer that wraps MainDataHandler and provides Flow-based API
 * for reactive data access in ViewModels.
 */
@Singleton
class StrikeDataRepository
    @Inject
    constructor(
        private val mainDataHandler: MainDataHandler,
    ) {
        /**
         * Observe data events as a Flow.
         * This converts the callback-based ConsumerContainer to a reactive Flow.
         */
        fun observeDataEvents(): Flow<DataEvent> =
            callbackFlow {
                val consumer: (DataEvent) -> Unit = { event ->
                    trySend(event)
                }

                mainDataHandler.requestUpdates(consumer)

                awaitClose {
                    mainDataHandler.removeUpdates(consumer)
                }
            }

        /**
         * Request a data update from the server
         */
        fun updateData() {
            mainDataHandler.updateData()
        }

        /**
         * Get current parameters
         */
        fun getParameters(): Parameters = mainDataHandler.parameters

        /**
         * Get current interval duration
         */
        fun getIntervalDuration(): Int = mainDataHandler.intervalDuration

        /**
         * Check if currently in realtime mode
         */
        fun isRealtime(): Boolean = mainDataHandler.isRealtime

        /**
         * Switch to realtime mode
         * @return true if parameters changed
         */
        fun goRealtime(): Boolean = mainDataHandler.goRealtime()

        /**
         * Set history position
         * @return true if parameters changed
         */
        fun setPosition(position: Int): Boolean = mainDataHandler.setPosition(position)

        /**
         * Get number of history steps available
         */
        fun historySteps(): Int = mainDataHandler.historySteps()

        /**
         * Start periodic data updates
         */
        fun start() {
            mainDataHandler.start()
        }

        /**
         * Stop periodic data updates
         */
        fun stop() {
            mainDataHandler.stop()
        }

        /**
         * Restart data handler in realtime mode
         */
        fun restart() {
            mainDataHandler.restart()
        }

        /**
         * Start animation mode
         */
        fun startAnimation() {
            mainDataHandler.startAnimation()
        }

        /**
         * Toggle extended data mode
         */
        fun toggleExtendedMode() {
            mainDataHandler.toggleExtendedMode()
        }

        /**
         * Calculate total cache size
         */
        fun calculateTotalCacheSize(): CacheSize = mainDataHandler.calculateTotalCacheSize()
    }
