package org.blitzortung.android.data.cache

import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ResultEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCache @Inject constructor() {

    private val cache = hashMapOf<Parameters, Timestamped<ResultEvent>>()

    fun get(parameters: Parameters, expiryTime: Long = DEFAULT_EXPIRY_TIME): ResultEvent? {
        val entry = cache[parameters] ?: return null
        if (entry.timestamp < System.currentTimeMillis() - expiryTime) {
            cache.remove(parameters)
            return null
        }
        return entry.value
    }

    fun put(parameters: Parameters, dataEvent: ResultEvent) {
        cache[parameters] = Timestamped(dataEvent)
    }

    companion object {
        const val DEFAULT_EXPIRY_TIME: Long = 5 * 60 * 1000
    }
}

data class Timestamped<T>(val value: T, val timestamp: Long = System.currentTimeMillis())