package org.blitzortung.android.notification.signal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.os.Build
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.util.isAtLeast

open class SoundSignal(
        private val context: Context,
        private val ringtoneProvider: () -> Ringtone?
) : NotificationSignal {

    override fun signal() {
        val ringtone = ringtoneProvider.invoke()

        if (ringtone != null) {
            if (!ringtone.isPlaying) {
                if (isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
                    ringtone.audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
                } else {
                    @Suppress("DEPRECATION")
                    ringtone.streamType = AudioManager.STREAM_NOTIFICATION
                }
                ringtone.play()

                Log.v(Main.LOG_TAG, "SoundSignal: playing " + ringtone.getTitle(context))
            }
        } else {
            Log.w(Main.LOG_TAG, "SoundSignal: could not create ringtone")
        }
    }
}