package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
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
class SoundSignalContainerTest {

    lateinit private var soundSignalContainer: SoundSignalContainer

    lateinit private var preferences: SharedPreferences

    private val soundSignal: SoundSignal = mock()

    private var ringtoneUri: Uri? = null

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.application
        preferences = context.defaultSharedPreferences
        soundSignalContainer = SoundSignalContainer(context, preferences, { context, uri -> soundSignalProvider(context, uri) })
    }

    @Test
    fun soundUriShouldBeInitializedToNullIfNoUriIsSet() {
        assertThat(ringtoneUri).isNull()
    }

    @Test
    fun soundUriShouldBeUpdatedWhenPreferencesAreModified() {
        val soundUri = "file:///path/to/signal"
        preferences.edit().putString(PreferenceKey.ALERT_SOUND_SIGNAL.toString(), soundUri).apply()
        assertThat(ringtoneUri).isEqualTo(Uri.parse(soundUri))
    }

    @Test
    fun ringtoneShouldBePlayedWhenSignalIsCalled() {
        val soundUri = "file:///path/to/signal"
        preferences.edit().putString(PreferenceKey.ALERT_SOUND_SIGNAL.toString(), soundUri).apply()

        soundSignalContainer.signal()

        verify(soundSignal).signal()
    }

    fun soundSignalProvider(context: Context, ringtoneUri: Uri?): SoundSignal? {
        assertThat(context).isSameAs(RuntimeEnvironment.application)
        this.ringtoneUri = ringtoneUri
        return soundSignal
    }
}