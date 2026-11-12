package com.appdroid.absenalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * BroadcastReceiver that triggers when an alarm fires.
 * Starts the AlarmService which handles sound, vibration, and fullscreen display.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Alarm received!");

        String alarmType = intent.getStringExtra("alarm_type");
        String ringtoneUri = intent.getStringExtra("ringtone_uri");
        String url = intent.getStringExtra("url");

        if (alarmType == null) {
            alarmType = AlarmPreferences.TYPE_MASUK;
        }

        Log.i(TAG, "Alarm type: " + alarmType);

        // Start the foreground service
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("alarm_type", alarmType);
        serviceIntent.putExtra("ringtone_uri", ringtoneUri);
        serviceIntent.putExtra("url", url);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Schedule next occurrence of this alarm
        AlarmScheduler.scheduleAlarm(context, alarmType);
    }
}

