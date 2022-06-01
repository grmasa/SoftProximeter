package com.android.softproximeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class autostart extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1) {
        Intent serviceIntent = new Intent(context, DetectService.class);
        serviceIntent.putExtra("inputExtra", "AutoStartService");
        context.startForegroundService(serviceIntent);
    }
}
