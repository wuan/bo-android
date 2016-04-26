package org.blitzortung.android.notification.signal

import android.media.Ringtone
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
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
class SoundSignalTest {

    lateinit private var soundSignal: SoundSignal

    private val ringtone: Ringtone = mock()

    private val providerCallCounter = AtomicInteger()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.application
        soundSignal = SoundSignal(context, { providerCallCounter.incrementAndGet(); ringtone })
    }

    @Test
    fun providerShouldNotBeCalledDuringConstruction() {
        assertThat(providerCallCounter.get()).isEqualTo(0)
    }

    @Test
    fun ringtoneShouldBeCreatedAndPlayedWhenSignalIsCalled() {
        soundSignal.signal()

        assertThat(providerCallCounter.get()).isEqualTo(1)
        verify(ringtone).play()
    }
}