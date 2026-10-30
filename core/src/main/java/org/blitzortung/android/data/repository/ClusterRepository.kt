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
import org.blitzortung.android.data.provider.result.ClusterEvent

/**
 * Repository layer that wraps [MainDataHandler]'s independent cluster stream and
 * provides a Flow-based API. Kept separate from [StrikeDataRepository] so cluster
 * fetching can be toggled without touching strike data.
 */
@Singleton
class ClusterRepository
    @Inject
    constructor(
        private val mainDataHandler: MainDataHandler,
    ) {
        fun observeClusterEvents(): Flow<ClusterEvent> =
            callbackFlow {
                val consumer: (ClusterEvent) -> Unit = { event ->
                    trySend(event)
                }

                mainDataHandler.requestClusterUpdates(consumer)

                awaitClose {
                    mainDataHandler.removeClusterUpdates(consumer)
                }
            }
    }
