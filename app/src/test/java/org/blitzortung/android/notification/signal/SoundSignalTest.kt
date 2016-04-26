package org.blitzortung.android.notification.signal

import android.content.SharedPreferences
import android.media.Ringtone
import android.net.Uri
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
class SoundSignalTest {

    lateinit private var soundSignal: SoundSignal

    lateinit private var preferences: SharedPreferences

    private val ringtone: Ringtone = mock()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.application
        preferences = context.defaultSharedPreferences
        soundSignal = SoundSignal(context, preferences)
    }

    @Test
    fun soundUriShouldBeInitializedToNullIfNoUriIsSet() {
        assertThat(soundSignal.soundUri).isNull()
    }

    @Test
    fun soundUriShouldBeUpdatedWhenPreferencesAreModified() {
        val soundUri = "file:///path/to/signal"
        preferences.edit().putString(PreferenceKey.ALERT_SOUND_SIGNAL.toString(), soundUri).apply()
        assertThat(soundSignal.soundUri).isEqualTo(Uri.parse(soundUri))
    }

    @Test
    fun ringtoneShouldBePlayedWhenSignalIsCalled() {
        val soundUri = "file:///path/to/signal"
        preferences.edit().putString(PreferenceKey.ALERT_SOUND_SIGNAL.toString(), soundUri).apply()
        soundSignal = SoundSignal(RuntimeEnvironment.application, preferences, { context, uri ->
            assert(uri.toString() == soundUri)
            ringtone
        })

        soundSignal.signal()

        verify(ringtone).play()
    }


}