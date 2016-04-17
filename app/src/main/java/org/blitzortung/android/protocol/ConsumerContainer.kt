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

package org.blitzortung.android.protocol

import java.util.*

open class ConsumerContainer<P> {

    private val consumers: MutableSet<(P) -> Unit>

    var payload: P? = null
        private set

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
        val currentPayload = payload
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

    protected open fun addedFirstConsumer() {
    }

    protected open fun removedLastConsumer() {
    }

    fun storeAndBroadcast(updatedPayload: P) {
        payload = updatedPayload
        broadcast(updatedPayload)
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
