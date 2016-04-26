package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences

class SignalManager(
        context: Context,
        sharedPreferences: SharedPreferences,
        private val signals: List<NotificationSignal> = listOf(
                VibrationSignal(context, sharedPreferences),
                SoundSignal(context, sharedPreferences)
        )
) {
    fun signal() {
        signals.forEach { it.signal() }
    }
}