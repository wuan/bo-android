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
