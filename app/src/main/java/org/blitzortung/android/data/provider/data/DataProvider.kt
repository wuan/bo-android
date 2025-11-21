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

package org.blitzortung.android.data.provider.data

import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.History
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.DataReceived

interface DataProvider {
    val type: DataProviderType

    fun reset()

    fun <T> retrieveData(retrieve: DataRetriever.() -> T): T

    interface DataRetriever {
        fun getStrikes(
            parameters: Parameters,
            history: History?,
            flags: Flags,
        ): DataReceived

        fun getStrikesGrid(
            parameters: Parameters,
            history: History?,
            flags: Flags,
        ): DataReceived

        fun getStations(region: Int): List<Station>
    }
}

fun initializeResult(
    parameters: Parameters,
    history: History?,
    flags: Flags,
): DataReceived =
    DataReceived(referenceTime = System.currentTimeMillis(), parameters = parameters, history = history, flags = flags)
