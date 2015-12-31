package org.blitzortung.android.protocol

import android.util.Log
import org.blitzortung.android.app.Main
import java.util.*

abstract class ConsumerContainer<P> {

    private val consumers: MutableSet<(P) -> Unit>

    private var currentPayload: P? = null

    init {
        consumers = HashSet<(P) -> Unit>()
    }

    fun addConsumer(consumer: ((P) -> Unit)?) {
        if (consumer == null) {
            throw IllegalArgumentException("consumer may not be null")
        }

        if (!consumers.contains(consumer)) {
            val isFirst = consumers.isEmpty()
            consumers.add(consumer)
            if (isFirst) {
                addedFirstConsumer()
            }
            sendCurrentPayloadTo(consumer)
        }
    }

    protected fun sendCurrentPayloadTo(consumer: (P) -> Unit) {
        val currentPayload = currentPayload
        if (currentPayload != null) {
            consumer.invoke(currentPayload)
        }
    }

    fun removeConsumer(consumer: (P) -> Unit) {
        if (consumers.contains(consumer)) {
            consumers.remove(consumer)
            if (consumers.isEmpty()) {
                removedLastConsumer()
            }
        }
    }

    abstract fun addedFirstConsumer()

    abstract fun removedLastConsumer()

    fun storeAndBroadcast(payload: P) {
        currentPayload = payload
        broadcast(payload)
    }

    fun broadcast(payload: P) {
        for (consumer in consumers) {
            consumer.invoke(payload)
        }
    }

    val isEmpty: Boolean
        get() = consumers.isEmpty()

    fun size(): Int {
        return consumers.size
    }
}
