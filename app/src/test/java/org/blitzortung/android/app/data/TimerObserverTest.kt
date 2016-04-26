package org.blitzortung.android.app.data

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.blitzortung.android.app.ParametersComponent
import org.blitzortung.android.app.StateFragment
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.provider.event.status.StatusEvent
import org.blitzortung.android.data.provider.event.status.StatusProgressUpdateEvent
import org.blitzortung.android.data.provider.event.status.TimeStatusUpdateEvent
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.test.createPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rx.Observer

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class TimerObserverTest {

    private val statusObserver: Observer<StatusEvent> = mock()
    private val stateFragment: StateFragment = StateFragment(mock(), mock())
    private val parametersComponent: ParametersComponent = mock()

    private lateinit var timerObserver: TimerObserver

    @Before
    fun setUp() {
        val preferences = createPreferences { it.putString(PreferenceKey.QUERY_PERIOD.key, "15") }
        timerObserver = TimerObserver(preferences, statusObserver, stateFragment, parametersComponent)
    }

    @Test
    fun onNextShouldSendProgressStatusAndTriggerIfQueryPeriodIsOver() {
        val currentTimeMillis = System.currentTimeMillis()
        stateFragment.dataObservable.onNext(
                DataEvent(referenceTime = currentTimeMillis - 20000)
        )
        timerObserver.onNext(123L)

        verify(statusObserver).onNext(TimeStatusUpdateEvent("-/15s"))
        verify(statusObserver).onNext(StatusProgressUpdateEvent(running = true))
        verify(parametersComponent).trigger()
    }

    @Test
    fun onNextShouldSendTimerStatusIfQueryPeriodIsNotYetOver() {
        stateFragment.dataObservable.onNext(
                DataEvent(referenceTime = System.currentTimeMillis())
        )
        timerObserver.onNext(123L)

        verify(statusObserver).onNext(TimeStatusUpdateEvent("15/15s"))
        verifyNoMoreInteractions(statusObserver, parametersComponent)
    }

}