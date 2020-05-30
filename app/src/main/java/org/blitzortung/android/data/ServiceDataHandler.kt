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
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.protocol.ConsumerContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceDataHandler @Inject constructor(
        private val context: Context,
        private val wakeLock: PowerManager.WakeLock,
        private val dataProviderFactory: DataProviderFactory,
        private val preferences: SharedPreferences
) : OnSharedPreferenceChangeListener {

    private var dataProvider: DataProvider? = null

    var parameters = Parameters()
        private set

    private lateinit var parametersController: ParametersController

    private val dataConsumerContainer = object : ConsumerContainer<DataEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "ServiceDataHandler: added first data consumer")
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "ServiceDataHandler: removed last data consumer")
        }
    }

    private var dataMode = DataMode()

    init {
        this.preferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(this.preferences, PreferenceKey.DATA_SOURCE, PreferenceKey.USERNAME, PreferenceKey.PASSWORD, PreferenceKey.RASTER_SIZE, PreferenceKey.COUNT_THRESHOLD, PreferenceKey.REGION, PreferenceKey.INTERVAL_DURATION, PreferenceKey.HISTORIC_TIMESTEP, PreferenceKey.QUERY_PERIOD)

        updateProviderSpecifics()
    }

    fun updateData() {
        dataProvider?.let { dataProvider ->
            FetchBackgroundDataTask(
                    dataMode,
                    dataProvider,
                    { sendEvent(it) },
                    ::toast,
                    wakeLock).execute(activeParameters)
        }
    }

    fun requestUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.removeConsumer(dataConsumer)
    }

    private val activeParameters: Parameters
        get() {
            return if (dataMode.raster) {
                parameters
            } else {
                var parameters = parameters
                if (!dataMode.region) {
                    parameters = parameters.copy(region = 0)
                }
                parameters.copy(rasterBaselength = 0, countThreshold = 0)
            }
        }

    private fun sendEvent(dataEvent: DataEvent) {
        if (dataEvent is ResultEvent && dataEvent.flags.storeResult) {
            dataConsumerContainer.storeAndBroadcast(dataEvent)
        } else {
            dataConsumerContainer.broadcast(dataEvent)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE, PreferenceKey.SERVICE_URL -> {
                val providerTypeString = sharedPreferences.get(PreferenceKey.DATA_SOURCE, DataProviderType.RPC.toString())
                val providerType = DataProviderType.valueOf(providerTypeString.toUpperCase())
                val dataProvider = dataProviderFactory.getDataProviderForType(providerType)
                this.dataProvider = dataProvider

                updateProviderSpecifics()

                if (providerTypeString == DataProviderType.HTTP.toString()) {
                    showBlitzortungProviderWarning()
                }
                updateData()
            }

            PreferenceKey.RASTER_SIZE -> {
                val rasterBaselength = sharedPreferences.get(key, DEFAULT_RASTER_BASELENGTH.toString())
                parameters = parameters.copy(rasterBaselength = if (rasterBaselength == AUTO_RASTER_BASELENGTH) DEFAULT_RASTER_BASELENGTH else rasterBaselength.toInt())
                updateData()
            }

            PreferenceKey.COUNT_THRESHOLD -> {
                val countThreshold = Integer.parseInt(sharedPreferences.get(key, "1"))
                parameters = parameters.copy(countThreshold = countThreshold)
                updateData()
            }

            PreferenceKey.INTERVAL_DURATION -> {
                parameters = parameters.copy(intervalDuration = Integer.parseInt(sharedPreferences.get(key, "60")))
                updateData()
            }

            PreferenceKey.HISTORIC_TIMESTEP -> {
                parametersController = ParametersController.withOffsetIncrement(
                        sharedPreferences.get(key, "30").toInt())
            }

            PreferenceKey.REGION -> {
                val region = Integer.parseInt(sharedPreferences.get(key, "1"))
                parameters = parameters.copy(region = region)
                updateData()
            }

            else -> {
            }
        }
    }

    private fun showBlitzortungProviderWarning() {
        Toast.makeText(context, R.string.provider_warning, Toast.LENGTH_LONG).show()
    }

    private fun updateProviderSpecifics() {

        val providerType = dataProvider!!.type

        dataMode = when (providerType) {
            DataProviderType.RPC -> DataMode(raster = true, region = false)

            DataProviderType.HTTP -> DataMode(raster = false, region = true)
        }
    }


    private suspend fun toast(stringResource: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(context, stringResource, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val WAKELOCK_TIMEOUT = 5000L
    }
}
