package org.blitzortung.android.data.cache

import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ResultEvent
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCache @Inject constructor() {

    val cache = hashMapOf<Parameters, Timestamped<ResultEvent>>()

    fun get(parameters: Parameters, expiryTime: Long = DEFAULT_EXPIRY_TIME): ResultEvent? {
        val entry = cache[parameters] ?: return null
        if (entry.timestamp < System.currentTimeMillis() - expiryTime) {
            cache.remove(parameters)
            return null
        }
        return entry.value
    }

    fun put(parameters: Parameters, dataEvent: ResultEvent) {
        cache[parameters] = Timestamped(dataEvent.copy(sequenceNumber = null))
    }

    fun calculateTotalSize(): CacheSize = cache.entries.fold(CacheSize(0, 0)) { acc, entry ->
        val resultEvent = entry.value.value
        val strikeCount = resultEvent.strikes?.size ?: 0
        Log.v(LOG_TAG, "${entry.key} -> ${strikeCount}")
        CacheSize(acc.entries + 1, acc.strikes + strikeCount)
    }

    fun clear() {
        cache.clear()
    }

    companion object {
        const val DEFAULT_EXPIRY_TIME: Long = 5 * 60 * 1000
    }
}

data class Timestamped<T>(val value: T, val timestamp: Long = System.currentTimeMillis()) : Serializable

data class CacheSize(
    val entries: Int,
    val strikes: Int,
)