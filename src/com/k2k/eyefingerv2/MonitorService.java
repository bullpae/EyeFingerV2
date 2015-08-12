package com.k2k.eyefingerv2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class MonitorService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter i = new IntentFilter();
        i.addAction(Intent.ACTION_SCREEN_ON);
        i.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mMonitorReceiver, i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
        String action = "";
        if(intent != null && intent.getAction() != null) {
            action = intent.getAction();
        }
//        if(action.equals("start")) {

//        }
//        else if(action.equals("stop")) {
//            unregisterReceiver(mMonitorReceiver);
//        }
        return START_STICKY;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mMonitorReceiver);
    }

    private final BroadcastReceiver mMonitorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                EyeFingerActivity.startCover(context, false);
            }
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                EyeFingerActivity.startCover(context, false);
            }
        }
    };
}
