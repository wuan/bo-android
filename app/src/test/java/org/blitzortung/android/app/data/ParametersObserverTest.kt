package org.blitzortung.android.app.data

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.assertThat
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.event.status.StatusEvent
import org.blitzortung.android.data.provider.event.status.StatusProgressUpdateEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rx.Observer

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class ParametersObserverTest {

    private val statusObserver: Observer<StatusEvent> = mock()

    @Test
    fun onNextShouldInvokeUpdaterAndOnNextMethod() {
        data class Test(var parameters: Parameters?)

        val test = Test(null)
        val observer = ParametersObserver(this.statusObserver, { parameters -> test.parameters = parameters })

        val parameters = Parameters()

        observer.onNext(parameters)

        assertThat(test.parameters).isEqualTo(parameters)
        verify(statusObserver).onNext(StatusProgressUpdateEvent(running = true))
    }

}