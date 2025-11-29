package org.blitzortung.android.data

import android.util.Log
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataReceived

internal open class FetchDataTask(
    private val dataMode: DataMode,
    private val dataProvider: DataProvider,
    private val resultConsumer: (DataReceived) -> Unit,
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    fun cancel() {
        job.cancel()
    }

    fun execute(
        parameters: Parameters,
        history: History? = null,
        flags: Flags = Flags(),
    ) = launch {
        onPostExecute(doInBackground(parameters, history, flags))
    }

    protected open suspend fun doInBackground(
        parameters: Parameters,
        history: History?,
        flags: Flags,
    ): DataReceived? =
        withContext(Dispatchers.IO) {
            try {
                dataProvider.retrieveData {
                    if (dataMode.grid) {
                        getStrikesGrid(parameters, history, flags)
                    } else {
                        getStrikes(parameters, history, flags)
                    }
                }
            } catch (e: RuntimeException) {
                Log.e(Main.LOG_TAG, "error fetching data", e)

                DataReceived(
                    failed = true,
                    referenceTime = System.currentTimeMillis(),
                    parameters = parameters,
                    history = history,
                    flags = flags,
                )
            }
        }

    open fun onPostExecute(result: DataReceived?) {
        if (result != null) {
            resultConsumer.invoke(result)
        }
    }
}
