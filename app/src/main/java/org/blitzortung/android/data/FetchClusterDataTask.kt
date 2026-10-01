/*

   Copyright 2026 Andreas Würl

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

import android.util.Log
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ClusterReceived

internal class FetchClusterDataTask(
    private val dataProvider: DataProvider,
    private val resultConsumer: (ClusterReceived) -> Unit,
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    fun cancel() {
        job.cancel()
    }

    fun execute(parameters: ClusterParameters) =
        launch {
            onPostExecute(doInBackground(parameters))
        }

    private suspend fun doInBackground(parameters: ClusterParameters): ClusterReceived? =
        withContext(Dispatchers.IO) {
            try {
                dataProvider.retrieveData { getClusters(parameters) }
            } catch (e: RuntimeException) {
                Log.e(Main.LOG_TAG, "error fetching cluster data", e)

                ClusterReceived(
                    failed = true,
                    referenceTime = System.currentTimeMillis(),
                    parameters = parameters,
                )
            }
        }

    private fun onPostExecute(result: ClusterReceived?) {
        if (result != null) {
            resultConsumer.invoke(result)
        }
    }
}
