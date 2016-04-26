package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get

class SoundSignalContainer(
        private val context: Context,
        preferences: SharedPreferences,
        private val soundSignalProvider: (Context, Uri?) -> SoundSignal? = { context, uri ->
            defaultSoundSignalProvider(context, uri)
        }
) : NotificationSignal, OnSharedPreferenceChangeListener {

    private var soundSignal: SoundSignal? = null

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferencesChanged(preferences, PreferenceKey.ALERT_SOUND_SIGNAL)
    }

    override fun signal() {
        soundSignal?.signal()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.ALERT_SOUND_SIGNAL -> {
                val sound: String = sharedPreferences.get(key, "")

                soundSignal = if (!sound.isNullOrEmpty()) {
                    soundSignalProvider.invoke(context, Uri.parse(sound))
                } else {
                    null
                }
            }
        }
    }
}

internal fun defaultSoundSignalProvider(
        context: Context,
        ringtoneUri: Uri?,
        ringtoneProvider: (context: Context, ringtoneUri: Uri) -> Ringtone? = ::defaultRingtoneProvider): SoundSignal? {
    return ringtoneUri?.let {
        return SoundSignal(context, { ringtoneProvider.invoke(context, ringtoneUri) })
    }
}

private fun defaultRingtoneProvider(context: Context, ringtoneUri: Uri): Ringtone? {
    return RingtoneManager.getRingtone(context, ringtoneUri)
}
