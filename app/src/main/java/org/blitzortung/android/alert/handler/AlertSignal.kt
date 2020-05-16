package org.blitzortung.android.alert.handler

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.PreferenceKey.ALERT_SOUND_SIGNAL
import org.blitzortung.android.app.view.PreferenceKey.ALERT_VIBRATION_SIGNAL
import org.blitzortung.android.app.view.get
import org.blitzortung.android.util.isAtLeast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertSignal @Inject constructor(
        private val context: Context,
        private val vibrator: Vibrator
) : OnSharedPreferenceChangeListener {

    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, ALERT_VIBRATION_SIGNAL, ALERT_SOUND_SIGNAL)
    }

    private var soundSignal: Uri? = null

    private var vibrationDuration = 0L

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            ALERT_VIBRATION_SIGNAL -> {
                vibrationDuration = sharedPreferences.get(key, 3) * 10L
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() vibrationDuration = $vibrationDuration")
            }

            ALERT_SOUND_SIGNAL -> {
                val signalUri = sharedPreferences.get(key, "")
                soundSignal = if (signalUri.isNotEmpty()) Uri.parse(signalUri) else null
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() soundSignal = $soundSignal")
            }
        }
    }

    fun emitSignal() {
        vibrateIfEnabled()
        playSoundIfEnabled()
    }

    private fun vibrateIfEnabled() {
        vibrator.vibrate(vibrationDuration)
    }

    private fun playSoundIfEnabled() {
        soundSignal?.let { signal ->
            RingtoneManager.getRingtone(context, signal)?.let { ringtone ->
                if (!ringtone.isPlaying) {
                    if (isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
                        ringtone.audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
                    } else {
                        @Suppress("DEPRECATION")
                        ringtone.streamType = AudioManager.STREAM_NOTIFICATION
                    }
                    ringtone.play()
                }
                Log.v(Main.LOG_TAG, "playing " + ringtone.getTitle(context))
            }
        }
    }
}