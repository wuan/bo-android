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
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.provider.blitzortung.BlitzortungHttpDataProvider
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider
import java.net.URL

class DataProviderFactory {
    fun getDataProviderForType(providerType: DataProviderType, sharedPreferences: SharedPreferences): DataProvider {
        when (providerType) {
            DataProviderType.RPC -> {
                var serviceUrl = sharedPreferences.getString(PreferenceKey.SERVICE_URL.toString(), "")

                try {
                    URL(serviceUrl)
                } catch (e: Exception) {
                    serviceUrl = ""
                }
                return JsonRpcDataProvider(serviceUrl)
            }

            DataProviderType.HTTP -> return BlitzortungHttpDataProvider()

            else -> throw IllegalStateException("unhandled data provider type '%s'".format(providerType))
        }
    }
}
