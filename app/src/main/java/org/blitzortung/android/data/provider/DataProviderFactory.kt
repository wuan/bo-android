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

import org.blitzortung.android.data.provider.blitzortung.BlitzortungHttpDataProvider
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataProviderFactory @Inject constructor(defaultProvider: JsonRpcDataProvider, blitzortungProvider: BlitzortungHttpDataProvider) {

    val dataProvidersByType: Map<DataProviderType, DataProvider> = listOf<DataProvider>(defaultProvider, blitzortungProvider).groupBy { it.type }.mapValues { it.value.first() };

    fun getDataProviderForType(providerType: DataProviderType): DataProvider {
        return dataProvidersByType[providerType]
                ?: throw IllegalStateException("unhandled data provider type '$providerType'")
    }
}
