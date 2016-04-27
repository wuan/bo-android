package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.jetbrains.anko.vibrator

class VibrationSignalContainer(
        context: Context,
        preferences: SharedPreferences,
        val vibrationSignalProvider: (Long) -> VibrationSignal = { vibrationDuration ->
            defaultVibrationSignalProvider(context, vibrationDuration)
        }
) : NotificationSignal, OnSharedPreferenceChangeListener {

    lateinit private var vibrationSignal: VibrationSignal

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferencesChanged(preferences, PreferenceKey.ALERT_VIBRATION_SIGNAL)
    }

    override fun signal() {
        vibrationSignal.signal()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.ALERT_VIBRATION_SIGNAL -> {
                val vibrationDuration = sharedPreferences.get(key, 3) * 10L
                vibrationSignal = vibrationSignalProvider.invoke(vibrationDuration)
            }
        }
    }
}

internal fun defaultVibrationSignalProvider(
        context: Context,
        vibrationDuration: Long,
        vibrator: (Long) -> Unit = defaultVibrator(context)
): VibrationSignal {
    return VibrationSignal(vibrationDuration, vibrator)
}

private fun defaultVibrator(context: Context): (Long) -> Unit {
    return { vibrationDuration -> context.vibrator.vibrate(vibrationDuration) }
}