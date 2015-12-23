package org.blitzortung.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bootIntent = Intent(context, AppService::class.java)
        bootIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startService(bootIntent)
    }
}