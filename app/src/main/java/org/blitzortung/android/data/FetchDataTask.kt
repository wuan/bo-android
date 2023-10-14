package org.blitzortung.android.data

import android.util.Log
import kotlinx.coroutines.*
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ResultEvent
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KSuspendFunction1

internal open class FetchDataTask(
    private val dataMode: DataMode,
    private val dataProvider: DataProvider,
    private val resultConsumer: (ResultEvent) -> Unit,
    private val toast: KSuspendFunction1<Int, Unit>
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    fun cancel() {
        job.cancel()
    }

    fun execute(parameters: Parameters, history: History? = null, flags: Flags = Flags()) = launch {
        onPostExecute(doInBackground(parameters, history, flags))
    }

    protected open suspend fun doInBackground(parameters: Parameters, history: History?, flags: Flags): ResultEvent? =
        withContext(Dispatchers.IO) {
            try {
                dataProvider.retrieveData {
                    if (dataMode.raster) {
                        getStrikesGrid(parameters, history, flags)
                    } else {
                        getStrikes(parameters, history, flags)
                    }
                }
            } catch (e: RuntimeException) {
                Log.e(Main.LOG_TAG, "error fetching data", e)

                ResultEvent(
                    failed = true,
                    referenceTime = System.currentTimeMillis(),
                    parameters = parameters,
                    history = history,
                    flags = flags
                )
            }
        }

    open fun onPostExecute(result: ResultEvent?) {
        if (result != null) {
            resultConsumer.invoke(result)
        }
    }
}