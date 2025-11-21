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

package org.blitzortung.android.data

import android.content.Context
import android.location.Location
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.LocalData
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.NoData
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.protocol.ConsumerContainer

@Singleton
class ServiceDataHandler
    @Inject
    constructor(
        private val context: Context,
        private val wakeLock: PowerManager.WakeLock,
        dataProviderFactory: DataProviderFactory,
        private val localData: LocalData,
    ) {
        private var location: Location? = null

        private var dataProvider: DataProvider? = null

        private val parameters =
            Parameters(
                region = LOCAL_REGION,
                gridSize = 5000,
                interval = TimeInterval.BACKGROUND,
                countThreshold = 0,
            )

        private val dataConsumerContainer =
            object : ConsumerContainer<DataEvent>(NoData) {
                override fun addedFirstConsumer() {
                    Log.d(Main.LOG_TAG, "ServiceDataHandler: added first data consumer")
                }

                override fun removedLastConsumer() {
                    Log.d(Main.LOG_TAG, "ServiceDataHandler: removed last data consumer")
                }
            }

        val locationEventConsumer: (LocationEvent) -> Unit = { locationEvent ->
            Log.v(Main.LOG_TAG, "AlertView received location ${locationEvent}")
            location = locationEvent.location()
        }

        private val dataMode = DataMode(grid = true, region = false)

        init {
            dataProvider = dataProviderFactory.getDataProviderForType(DataProviderType.RPC)
        }

        fun updateData() {
            dataProvider?.let { dataProvider ->
                Log.v(Main.LOG_TAG, "ServiceDataHandler.updateData() $activeParameters $wakeLock")
                val task =
                    FetchBackgroundDataTask(dataMode, dataProvider, { sendEvent(it) }, wakeLock)
                task
                    .execute(activeParameters)
            }
        }

        fun requestUpdates(dataConsumer: (DataEvent) -> Unit) {
            dataConsumerContainer.addConsumer(dataConsumer)
        }

        fun removeUpdates(dataConsumer: (DataEvent) -> Unit) {
            dataConsumerContainer.removeConsumer(dataConsumer)
        }

        private val activeParameters: Parameters
            get() = localData.updateParameters(parameters.copy(region = LOCAL_REGION), location)

        private fun sendEvent(dataEvent: DataEvent) {
            dataConsumerContainer.broadcast(dataEvent)
        }

        private suspend fun toast(stringResource: Int) =
            withContext(Dispatchers.Main) {
                Toast.makeText(context, stringResource, Toast.LENGTH_LONG).show()
            }

        companion object {
            const val WAKELOCK_TIMEOUT = 5000L
        }
    }
