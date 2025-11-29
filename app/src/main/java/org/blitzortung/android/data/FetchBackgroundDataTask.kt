package org.blitzortung.android.data

import android.annotation.SuppressLint
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.util.isAtLeast

internal class FetchBackgroundDataTask(
    dataMode: DataMode,
    dataProvider: DataProvider,
    resultConsumer: (DataReceived) -> Unit,
    private val wakeLock: PowerManager.WakeLock,
) : FetchDataTask(dataMode, dataProvider, resultConsumer) {
    override fun onPostExecute(result: DataReceived?) {
        super.onPostExecute(result)
        if (wakeLock.isHeld) {
            try {
                wakeLock.release()
                if (wakeLock.isHeld) {
                    Log.v(Main.LOG_TAG, "FetchBackgroundDataTask.postExecute() released wakelock $wakeLock")
                }
            } catch (e: RuntimeException) {
                Log.e(Main.LOG_TAG, "FetchBackgroundDataTask.postExecute() release wakelock failed ", e)
            }
        } else {
            Log.e(Main.LOG_TAG, "FetchBackgroundDataTask.postExecute() wakelock not held")
        }
    }

    @SuppressLint("WakelockTimeout")
    override suspend fun doInBackground(
        parameters: Parameters,
        history: History?,
        flags: Flags,
    ): DataReceived? =
        withContext(Dispatchers.IO) {
            if (isAtLeast(Build.VERSION_CODES.N)) {
                wakeLock.acquire(ServiceDataHandler.WAKELOCK_TIMEOUT)
            } else {
                wakeLock.acquire()
            }
            if (wakeLock.isHeld) {
                // Log.v(Main.LOG_TAG, "FetchBackgroundDataTask aquired wakelock $wakeLock")

                val updatedParameters =
                    parameters.copy(
                        interval = TimeInterval.BACKGROUND,
                        countThreshold = 0,
                        gridSize = 5000,
                    )
                val updatedFlags = flags.copy(storeResult = false)

                super.doInBackground(updatedParameters, history, updatedFlags)
            } else {
                Log.e(Main.LOG_TAG, "could not acquire wakelock")
                null // ignore
            }
        }
}
