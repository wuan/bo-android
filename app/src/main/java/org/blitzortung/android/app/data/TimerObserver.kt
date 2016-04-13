package org.blitzortung.android.app.data;

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.ParametersComponent
import org.blitzortung.android.app.StateFragment
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.event.status.StatusEvent
import org.blitzortung.android.data.provider.event.status.StatusProgressUpdateEvent
import org.blitzortung.android.data.provider.event.status.TimeStatusUpdateEvent
import rx.Observer
import rx.subjects.PublishSubject

class TimerObserver(
        preferences: SharedPreferences,
        private val statusObservable: PublishSubject<StatusEvent>,
        private val stateFragment: StateFragment,
        private val parametersComponent: ParametersComponent
) : Observer<Long>, OnSharedPreferenceChangeListener {

    private val errorUpdateInterval = 10000L

    private var updateInterval = 30000L

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD)
    }

    override fun onNext(index: Long?) {
        val currentTime = System.currentTimeMillis()
        val currentUpdateInterval = if (stateFragment.data.failed) errorUpdateInterval else updateInterval
        val timeUntilUpdate = getTimeUntilUpdate(currentUpdateInterval, currentTime) + 500
        statusObservable.onNext(TimeStatusUpdateEvent("${if (timeUntilUpdate < 0) "-" else "${timeUntilUpdate / 1000}"}/${currentUpdateInterval / 1000}s"))
        if (timeUntilUpdate <= 0) {
            Log.d(Main.LOG_TAG, "timerObserver() trigger update")
            statusObservable.onNext(StatusProgressUpdateEvent(true))
            parametersComponent.trigger()
        }
    }

    override fun onCompleted() {
        Log.v(Main.LOG_TAG, "TimerSubscriber.onCompleted()")
    }

    override fun onError(e: Throwable?) {
        Log.v(Main.LOG_TAG, "TimerSubscriber.onError() $e")
    }

    private fun getTimeUntilUpdate(currentUpdateInterval: Long, currentTime: Long): Long {
        return stateFragment.referenceTime + currentUpdateInterval - currentTime
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.QUERY_PERIOD -> {
                updateInterval = sharedPreferences.get(key, "30").toLong() * 1000
            }

            else -> {
            }
        }
    }
}
