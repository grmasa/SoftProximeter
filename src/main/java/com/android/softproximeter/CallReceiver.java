package com.android.softproximeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {
    public static String lastState = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            lastState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String returnCallState() {
        if (lastState == null) return "";
        return lastState;
    }
}