package org.blitzortung.android.notification.signal

import android.content.Context
import org.jetbrains.anko.vibrator

class VibrationSignal(context: Context, var vibrationDuration: Long) : NotificationSignal {
    private val vibrator = context.vibrator

    override fun signal() {
        vibrator.vibrate(vibrationDuration)
    }
}