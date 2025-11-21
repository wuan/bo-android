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

package org.blitzortung.android.alert

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.handler.AlertHandler

/**
 * Repository layer that wraps AlertHandler and provides Flow-based API
 * for reactive alert access in ViewModels.
 */
@Singleton
class AlertRepository
    @Inject
    constructor(
        private val alertHandler: AlertHandler,
    ) {
        /**
         * Observe alert events as a Flow
         */
        fun observeAlertEvents(): Flow<AlertEvent> =
            callbackFlow {
                val consumer: (AlertEvent) -> Unit = { event ->
                    trySend(event)
                }

                alertHandler.requestUpdates(consumer)

                awaitClose {
                    alertHandler.removeUpdates(consumer)
                }
            }

        /**
         * Get current alert parameters
         */
        fun getAlertParameters(): AlertParameters = alertHandler.alertParameters
    }
