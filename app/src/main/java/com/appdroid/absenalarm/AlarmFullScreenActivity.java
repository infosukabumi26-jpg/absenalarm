package com.appdroid.absenalarm;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Full-screen alarm activity that shows over the lock screen.
 * Cannot be dismissed with back button - only via the ABSEN SEKARANG button.
 * When confirmed, stops alarm and opens the attendance URL in browser.
 */
public class AlarmFullScreenActivity extends AppCompatActivity {

    private Handler timeHandler;
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        // Window flags for visibility over lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );

        // Full screen immersive mode
        hideSystemUI();

        setContentView(R.layout.activity_alarm_fullscreen);

        // Get alarm info
        String alarmType = getIntent().getStringExtra("alarm_type");
        String url = getIntent().getStringExtra("url");

        if (alarmType == null) alarmType = AlarmPreferences.TYPE_MASUK;
        if (url == null) url = new AlarmPreferences(this).getUrl();

        // Set alarm title
        TextView tvTitle = findViewById(R.id.tvAlarmTitle);
        if (AlarmPreferences.TYPE_MASUK.equals(alarmType)) {
            tvTitle.setText(getString(R.string.waktunya_masuk));
        } else {
            tvTitle.setText(getString(R.string.waktunya_pulang));
        }

        // Set current date
        TextView tvDate = findViewById(R.id.tvCurrentDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        tvDate.setText(dateFormat.format(new Date()));

        // Update time display every second
        TextView tvTime = findViewById(R.id.tvCurrentTime);
        startTimeUpdate(tvTime);

        // Animate alarm icon (shake/pulse)
        ImageView ivAlarm = findViewById(R.id.ivAlarmIcon);
        startAlarmAnimation(ivAlarm);

        // Animate confirm button (pulse)
        MaterialButton btnConfirm = findViewById(R.id.btnAbsenSekarang);
        startButtonPulse(btnConfirm);

        // Handle confirm button
        final String finalUrl = url;
        btnConfirm.setOnClickListener(v -> {
            confirmAlarm(finalUrl);
        });
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void startTimeUpdate(TextView tvTime) {
        timeHandler = new Handler(Looper.getMainLooper());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                tvTime.setText(timeFormat.format(new Date()));
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void startAlarmAnimation(ImageView icon) {
        // Shake animation (rotate left-right like a ringing bell)
        ObjectAnimator rotateLeft = ObjectAnimator.ofFloat(icon, "rotation", 0f, -15f);
        rotateLeft.setDuration(100);

        ObjectAnimator rotateRight = ObjectAnimator.ofFloat(icon, "rotation", -15f, 15f);
        rotateRight.setDuration(200);
        rotateRight.setRepeatCount(3);
        rotateRight.setRepeatMode(ObjectAnimator.REVERSE);

        ObjectAnimator rotateBack = ObjectAnimator.ofFloat(icon, "rotation", 15f, 0f);
        rotateBack.setDuration(100);

        // Scale pulse
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.2f, 1f);
        scaleX.setDuration(800);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.2f, 1f);
        scaleY.setDuration(800);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet shakeSet = new AnimatorSet();
        shakeSet.playSequentially(rotateLeft, rotateRight, rotateBack);
        shakeSet.setStartDelay(500);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY);

        shakeSet.start();
        pulseSet.start();
    }

    private void startButtonPulse(MaterialButton button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.05f, 1f);
        scaleX.setDuration(1200);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.05f, 1f);
        scaleY.setDuration(1200);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.8f, 1f);
        alpha.setDuration(1200);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
    }

    /**
     * User confirmed the alarm - stop everything and open URL.
     */
    private void confirmAlarm(String url) {
        // Stop time updates
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }

        // Stop the alarm service
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction("STOP_ALARM");
        startService(stopIntent);

        // Open URL in browser
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(browserIntent);
        } catch (Exception e) {
            // If no browser available, just finish
        }

        // Close this activity
        finish();
    }

    /**
     * Override back press - alarm cannot be dismissed with back button!
     */
    @Override
    public void onBackPressed() {
        // DO NOTHING - user must press ABSEN SEKARANG
        // This is intentional - the alarm cannot be dismissed
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // User pressed HOME - the service will re-launch this activity
    }

    @Override
    protected void onDestroy() {
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Re-hide system UI when re-launched
        hideSystemUI();
    }
}

