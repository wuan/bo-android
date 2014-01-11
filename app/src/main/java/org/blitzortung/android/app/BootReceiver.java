package org.blitzortung.android.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(final Context context, Intent intent) {
        Intent bootIntent = new Intent(context, DataService.class);
        bootIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(bootIntent);
    }
}