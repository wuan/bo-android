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

package org.blitzortung.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.cache.CacheSize
import org.blitzortung.android.data.provider.result.DataEvent

/**
 * Repository layer that wraps [MainDataHandler] and provides a Flow-based API
 * for reactive data access in ViewModels.
 */
@Singleton
@Suppress("TooManyFunctions")
class StrikeDataRepository
    @Inject
    constructor(
        private val mainDataHandler: MainDataHandler,
    ) {
        /**
         * Observe data events as a Flow. Each collector registers a single consumer
         * with [MainDataHandler] and releases it when collection ends.
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

        fun updateData() = mainDataHandler.updateData()

        fun getParameters(): Parameters = mainDataHandler.parameters

        fun getIntervalDuration(): Int = mainDataHandler.intervalDuration

        fun isRealtime(): Boolean = mainDataHandler.isRealtime

        fun getMode(): Mode = mainDataHandler.mode

        fun goRealtime(): Boolean = mainDataHandler.goRealtime()

        fun setPosition(position: Int): Boolean = mainDataHandler.setPosition(position)

        fun historySteps(): Int = mainDataHandler.historySteps()

        fun start() = mainDataHandler.start()

        fun stop() = mainDataHandler.stop()

        fun restart() = mainDataHandler.restart()

        fun startAnimation() = mainDataHandler.startAnimation()

        fun toggleExtendedMode() = mainDataHandler.toggleExtendedMode()

        fun calculateTotalCacheSize(): CacheSize = mainDataHandler.calculateTotalCacheSize()
    }
