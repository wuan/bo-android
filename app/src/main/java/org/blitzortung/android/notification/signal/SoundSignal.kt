package org.blitzortung.android.notification.signal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.util.isAtLeast

class SoundSignal(private val context: Context,var soundUri: Uri): NotificationSignal {
    override fun signal() {
        RingtoneManager.getRingtone(context, soundUri)?.let {
            ringtone ->

            if(!ringtone.isPlaying) {
                if (isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
                    ringtone.audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
                } else {
                    ringtone.streamType = AudioManager.STREAM_NOTIFICATION
                }
                ringtone.play()

                Log.v(Main.LOG_TAG, "playing " + ringtone.getTitle(context))
            }
        }
    }

}