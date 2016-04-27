package org.blitzortung.android.notification.signal

open class VibrationSignal(
        val vibrationDuration: Long,
        val vibrator: (Long) -> Unit)
: NotificationSignal {

    override fun signal() {
        vibrator.invoke(vibrationDuration)
    }
}