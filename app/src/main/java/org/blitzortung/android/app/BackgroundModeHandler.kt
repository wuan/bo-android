package org.blitzortung.android.app

import android.content.Context
import android.os.Handler
import android.util.Log
import org.blitzortung.android.app.event.BackgroundModeEvent
import org.blitzortung.android.protocol.ConsumerContainer

class BackgroundModeHandler(private val context: Context)  {
    val handler = Handler(context.mainLooper)

    private var runnable: Runnable? = null

    private val consumerContainer = object : ConsumerContainer<BackgroundModeEvent>() {
        override fun addedFirstConsumer() { }

        override fun removedLastConsumer() { }
    }

    fun updateBackgroundMode(isInBackground: Boolean) {

        //Post the isInBackground = true after 500ms
        //When the users switches activities, the flag is set to true and shortly after to false,
        //so we shouldn't send out a broadcast
        if(isInBackground) {
            runnable = Runnable {
                sendUpdates(isInBackground)

                runnable = null
            }

            handler.postDelayed(runnable, 500)
        } else {
            if(runnable is Runnable) {
                handler.removeCallbacks(runnable)

                runnable = null
            } else {
                sendUpdates(isInBackground)
            }
        }
    }

    fun sendUpdates(isInBackground: Boolean) {
        consumerContainer.storeAndBroadcast(BackgroundModeEvent(isInBackground))
        Log.d(Main.LOG_TAG, "BackgroundModeHandler: Broadcasted isInBackground: $isInBackground")
    }

    fun requestUpdates(consumer: (BackgroundModeEvent) -> Unit) {
        consumerContainer.addConsumer(consumer)
    }

    fun removeUpdates(consumer: (BackgroundModeEvent) -> Unit) {
        consumerContainer.removeConsumer(consumer)
    }
}