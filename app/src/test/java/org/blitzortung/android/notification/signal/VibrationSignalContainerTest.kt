package org.blitzortung.android.notification.signal

import android.content.SharedPreferences
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.KotlinAssertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.jetbrains.anko.defaultSharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VibrationSignalContainerTest {

    lateinit private var vibrationSignalContainer: VibrationSignalContainer

    lateinit private var preferences: SharedPreferences

    private val vibrationSignal: VibrationSignal = mock()

    private var vibrationDuration: Long? = null

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.application
        preferences = context.defaultSharedPreferences
        vibrationSignalContainer = VibrationSignalContainer(context, preferences, { vibrationDuration ->
            vibrationSignalProvider(vibrationDuration)
        })
    }

    @Test
    fun vibrationDurationShouldBeInitializedWithDefaultValueIfNoDurationIsSet() {
        assertThat(vibrationDuration).isEqualTo(30L)
    }

    @Test
    fun vibrationDurationShouldBeUpdatedWhenPreferencesAreModified() {
        val vibrationDuration = 1234L
        preferences.edit().putInt(PreferenceKey.ALERT_VIBRATION_SIGNAL.toString(), vibrationDuration.toInt()).apply()
        assertThat(vibrationDuration).isEqualTo(vibrationDuration)
    }

    @Test
    fun vibratorShouldBeRunWhenSignalIsCalled() {
        val vibrationDuration = 1234
        preferences.edit().putInt(PreferenceKey.ALERT_VIBRATION_SIGNAL.toString(), vibrationDuration).apply()

        vibrationSignalContainer.signal()

        verify(vibrationSignal).signal()
    }

    fun vibrationSignalProvider(vibrationDuration: Long): VibrationSignal {
        this.vibrationDuration = vibrationDuration
        return vibrationSignal
    }
}

