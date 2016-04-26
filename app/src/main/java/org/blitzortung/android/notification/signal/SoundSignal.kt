package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.util.isAtLeast

class SoundSignal(
        private val context: Context,
        preferences: SharedPreferences,
        private val ringtoneFactory: (Context, Uri?) -> Ringtone? = { context, ringtoneUri -> RingtoneManager.getRingtone(context, ringtoneUri) }
) : NotificationSignal, OnSharedPreferenceChangeListener {

    internal var soundUri: Uri? = null

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferencesChanged(preferences, PreferenceKey.ALERT_SOUND_SIGNAL)
    }

    override fun signal() {
        ringtoneFactory.invoke(context, soundUri)?.let { ringtone ->
            if (!ringtone.isPlaying) {
                if (isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
                    ringtone.audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
                } else {
                    @Suppress("DEPRECATION")
                    ringtone.streamType = AudioManager.STREAM_NOTIFICATION
                }
                ringtone.play()

                Log.v(Main.LOG_TAG, "playing " + ringtone.getTitle(context))
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.ALERT_SOUND_SIGNAL -> {
                val sound: String = sharedPreferences.get(key, "")

                soundUri = if (!sound.isNullOrEmpty()) {
                    Uri.parse(sound)
                } else {
                    null
                }
            }
        }
    }
}