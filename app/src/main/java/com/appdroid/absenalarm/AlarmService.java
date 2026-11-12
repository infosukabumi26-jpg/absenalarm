package com.appdroid.absenalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Foreground service that plays alarm sound, vibrates, and launches fullscreen activity.
 * The alarm keeps playing until the user confirms (taps ABSEN SEKARANG).
 * If the user presses HOME, the service re-launches the fullscreen activity.
 */
public class AlarmService extends Service {

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 9999;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Handler handler;
    private Runnable relaunchRunnable;
    private String alarmType;
    private String url;
    private boolean isAlarmActive = false;

    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if ("STOP_ALARM".equals(action)) {
            stopAlarm();
            return START_NOT_STICKY;
        }

        alarmType = intent.getStringExtra("alarm_type");
        String ringtoneUri = intent.getStringExtra("ringtone_uri");
        url = intent.getStringExtra("url");

        if (alarmType == null) alarmType = AlarmPreferences.TYPE_MASUK;
        if (url == null) url = new AlarmPreferences(this).getUrl();

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        // Play alarm sound
        playAlarmSound(ringtoneUri);

        // Start vibration
        startVibration();

        // Launch fullscreen activity
        launchFullScreenActivity();

        // Set up periodic re-launch (if user presses HOME, re-show after 5 seconds)
        isAlarmActive = true;
        setupRelaunch();

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_alarm),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.channel_alarm_desc));
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        // Intent to open fullscreen activity when notification is tapped
        Intent fullScreenIntent = new Intent(this, AlarmFullScreenActivity.class);
        fullScreenIntent.putExtra("alarm_type", alarmType);
        fullScreenIntent.putExtra("url", url);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Stop action
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = AlarmPreferences.TYPE_MASUK.equals(alarmType)
                ? "⏰ Waktunya ABSEN MASUK!"
                : "⏰ Waktunya ABSEN PULANG!";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText("Tekan untuk membuka halaman absen")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_alarm, "ABSEN SEKARANG", fullScreenPendingIntent)
                .build();
    }

    private void playAlarmSound(String ringtoneUriString) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            Uri ringtoneUri;
            if (ringtoneUriString != null) {
                ringtoneUri = Uri.parse(ringtoneUriString);
            } else {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (ringtoneUri == null) {
                    ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, ringtoneUri);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);

            // Set volume to max
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            Log.i(TAG, "Alarm sound playing: " + ringtoneUri);
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
            // Fallback: try default notification sound
            try {
                Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (fallback != null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(this, fallback);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }
            } catch (Exception e2) {
                Log.e(TAG, "Fallback sound also failed", e2);
            }
        }
    }

    private void startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                // Vibrate pattern: wait 0ms, vibrate 1000ms, sleep 500ms, repeat
                long[] pattern = {0, 1000, 500, 1000, 500};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting vibration", e);
        }
    }

    private void launchFullScreenActivity() {
        Intent intent = new Intent(this, AlarmFullScreenActivity.class);
        intent.putExtra("alarm_type", alarmType);
        intent.putExtra("url", url);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    /**
     * Re-launch the fullscreen activity every 5 seconds if alarm is still active.
     * This ensures the user cannot dismiss the alarm by pressing HOME.
     */
    private void setupRelaunch() {
        relaunchRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAlarmActive) {
                    launchFullScreenActivity();
                    handler.postDelayed(this, 5000); // Re-launch every 5 seconds
                }
            }
        };
        handler.postDelayed(relaunchRunnable, 5000);
    }

    /**
     * Stop the alarm - called when user confirms (ABSEN SEKARANG).
     */
    public void stopAlarm() {
        isAlarmActive = false;
        isRunning = false;

        // Stop sound
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping media player", e);
            }
            mediaPlayer = null;
        }

        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }

        // Stop re-launch
        if (handler != null && relaunchRunnable != null) {
            handler.removeCallbacks(relaunchRunnable);
        }

        // Remove notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID);
        }

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

