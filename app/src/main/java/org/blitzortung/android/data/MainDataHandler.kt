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
import android.os.Handler
import android.util.Log
import android.widget.Toast
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStartedEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.protocol.ConsumerContainer
import org.blitzortung.android.util.Period
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainDataHandler @Inject constructor(
        private val context: Context,
        private val dataProviderFactory: DataProviderFactory,
        private val preferences: SharedPreferences,
        private val handler: Handler,
        private val updatePeriod: Period
) : OnSharedPreferenceChangeListener, Runnable {


    private var updatesEnabled = false

    private var period: Int = 0

    private val lock = ReentrantLock()

    private var dataProvider: DataProvider? = null

    var parameters = Parameters()
        private set

    private lateinit var parametersController: ParametersController

    private val dataConsumerContainer = object : ConsumerContainer<DataEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "MainDataHandler: added first data consumer")
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "MainDataHandler: removed last data consumer")
        }
    }

    private var dataMode = DataMode()

    init {
        this.preferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(this.preferences, PreferenceKey.DATA_SOURCE, PreferenceKey.USERNAME, PreferenceKey.PASSWORD, PreferenceKey.RASTER_SIZE, PreferenceKey.COUNT_THRESHOLD, PreferenceKey.REGION, PreferenceKey.INTERVAL_DURATION, PreferenceKey.HISTORIC_TIMESTEP, PreferenceKey.QUERY_PERIOD)

        updateProviderSpecifics()

        updatesEnabled = true
    }

    fun requestUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.removeConsumer(dataConsumer)
    }

    val hasConsumers: Boolean
        get() = dataConsumerContainer.isEmpty

    fun updateData(updateTargets: Set<DataChannel> = DEFAULT_DATA_CHANNELS) {
        if (updatesEnabled) {
            sendEvent(REQUEST_STARTED_EVENT)

            var updateParticipants = false
            if (updateTargets.contains(DataChannel.PARTICIPANTS)) {
                if (dataProvider!!.type == DataProviderType.HTTP || !dataMode.raster) {
                    updateParticipants = true
                }
            }
            Log.d(Main.LOG_TAG, "MainDataHandler.updateData() $activeParameters")
            FetchDataTask(
                    dataMode,
                    dataProvider!!,
                    lock,
                    { sendEvent(it) },
                    ::toast
            ).execute(TaskParameters(parameters = activeParameters, updateParticipants = updateParticipants))
        }
    }

    val activeParameters: Parameters
        get() {
            if (dataMode.raster) {
                return parameters
            } else {
                var parameters = parameters
                if (!dataMode.region) {
                    parameters = parameters.copy(region = 0)
                }
                return parameters.copy(rasterBaselength = 0, countThreshold = 0)
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
                val rasterBaselength = Integer.parseInt(sharedPreferences.get(key, "10000"))
                parameters = parameters.copy(rasterBaselength = rasterBaselength)
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


            PreferenceKey.QUERY_PERIOD -> {
                period = Integer.parseInt(sharedPreferences.get(key, "60"))
                Log.v(Main.LOG_TAG, "MainDataHandler query $period")
            }

            else -> {
            }
        }
    }

    private fun showBlitzortungProviderWarning() {
        doAsync {
            uiThread {
                Toast.makeText(context, R.string.provider_warning, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateProviderSpecifics() {

        val providerType = dataProvider!!.type

        dataMode = when (providerType) {
            DataProviderType.RPC -> DataMode(raster = true, region = false)

            DataProviderType.HTTP -> DataMode(raster = false, region = true)
        }
    }

    fun toggleExtendedMode() {
        dataMode = dataMode.copy(raster = dataMode.raster.xor(true))

        if (!isRealtime) {
            val dataChannels = HashSet<DataChannel>()
            dataChannels.add(DataChannel.STRIKES)
            updateData(dataChannels)
        }
    }

    val intervalDuration: Int
        get() = parameters.intervalDuration

    fun ffwdInterval(): Boolean {
        return updateParameters { parametersController.ffwdInterval(it) }
    }

    fun rewInterval(): Boolean {
        return updateParameters { parametersController.rewInterval(it) }
    }

    fun goRealtime(): Boolean {
        return updateParameters { parametersController.goRealtime(it) }
    }

    fun updateParameters(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = parameters
        parameters = updater.invoke(parameters)
        return parameters != oldParameters
    }

    val isRealtime: Boolean
        get() = parameters.isRealtime()

    private fun toast(stringResource: Int) {
        doAsync {
            uiThread {
                Toast.makeText(context, stringResource, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun broadcastEvent(event: DataEvent) {
        dataConsumerContainer.broadcast(event)
    }

    override fun run() {
        val currentTime = Period.currentTime
        val updateTargets = HashSet<DataChannel>()

        if (updatePeriod.shouldUpdate(currentTime, period)) {
            updateTargets.add(DataChannel.STRIKES)
        }

        if (!updateTargets.isEmpty()) {
            updateData(updateTargets)
        }

        if (parameters.isRealtime()) {
            val statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period
            broadcastEvent(StatusEvent(statusString))
            // Schedule the next update
            handler.postDelayed(this, 1000)
        }
    }

    fun start() {
        if (isRealtime) {
            handler.post(this)
        }
    }

    fun restart() {
        updatePeriod.restart()
        start()
    }

    fun stop() {
        handler.removeCallbacks(this)
    }

    companion object {
        val REQUEST_STARTED_EVENT = RequestStartedEvent()
        val DEFAULT_DATA_CHANNELS = setOf(DataChannel.STRIKES)
    }
}
