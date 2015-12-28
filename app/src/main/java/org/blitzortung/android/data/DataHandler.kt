package org.blitzortung.android.data

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageInfo
import android.os.AsyncTask
import android.os.PowerManager
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.ClearDataEvent
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStartedEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class DataHandler @JvmOverloads constructor(private val wakeLock: PowerManager.WakeLock, sharedPreferences: SharedPreferences, private val pInfo: PackageInfo,
        private val dataProviderFactory: DataProviderFactory = DataProviderFactory()) : OnSharedPreferenceChangeListener {

    private val lock = ReentrantLock()
    private var dataProvider: DataProvider? = null
    private var username: String? = null
    private var password: String? = null
    var parameters = Parameters()
        private set
    private var parametersController: ParametersController? = null
    private var dataEventConsumer: ((DataEvent) -> Unit)? = null
    private var preferencesRasterBaselength: Int = 0
    private var preferencesRegion: Int = 0

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.DATA_SOURCE)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.USERNAME)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.PASSWORD)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.RASTER_SIZE)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.COUNT_THRESHOLD)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.REGION)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.INTERVAL_DURATION)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.HISTORIC_TIMESTEP)

        updateProviderSpecifics()
    }

    fun updateDatainBackground() {
        FetchBackgroundDataTask(wakeLock).execute(TaskParameters(parameters = parameters, updateParticipants = false))
    }

    @JvmOverloads fun updateData(updateTargets: Set<DataChannel> = DEFAULT_DATA_CHANNELS) {

        sendEvent(REQUEST_STARTED_EVENT)

        var updateParticipants = false
        if (updateTargets.contains(DataChannel.PARTICIPANTS)) {
            if (dataProvider!!.type == DataProviderType.HTTP || parameters.rasterBaselength == 0) {
                updateParticipants = true
            }
        }

        FetchDataTask().execute(TaskParameters(parameters = parameters, updateParticipants = updateParticipants))
    }

    private fun sendEvent(dataEvent: DataEvent) {
        dataEventConsumer?.let { it.invoke(dataEvent) }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE, PreferenceKey.SERVICE_URL -> {
                val providerTypeString = sharedPreferences.getString(PreferenceKey.DATA_SOURCE.toString(), DataProviderType.RPC.toString())
                val providerType = DataProviderType.valueOf(providerTypeString.toUpperCase())
                val dataProvider = dataProviderFactory.getDataProviderForType(providerType, sharedPreferences)
                dataProvider.setPackageInfo(pInfo)
                this.dataProvider = dataProvider

                updateProviderSpecifics()

                notifyDataReset()
            }

            PreferenceKey.USERNAME -> username = sharedPreferences.getString(key.toString(), "")

            PreferenceKey.PASSWORD -> password = sharedPreferences.getString(key.toString(), "")

            PreferenceKey.RASTER_SIZE -> {
                preferencesRasterBaselength = Integer.parseInt(sharedPreferences.getString(key.toString(), "10000"))
                parameters = parameters.copy(rasterBaselength = preferencesRasterBaselength);
                notifyDataReset()
            }

            PreferenceKey.COUNT_THRESHOLD -> {
                val countThreshold = Integer.parseInt(sharedPreferences.getString(key.toString(), "1"))
                parameters = parameters.copy(countThreshold = countThreshold);
                notifyDataReset()
            }

            PreferenceKey.INTERVAL_DURATION -> {
                parameters = parameters.copy(intervalDuration = Integer.parseInt(sharedPreferences.getString(key.toString(), "60")));
                dataProvider!!.reset()
                notifyDataReset()
            }

            PreferenceKey.HISTORIC_TIMESTEP -> parametersController = ParametersController.withOffsetIncrement(
                    Integer.parseInt(sharedPreferences.getString(key.toString(), "30")))

            PreferenceKey.REGION -> {
                preferencesRegion = Integer.parseInt(sharedPreferences.getString(key.toString(), "1"))
                parameters = parameters.copy(region = preferencesRegion);
                dataProvider!!.reset()
                notifyDataReset()
            }
        }

        if (dataProvider != null) {
            //dataProvider!!.setCredentials(username!!, password!!)
        }
    }

    private fun updateProviderSpecifics() {

        val providerType = dataProvider!!.type

        when (providerType) {
            DataProviderType.RPC -> enableRasterMode()

            DataProviderType.HTTP -> disableRasterMode()
        }
    }

    private fun notifyDataReset() {
        sendEvent(CLEAR_DATA_EVENT)
    }

    fun toggleExtendedMode() {
        if (parameters.rasterBaselength > 0) {
            disableRasterMode()
        } else {
            enableRasterMode()
        }
        if (!isRealtime) {
            val dataChannels = HashSet<DataChannel>()
            dataChannels.add(DataChannel.STRIKES)
            updateData(dataChannels)
        }
    }

    fun disableRasterMode() {
        parameters = parameters.copy(rasterBaselength = 0)
    }

    fun enableRasterMode() {
        parameters = parameters.copy(region = preferencesRegion, rasterBaselength = preferencesRasterBaselength)
    }

    fun setDataConsumer(consumer: (DataEvent) -> Unit) {
        this.dataEventConsumer = consumer
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

                    dataProvider!!.setUp()
                    dataProvider!!.setCredentials(username!!, password!!)

                    if (parameters.isRaster()) {
                        result = dataProvider!!.getStrikesGrid(parameters, result)
                    } else {
                        result = dataProvider!!.getStrikes(parameters, result)
                    }

                    /*if (taskParameters.updateParticipants) {
                        result.copy(stations = dataProvider!!.getStations(parameters.region))
                    }*/

                    dataProvider!!.shutDown()

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
            return super.doInBackground(*params)
        }
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
