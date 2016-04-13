package org.blitzortung.android.app.data

import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.event.status.StatusEvent
import org.blitzortung.android.data.provider.event.status.StatusProgressUpdateEvent
import rx.Observer

class ParametersObserver(
        private val statusObserver: Observer<StatusEvent>,
        private val updater: (Parameters) -> Unit
) : Observer<Parameters> {
    override fun onError(e: Throwable?) {
        Log.v(Main.LOG_TAG, "ParametersObserver.onError() $e")
    }

    override fun onCompleted() {
        Log.v(Main.LOG_TAG, "ParametersObserver.onCompleted()")
    }

    override fun onNext(parameters: Parameters?) {
        Log.d(Main.LOG_TAG, "ParametersObserver.onNext($parameters)")
        parameters?.let { parameters -> updater.invoke(parameters) }
        statusObserver.onNext(StatusProgressUpdateEvent(running = true))
    }
}