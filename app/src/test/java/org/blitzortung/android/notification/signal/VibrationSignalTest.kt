package org.blitzortung.android.notification.signal

import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VibrationSignalTest {

    lateinit private var vibrationSignal: VibrationSignal

    private val vibrationDuration = 1234L

    private val vibratorCallCounter = AtomicInteger()

    private val vibrator : Function1<Long, Unit> = {vibrationDuration ->
        assertThat(vibrationDuration).isEqualTo(this.vibrationDuration)
        vibratorCallCounter.incrementAndGet()
    }

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.application
        vibrationSignal = VibrationSignal(vibrationDuration, vibrator)
    }

    @Test
    fun vibratorShouldNotBeCalledDuringConstruction() {
        assertThat(vibratorCallCounter.get()).isEqualTo(0)
    }

    @Test
    fun ringtoneShouldBeCreatedAndPlayedWhenSignalIsCalled() {
        vibrationSignal.signal()

        assertThat(vibratorCallCounter.get()).isEqualTo(1)
    }
}

