package org.blitzortung.android.data

import android.os.AsyncTask
import org.blitzortung.android.app.R
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ResultEvent
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.locks.Lock

internal open class FetchDataTask(
        private val dataMode: DataMode,
        private val dataProvider: DataProvider,
        private val lock: Lock,
        private val resultConsumer: (ResultEvent) -> Unit,
        private val toast: (Int) -> Unit
) : AsyncTask<TaskParameters, Int, ResultEvent>() {

    override fun onPostExecute(result: ResultEvent?) {
        if (result != null) {
            resultConsumer.invoke(result)
        }
    }

    override fun doInBackground(vararg taskParametersArray: TaskParameters): ResultEvent? {
        val taskParameters = taskParametersArray[0]
        val parameters = taskParameters.parameters
        val flags = taskParameters.flags

        if (lock.tryLock()) {
            try {
                var result = ResultEvent(referenceTime = System.currentTimeMillis(), parameters = parameters, flags = flags)

                dataProvider.retrieveData {
                    if (dataMode.raster) {
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

                handleErrorUserFeedback(e)

                return ResultEvent(failed = true, referenceTime = System.currentTimeMillis(), parameters = parameters, flags = flags)
            } finally {
                lock.unlock()
            }
        }
        return null
    }

    private fun handleErrorUserFeedback(e: RuntimeException) {
        val warningToastStringResource = when (e.cause) {
            is SocketTimeoutException ->
                R.string.timeout_warning

            is SocketException ->
                R.string.connection_warning

            else -> null
        }

        if (warningToastStringResource != null) {
            toast.invoke(warningToastStringResource)
        }
    }
}