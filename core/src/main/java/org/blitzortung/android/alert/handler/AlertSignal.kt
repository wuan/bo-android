package org.blitzortung.android.alert.handler

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton
import org.blitzortung.android.preferences.OnSharedPreferenceChangeListener
import org.blitzortung.android.preferences.PreferenceKey
import org.blitzortung.android.preferences.PreferenceKey.ALERT_SOUND_SIGNAL
import org.blitzortung.android.preferences.PreferenceKey.ALERT_VIBRATION_SIGNAL
import org.blitzortung.android.preferences.get
import org.blitzortung.android.util.isAtLeast
import org.blitzortung.android.util.LogUtil.LOG_TAG

@Singleton
class AlertSignal
@Inject
constructor(
    private val context: Context,
    private val vibrator: Vibrator,
    private val notificationManager: NotificationManager,
) : OnSharedPreferenceChangeListener {
    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, ALERT_VIBRATION_SIGNAL, ALERT_SOUND_SIGNAL)
    }

    private var soundSignal: Uri? = null

    private var vibrationDuration = 0L

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: PreferenceKey,
    ) {
        when (key) {
            ALERT_VIBRATION_SIGNAL -> {
                vibrationDuration = sharedPreferences.get(key, 3) * 10L
                Log.v(
                    LOG_TAG,
                    "AlertSignal.onSharedPreferenceChanged() vibrationDuration = $vibrationDuration",
                )
            }

            ALERT_SOUND_SIGNAL -> {
                val signalUri = sharedPreferences.get(key, "")
                soundSignal = if (signalUri.isNotEmpty()) signalUri.toUri() else null
                Log.v(LOG_TAG, "AlertSignal.onSharedPreferenceChanged() soundSignal = $soundSignal")
            }

            else -> {}
        }
    }

    fun emitSignal() {
        val vibrated = vibrateIfEnabled()
        if (doNotDisturb()) {
            Log.i(LOG_TAG, "AlertSignal.emitSignal() sound suppressed by do not disturb, vibration: $vibrated")
        } else {
            val sounded = playSoundIfEnabled()
            Log.i(LOG_TAG, "AlertSignal.emitSignal() vibration: $vibrated, sound: $sounded")
        }
    }

    private fun vibrateIfEnabled(): Boolean {
        if (vibrationDuration <= 0) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ (API 26+): Use VibrationEffect
            val vibrationEffect =
                VibrationEffect.createOneShot(
                    vibrationDuration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            vibrator.vibrate(vibrationEffect)
        } else {
            // Legacy devices: Use deprecated method
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationDuration)
        }
        return true
    }

    private fun playSoundIfEnabled(): Boolean {
        val signal = soundSignal
        val ringtone = signal?.let { RingtoneManager.getRingtone(context, it) }
        return when {
            signal == null -> {
                Log.d(LOG_TAG, "AlertSignal.playSoundIfEnabled() no sound signal configured")
                false
            }

            ringtone == null -> {
                Log.w(LOG_TAG, "AlertSignal.playSoundIfEnabled() no ringtone for $signal")
                false
            }

            ringtone.isPlaying -> {
                Log.d(LOG_TAG, "AlertSignal.playSoundIfEnabled() ringtone already playing")
                false
            }

            else -> {
                playRingtone(ringtone)
                true
            }
        }
    }

    private fun playRingtone(ringtone: Ringtone) {
        if (isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            ringtone.audioAttributes =
                AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
        } else {
            @Suppress("DEPRECATION")
            ringtone.streamType = AudioManager.STREAM_NOTIFICATION
        }
        ringtone.play()
        Log.v(LOG_TAG, "AlertSignal.playRingtone() playing " + ringtone.getTitle(context))
    }

    private fun doNotDisturb(): Boolean {
        val currentInterruptionFilter = notificationManager.currentInterruptionFilter
        val doNotDisturb = currentInterruptionFilter >= NotificationManager.INTERRUPTION_FILTER_PRIORITY
        Log.v(
            LOG_TAG,
            "AlertSignal.doNotDisturb() interruption filter: ${interruptionFilterName(currentInterruptionFilter)}, " +
                "suppressed: $doNotDisturb",
        )
        return doNotDisturb
    }

    private fun interruptionFilterName(interruptionFilter: Int): String =
        when (interruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> "ALL"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "PRIORITY"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "NONE"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "ALARMS"
            else -> "UNKNOWN($interruptionFilter)"
        }
}
