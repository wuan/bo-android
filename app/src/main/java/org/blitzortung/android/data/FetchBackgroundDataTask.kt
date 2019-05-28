package org.blitzortung.android.data

import android.os.Build
import android.os.PowerManager
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.util.isAtLeast
import java.util.concurrent.locks.Lock

internal class FetchBackgroundDataTask(
        dataMode: DataMode,
        dataProvider: DataProvider,
        lock: Lock,
        resultConsumer: (ResultEvent) -> Unit,
        toast: (Int) -> Unit,
        private val wakeLock: PowerManager.WakeLock
) : FetchDataTask(dataMode, dataProvider, lock, resultConsumer, toast) {

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
            Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock not held")
        }
    }

    override fun doInBackground(vararg taskParametersArray: TaskParameters): ResultEvent? {
        if (isAtLeast(Build.VERSION_CODES.N)) {
            wakeLock.acquire(ServiceDataHandler.WAKELOCK_TIMEOUT)
        } else {
            wakeLock.acquire()
        }

        Log.v(Main.LOG_TAG, "FetchBackgroundDataTask aquire wakelock " + wakeLock)

        val taskParameters = taskParametersArray[0]
        val updatedParameters = taskParameters.parameters.copy(intervalDuration = 10, intervalOffset = 0)
        val updatedParams = arrayOf(taskParameters.copy(parameters = updatedParameters, flags = Flags(storeResult = false)))

        return super.doInBackground(*updatedParams)
    }
}