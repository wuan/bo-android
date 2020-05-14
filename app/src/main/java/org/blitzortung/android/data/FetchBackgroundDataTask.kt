package org.blitzortung.android.data

import android.annotation.SuppressLint
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.util.isAtLeast
import java.util.concurrent.locks.Lock
import kotlin.reflect.KSuspendFunction1

internal class FetchBackgroundDataTask(
        dataMode: DataMode,
        dataProvider: DataProvider,
        lock: Lock,
        resultConsumer: (ResultEvent) -> Unit,
        toast: KSuspendFunction1<Int, Unit>,
        private val wakeLock: PowerManager.WakeLock
) : FetchDataTask(dataMode, dataProvider, lock, resultConsumer, toast) {

    override fun onPostExecute(result: ResultEvent?) {
        super.onPostExecute(result)
        if (wakeLock.isHeld) {
            try {
                wakeLock.release()
                Log.v(Main.LOG_TAG, "FetchBackgroundDataTask released wakelock $wakeLock")
            } catch (e: RuntimeException) {
                Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock failed ", e)
            }

        } else {
            Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock not held")
        }
    }

    @SuppressLint("WakelockTimeout")
    override suspend fun doInBackground(parameters: Parameters, flags: Flags): ResultEvent? = withContext(Dispatchers.IO) {
        if (isAtLeast(Build.VERSION_CODES.N)) {
            wakeLock.acquire(ServiceDataHandler.WAKELOCK_TIMEOUT)
        } else {
            wakeLock.acquire()
        }

        Log.v(Main.LOG_TAG, "FetchBackgroundDataTask aquire wakelock $wakeLock")

        val updatedParameters = parameters.copy(intervalDuration = 10, intervalOffset = 0)
        val updatedFlags = flags.copy(storeResult = false)

        super.doInBackground(updatedParameters, updatedFlags)
    }
}