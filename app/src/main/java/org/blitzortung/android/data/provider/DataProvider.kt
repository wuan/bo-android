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

import android.content.SharedPreferences
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.blitzortung.BlitzortungHttpDataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider

interface DataProvider : OnSharedPreferenceChangeListener {
    fun createResultEvent(parameters: Parameters)
            = DataEvent(referenceTime = System.currentTimeMillis(), parameters = parameters)

    fun getStrikes(parameters: Parameters): DataEvent

    fun reset()

    companion object {

        fun createDataProvider(sharedPreferences: SharedPreferences, agentSuffix: String): DataProvider {
            val providerType = sharedPreferences.get(PreferenceKey.DATA_SOURCE, DataProviderType.RPC.name).toUpperCase()

            return when (providerType) {
                DataProviderType.RPC.name ->
                    JsonRpcDataProvider(agentSuffix, sharedPreferences)

                DataProviderType.HTTP.name ->
                    BlitzortungHttpDataProvider(sharedPreferences)

                else ->
                    InvalidDataProvider()
            }
        }

    }
}
