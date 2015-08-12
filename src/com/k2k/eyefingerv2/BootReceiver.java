package com.k2k.eyefingerv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    public static final String PREFS_NAME = "OHService";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            context.startService(new Intent(context, MonitorService.class));
        }
    }
}
