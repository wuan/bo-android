package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get

@Suppress("NON_EXHAUSTIVE_WHEN")
class SignalManager(private val context: Context, sharedPreferences: SharedPreferences): OnSharedPreferenceChangeListener {
    private val signals = mutableListOf<NotificationSignal>()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferencesChanged(sharedPreferences,
                PreferenceKey.ALERT_SOUND_SIGNAL, PreferenceKey.ALERT_VIBRATION_SIGNAL)
    }

    //TODO Doesn't follow the "Open for Extension/Closed for modification" principle
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when(key) {
            PreferenceKey.ALERT_VIBRATION_SIGNAL -> {
                val vibrationDuration = sharedPreferences.get(key, 3) * 10L

                val vibrationSignal = signals.firstOrNull { it is VibrationSignal }

                if(vibrationSignal != null) {
                    (vibrationSignal as VibrationSignal).vibrationDuration = vibrationDuration
                } else {
                    signals.add(VibrationSignal(context, vibrationDuration))
                }
            }

            PreferenceKey.ALERT_SOUND_SIGNAL -> {
                val sound = sharedPreferences.get(key, "")

                if(sound.isNullOrEmpty()) {
                    signals.removeAll { it is SoundSignal }
                } else {
                    val currentSignal = signals.firstOrNull { it is SoundSignal }

                    if(currentSignal != null) {
                        (currentSignal as SoundSignal).soundUri = Uri.parse(sound)
                    } else {
                        signals.add(SoundSignal(context, Uri.parse(sound)))
                    }
                }
            }
        }
    }

    fun signal() {
        signals.forEach { it.signal() }
    }
}