package org.blitzortung.android.data

import android.util.Log
import kotlinx.coroutines.*
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ResultEvent
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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

    fun execute(parameters: Parameters, flags: Flags = Flags()) = launch {
        onPostExecute(doInBackground(parameters, flags))
    }

    protected open suspend fun doInBackground(parameters: Parameters, flags: Flags): ResultEvent? = withContext(Dispatchers.IO) {
        try {
            var result = ResultEvent(referenceTime = System.currentTimeMillis(), parameters = parameters, flags = flags)

            dataProvider.retrieveData {
                result = if (dataMode.raster) {
                    getStrikesGrid(parameters, result)
                } else {
                    getStrikes(parameters, result)
                }
            }

            result
        } catch (e: RuntimeException) {
            e.printStackTrace()

            handleErrorUserFeedback(e)

            ResultEvent(failed = true, referenceTime = System.currentTimeMillis(), parameters = parameters, flags = flags)
        }
    }

    private suspend fun handleErrorUserFeedback(e: RuntimeException) {
        val warningToastStringResource = when (e.cause) {
            is SocketTimeoutException ->
                R.string.timeout_warning

            is SocketException, is UnknownHostException->
                R.string.connection_warning

            else -> null
        }

        if (warningToastStringResource != null) {
            withContext(Dispatchers.Main) {
                toast.invoke(warningToastStringResource)
            }
        }
    }

    open fun onPostExecute(result: ResultEvent?) {
        if (result != null) {
            resultConsumer.invoke(result)
        }
    }
}