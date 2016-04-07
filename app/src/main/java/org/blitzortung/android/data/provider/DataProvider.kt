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

package org.blitzortung.android.data.provider

import android.content.pm.PackageInfo

import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.provider.result.ResultEvent

abstract class DataProvider {
    protected var pInfo: PackageInfo? = null

    abstract val type: DataProviderType

    abstract fun reset()

    fun setPackageInfo(pInfo: PackageInfo) {
        this.pInfo = pInfo
    }

    abstract val isCapableOfHistoricalData: Boolean

    abstract fun <T> retrieveData(username: String?, password: String?, retrieve: DataRetriever.() -> T): T

    interface DataRetriever {
        fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent
        fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent
        fun getStations(region: Int): List<Station>
    }
}
