package org.blitzortung.android.data.provider

import org.blitzortung.android.data.provider.blitzortung.BlitzortungHttpDataProvider
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider

class DataProviderFactory {
    fun getDataProviderForType(providerType: DataProviderType): DataProvider {
        when (providerType) {
            DataProviderType.RPC -> return JsonRpcDataProvider()

            DataProviderType.HTTP -> return BlitzortungHttpDataProvider()

            else -> throw IllegalStateException("unhandled data provider type '%s'".format(providerType))
        }
    }
}
