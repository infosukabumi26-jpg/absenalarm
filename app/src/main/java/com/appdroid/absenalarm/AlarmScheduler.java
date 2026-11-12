package com.appdroid.absenalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;

/**
 * Schedules and cancels alarms using AlarmManager.
 * Uses setAlarmClock() for guaranteed exact timing even in Doze mode.
 */
public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private static final int REQUEST_CODE_MASUK = 1001;
    private static final int REQUEST_CODE_PULANG = 1002;

    /**
     * Schedule the next alarm for the given type (masuk/pulang).
     * Finds the next matching day and time, then sets an exact alarm.
     */
    public static void scheduleAlarm(Context context, String type) {
        AlarmPreferences prefs = new AlarmPreferences(context);

        if (!prefs.isEnabled(type)) {
            cancelAlarm(context, type);
            return;
        }

        int hour = prefs.getHour(type);
        int minute = prefs.getMinute(type);
        Set<Integer> days = prefs.getDays(type);

        if (days.isEmpty()) {
            Log.w(TAG, "No days selected for " + type);
            return;
        }

        long triggerTime = getNextAlarmTime(hour, minute, days);
        if (triggerTime <= 0) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("alarm_type", type);
        intent.putExtra("ringtone_uri", prefs.getRingtoneUri(type).toString());
        intent.putExtra("url", prefs.getUrl());

        int requestCode = AlarmPreferences.TYPE_MASUK.equals(type) ? REQUEST_CODE_MASUK : REQUEST_CODE_PULANG;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Use setAlarmClock for guaranteed delivery (shows alarm icon in status bar)
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                triggerTime, pendingIntent
        );
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(triggerTime);
        Log.i(TAG, "Alarm " + type + " scheduled for: " + cal.getTime());
    }

    /**
     * Cancel the alarm for the given type.
     */
    public static void cancelAlarm(Context context, String type) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = AlarmPreferences.TYPE_MASUK.equals(type) ? REQUEST_CODE_MASUK : REQUEST_CODE_PULANG;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        Log.i(TAG, "Alarm " + type + " cancelled");
    }

    /**
     * Schedule both alarms (masuk and pulang) if enabled.
     * Called after boot and when settings change.
     */
    public static void rescheduleAll(Context context) {
        scheduleAlarm(context, AlarmPreferences.TYPE_MASUK);
        scheduleAlarm(context, AlarmPreferences.TYPE_PULANG);
    }

    /**
     * Calculate the next alarm time based on hour, minute, and allowed days.
     * Returns milliseconds since epoch.
     */
    private static long getNextAlarmTime(int hour, int minute, Set<Integer> days) {
        Calendar now = Calendar.getInstance();
        Calendar alarm = Calendar.getInstance();
        alarm.set(Calendar.HOUR_OF_DAY, hour);
        alarm.set(Calendar.MINUTE, minute);
        alarm.set(Calendar.SECOND, 0);
        alarm.set(Calendar.MILLISECOND, 0);

        // Check today first
        if (alarm.after(now) && days.contains(alarm.get(Calendar.DAY_OF_WEEK))) {
            return alarm.getTimeInMillis();
        }

        // Check next 7 days
        for (int i = 1; i <= 7; i++) {
            alarm.add(Calendar.DAY_OF_YEAR, 1);
            if (days.contains(alarm.get(Calendar.DAY_OF_WEEK))) {
                return alarm.getTimeInMillis();
            }
        }

        return -1; // Should never reach here if days is not empty
    }
}

