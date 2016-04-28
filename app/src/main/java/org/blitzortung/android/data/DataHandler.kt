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

import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.PowerManager
import android.util.Log
import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.*
import org.blitzortung.android.protocol.ConsumerContainer
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class DataHandler @JvmOverloads constructor(private val wakeLock: PowerManager.WakeLock, private val agentSuffix: String,
                                            private val dataProviderFactory: DataProviderFactory = DataProviderFactory()) : OnSharedPreferenceChangeListener {

    private val sharedPreferences = BOApplication.sharedPreferences

    private val lock = ReentrantLock()
    private var dataProvider: DataProvider? = null
    var parameters = Parameters()
        private set

    private var parametersController: ParametersController? = null

    private val dataConsumerContainer = object : ConsumerContainer<DataEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "DataHandler: added first data consumer")
            AppService.instance?.configureServiceMode()
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "DataHandler: removed last data consumer")
            AppService.instance?.configureServiceMode()
        }
    }

    private val internalDataConsumer: (DataEvent) -> Unit = {
        dataEvent ->
            if(dataEvent is ResultEvent)
                dataConsumerContainer.storeAndBroadcast(dataEvent)
            else
                dataConsumerContainer.broadcast(dataEvent)
    }

    private val internalDataConsumerContainer = ConsumerContainer<DataEvent>()

    private var dataMode = DataMode()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferencesChanged(sharedPreferences, PreferenceKey.DATA_SOURCE, PreferenceKey.USERNAME, PreferenceKey.PASSWORD, PreferenceKey.RASTER_SIZE, PreferenceKey.COUNT_THRESHOLD, PreferenceKey.REGION, PreferenceKey.INTERVAL_DURATION, PreferenceKey.HISTORIC_TIMESTEP)

        updateProviderSpecifics()

        internalDataConsumerContainer.addConsumer(internalDataConsumer)
    }

    fun updateDataInBackground() {
        FetchBackgroundDataTask(wakeLock).execute(TaskParameters(parameters = parameters, updateParticipants = false))
    }

    fun requestInternalUpdates(dataConsumer: (DataEvent) -> Unit) {
        internalDataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeInternalUpdates(dataConsumer: (DataEvent) -> Unit) {
        internalDataConsumerContainer.removeConsumer(dataConsumer)
    }

    fun requestUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.removeConsumer(dataConsumer)
    }

    val hasConsumers: Boolean
        get() = dataConsumerContainer.isEmpty

    @JvmOverloads fun updateData(updateTargets: Set<DataChannel> = DEFAULT_DATA_CHANNELS) {

        sendEvent(REQUEST_STARTED_EVENT)

        var updateParticipants = false
        if (updateTargets.contains(DataChannel.PARTICIPANTS)) {
            if (dataProvider!!.type == DataProviderType.HTTP || !dataMode.raster) {
                updateParticipants = true
            }
        }
        Log.d(Main.LOG_TAG, "DataHandler.updateData() $activeParameters")
        FetchDataTask().execute(TaskParameters(parameters = activeParameters, updateParticipants = updateParticipants))
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
        if(dataEvent is StatusEvent || dataEvent is RequestStartedEvent)
            internalDataConsumerContainer.broadcast(dataEvent)
        else {
            internalDataConsumerContainer.storeAndBroadcast(dataEvent)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE, PreferenceKey.SERVICE_URL -> {
                val providerTypeString = sharedPreferences.get(PreferenceKey.DATA_SOURCE, DataProviderType.RPC.toString())
                val providerType = DataProviderType.valueOf(providerTypeString.toUpperCase())
                val dataProvider = dataProviderFactory.getDataProviderForType(providerType, sharedPreferences, agentSuffix)
                this.dataProvider?.run { sharedPreferences.unregisterOnSharedPreferenceChangeListener(this@DataHandler.dataProvider) }
                this.dataProvider = dataProvider

                updateProviderSpecifics()

                notifyDataReset()
            }

            PreferenceKey.RASTER_SIZE -> {
                val rasterBaselength = Integer.parseInt(sharedPreferences.get(key, "10000"))
                parameters = parameters.copy(rasterBaselength = rasterBaselength);
                notifyDataReset()
            }

            PreferenceKey.COUNT_THRESHOLD -> {
                val countThreshold = Integer.parseInt(sharedPreferences.get(key, "1"))
                parameters = parameters.copy(countThreshold = countThreshold);
                notifyDataReset()
            }

            PreferenceKey.INTERVAL_DURATION -> {
                parameters = parameters.copy(intervalDuration = Integer.parseInt(sharedPreferences.get(key, "60")));
                dataProvider!!.reset()
                notifyDataReset()
            }

            PreferenceKey.HISTORIC_TIMESTEP -> parametersController = ParametersController.withOffsetIncrement(
                    Integer.parseInt(sharedPreferences.get(key, "30")))

            PreferenceKey.REGION -> {
                val region = Integer.parseInt(sharedPreferences.get(key, "1"))
                parameters = parameters.copy(region = region);
                dataProvider!!.reset()
                notifyDataReset()
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

    private fun notifyDataReset() {
        sendEvent(CLEAR_DATA_EVENT)
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
        return updateParameters({ parametersController!!.ffwdInterval(it) })
    }

    fun rewInterval(): Boolean {
        return updateParameters({ parametersController!!.rewInterval(it) })
    }

    fun goRealtime(): Boolean {
        return updateParameters({ parametersController!!.goRealtime(it) })
    }

    fun updateParameters(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = parameters
        parameters = updater.invoke(parameters)
        return parameters != oldParameters
    }

    val isRealtime: Boolean
        get() = parameters.isRealtime()

    val isCapableOfHistoricalData: Boolean
        get() = dataProvider!!.isCapableOfHistoricalData

    private open inner class FetchDataTask : AsyncTask<TaskParameters, Int, ResultEvent>() {

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: ResultEvent?) {
            if (result != null) {
                sendEvent(result)
            }
        }

        override fun doInBackground(vararg taskParametersArray: TaskParameters): ResultEvent? {
            val taskParameters = taskParametersArray[0]
            val parameters = taskParameters.parameters

            if (lock.tryLock()) {
                try {
                    var result = ResultEvent(referenceTime = System.currentTimeMillis(), parameters = parameters)

                    dataProvider!!.retrieveData() {
                        if (parameters.isRaster()) {
                            result = getStrikesGrid(parameters, result)
                        } else {
                            result = getStrikes(parameters, result)
                        }

                        /*if (taskParameters.updateParticipants) {
                            result.copy(stations = getStations(parameters.region))
                        }*/
                    }

                    /*if (taskParameters.updateParticipants) {
                        result.copy(stations = dataProvider!!.getStations(parameters.region))
                    }*/

                    return result
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    return ResultEvent(failed = true)
                } finally {
                    lock.unlock()
                }
            }
            return null
        }
    }

    private inner class FetchBackgroundDataTask(private val wakeLock: PowerManager.WakeLock) : FetchDataTask() {

        override fun onPostExecute(result: ResultEvent?) {
            super.onPostExecute(result)
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.v(Main.LOG_TAG, "FetchBackgroundDataTask released wakelock " + wakeLock)
                } catch (e: RuntimeException) {
                    Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock failed ", e)
                }

            } else {
                Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock not held ")
            }
        }

        override fun doInBackground(vararg params: TaskParameters): ResultEvent? {
            wakeLock.acquire()
            Log.v(Main.LOG_TAG, "FetchBackgroundDataTask aquire wakelock " + wakeLock)

            val taskParameters = params[0]
            val updatedParameters = taskParameters.parameters.copy(intervalDuration = 10)
            val updatedParams = arrayOf(taskParameters.copy(parameters = updatedParameters))

            return super.doInBackground(*updatedParams)
        }
    }

    fun broadcastEvent(event: DataEvent) {
        internalDataConsumerContainer.broadcast(event)
    }

    companion object {
        val REQUEST_STARTED_EVENT = RequestStartedEvent()
        val CLEAR_DATA_EVENT = ClearDataEvent()
        val DEFAULT_DATA_CHANNELS: MutableSet<DataChannel> = HashSet()

        init {
            DEFAULT_DATA_CHANNELS.add(DataChannel.STRIKES)
        }
    }
}
