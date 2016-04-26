package org.blitzortung.android.notification.signal

import android.content.SharedPreferences
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
class VibrationSignalTest {

    lateinit private var vibrationSignal: VibrationSignal

    lateinit private var preferences: SharedPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.application
        preferences = context.defaultSharedPreferences
        vibrationSignal = VibrationSignal(context, preferences)
    }

    @Test
    fun soundUriShouldBeInitializedWithDefaultValueIfNoDurationIsSet() {
        assertThat(vibrationSignal.vibrationDuration).isEqualTo(30)
    }

    @Test
    fun soundUriShouldBeUpdatedWhenPreferencesAreModified() {
        val vibrationDuration = 1234
        preferences.edit().putInt(PreferenceKey.ALERT_VIBRATION_SIGNAL.toString(), vibrationDuration).apply()
        assertThat(vibrationSignal.vibrationDuration).isEqualTo(vibrationDuration * 10L)
    }

    @Test
    fun vibratorShouldBeRunWhenSignalIsCalled() {
        val vibrationDuration = 1234
        preferences.edit().putInt(PreferenceKey.ALERT_VIBRATION_SIGNAL.toString(), vibrationDuration).apply()
        vibrationSignal = VibrationSignal(RuntimeEnvironment.application, preferences, { duration ->
            assert(duration == vibrationDuration * 10L)
        })

        vibrationSignal.signal()
    }
}

