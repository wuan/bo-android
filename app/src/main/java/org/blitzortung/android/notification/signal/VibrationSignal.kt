package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.jetbrains.anko.vibrator

class VibrationSignal(
        context: Context,
        preferences: SharedPreferences,
        val vibrator: (Long) -> Unit = {vibrationDuration -> context.vibrator.vibrate(vibrationDuration)}
) : NotificationSignal, OnSharedPreferenceChangeListener {

    internal var vibrationDuration: Long = 0

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferencesChanged(preferences, PreferenceKey.ALERT_VIBRATION_SIGNAL)
    }

    override fun signal() {
        vibrator.invoke(vibrationDuration)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.ALERT_VIBRATION_SIGNAL -> {
                vibrationDuration = sharedPreferences.get(key, 3) * 10L
            }
        }
    }
}