package org.blitzortung.android.data.cache

import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ResultEvent
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
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

    fun logStats() {
        var totalSize = 0L
        cache.entries.forEach {
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(it.value)
            oos.close()
            val bytes = baos.toByteArray()
            Log.v(LOG_TAG, "${it.key} -> ${bytes.size}")
            totalSize += bytes.size
        }
        Log.v(LOG_TAG, "total size: %.2f MB".format(totalSize/1024f/1024f))
    }

    fun clear() {
        cache.clear()
    }

    companion object {
        const val DEFAULT_EXPIRY_TIME: Long = 5 * 60 * 1000
    }
}

data class Timestamped<T>(val value: T, val timestamp: Long = System.currentTimeMillis()) : Serializable