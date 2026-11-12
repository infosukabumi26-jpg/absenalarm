package com.appdroid.absenalarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashSet;
import java.util.Set;

/**
 * Main activity for configuring alarm settings.
 * Allows setting time, days, ringtone for both masuk and pulang alarms.
 */
public class MainActivity extends AppCompatActivity {

    private AlarmPreferences prefs;

    // UI - Masuk
    private MaterialSwitch switchMasuk;
    private TextView tvTimeMasuk;
    private Chip chipMasukSen, chipMasukSel, chipMasukRab, chipMasukKam, chipMasukJum, chipMasukSab, chipMasukMin;
    private TextView tvRingtoneMasuk;

    // UI - Pulang
    private MaterialSwitch switchPulang;
    private TextView tvTimePulang;
    private Chip chipPulangSen, chipPulangSel, chipPulangRab, chipPulangKam, chipPulangJum, chipPulangSab, chipPulangMin;
    private TextView tvRingtonePulang;

    // UI - URL
    private TextInputEditText etUrl;
    private TextView tvStatusMasuk, tvStatusPulang;

    // Ringtone picker
    private String currentRingtoneType; // which alarm's ringtone we're picking

    private final ActivityResultLauncher<Intent> ringtonePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null && currentRingtoneType != null) {
                        prefs.setRingtoneUri(currentRingtoneType, uri.toString());
                        // Get ringtone name
                        Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                        String name = ringtone != null ? ringtone.getTitle(this) : "Custom";
                        prefs.setRingtoneName(currentRingtoneType, name);
                        updateRingtoneDisplay();
                    }
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, "Izin notifikasi diperlukan untuk alarm", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AlarmPreferences(this);

        initViews();
        loadSettings();
        setupListeners();
        requestPermissions();
    }

    private void initViews() {
        // Masuk
        switchMasuk = findViewById(R.id.switchMasuk);
        tvTimeMasuk = findViewById(R.id.tvTimeMasuk);
        chipMasukSen = findViewById(R.id.chipMasukSen);
        chipMasukSel = findViewById(R.id.chipMasukSel);
        chipMasukRab = findViewById(R.id.chipMasukRab);
        chipMasukKam = findViewById(R.id.chipMasukKam);
        chipMasukJum = findViewById(R.id.chipMasukJum);
        chipMasukSab = findViewById(R.id.chipMasukSab);
        chipMasukMin = findViewById(R.id.chipMasukMin);
        tvRingtoneMasuk = findViewById(R.id.tvRingtoneMasuk);

        // Pulang
        switchPulang = findViewById(R.id.switchPulang);
        tvTimePulang = findViewById(R.id.tvTimePulang);
        chipPulangSen = findViewById(R.id.chipPulangSen);
        chipPulangSel = findViewById(R.id.chipPulangSel);
        chipPulangRab = findViewById(R.id.chipPulangRab);
        chipPulangKam = findViewById(R.id.chipPulangKam);
        chipPulangJum = findViewById(R.id.chipPulangJum);
        chipPulangSab = findViewById(R.id.chipPulangSab);
        chipPulangMin = findViewById(R.id.chipPulangMin);
        tvRingtonePulang = findViewById(R.id.tvRingtonePulang);

        // URL & Status
        etUrl = findViewById(R.id.etUrl);
        tvStatusMasuk = findViewById(R.id.tvStatusMasuk);
        tvStatusPulang = findViewById(R.id.tvStatusPulang);
    }

    private void loadSettings() {
        // Masuk
        switchMasuk.setChecked(prefs.isEnabled(AlarmPreferences.TYPE_MASUK));
        tvTimeMasuk.setText(prefs.getTimeFormatted(AlarmPreferences.TYPE_MASUK));
        loadDayChips(AlarmPreferences.TYPE_MASUK);
        tvRingtoneMasuk.setText(prefs.getRingtoneName(AlarmPreferences.TYPE_MASUK));

        // Pulang
        switchPulang.setChecked(prefs.isEnabled(AlarmPreferences.TYPE_PULANG));
        tvTimePulang.setText(prefs.getTimeFormatted(AlarmPreferences.TYPE_PULANG));
        loadDayChips(AlarmPreferences.TYPE_PULANG);
        tvRingtonePulang.setText(prefs.getRingtoneName(AlarmPreferences.TYPE_PULANG));

        // URL
        etUrl.setText(prefs.getUrl());

        updateStatusDisplay();
    }

    private void loadDayChips(String type) {
        Set<Integer> days = prefs.getDays(type);
        if (AlarmPreferences.TYPE_MASUK.equals(type)) {
            chipMasukSen.setChecked(days.contains(AlarmPreferences.MONDAY));
            chipMasukSel.setChecked(days.contains(AlarmPreferences.TUESDAY));
            chipMasukRab.setChecked(days.contains(AlarmPreferences.WEDNESDAY));
            chipMasukKam.setChecked(days.contains(AlarmPreferences.THURSDAY));
            chipMasukJum.setChecked(days.contains(AlarmPreferences.FRIDAY));
            chipMasukSab.setChecked(days.contains(AlarmPreferences.SATURDAY));
            chipMasukMin.setChecked(days.contains(AlarmPreferences.SUNDAY));
        } else {
            chipPulangSen.setChecked(days.contains(AlarmPreferences.MONDAY));
            chipPulangSel.setChecked(days.contains(AlarmPreferences.TUESDAY));
            chipPulangRab.setChecked(days.contains(AlarmPreferences.WEDNESDAY));
            chipPulangKam.setChecked(days.contains(AlarmPreferences.THURSDAY));
            chipPulangJum.setChecked(days.contains(AlarmPreferences.FRIDAY));
            chipPulangSab.setChecked(days.contains(AlarmPreferences.SATURDAY));
            chipPulangMin.setChecked(days.contains(AlarmPreferences.SUNDAY));
        }
    }

    private void setupListeners() {
        // Time pickers
        MaterialButton btnTimeMasuk = findViewById(R.id.btnTimeMasuk);
        btnTimeMasuk.setOnClickListener(v -> showTimePicker(AlarmPreferences.TYPE_MASUK));

        MaterialButton btnTimePulang = findViewById(R.id.btnTimePulang);
        btnTimePulang.setOnClickListener(v -> showTimePicker(AlarmPreferences.TYPE_PULANG));

        // Ringtone pickers
        MaterialButton btnRingtoneMasuk = findViewById(R.id.btnRingtoneMasuk);
        btnRingtoneMasuk.setOnClickListener(v -> showRingtonePicker(AlarmPreferences.TYPE_MASUK));

        MaterialButton btnRingtonePulang = findViewById(R.id.btnRingtonePulang);
        btnRingtonePulang.setOnClickListener(v -> showRingtonePicker(AlarmPreferences.TYPE_PULANG));

        // Save button
        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void showTimePicker(String type) {
        int currentHour = prefs.getHour(type);
        int currentMinute = prefs.getMinute(type);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            prefs.setTime(type, hourOfDay, minute);
            String formatted = String.format("%02d:%02d", hourOfDay, minute);
            if (AlarmPreferences.TYPE_MASUK.equals(type)) {
                tvTimeMasuk.setText(formatted);
            } else {
                tvTimePulang.setText(formatted);
            }
        }, currentHour, currentMinute, true);

        dialog.setTitle(AlarmPreferences.TYPE_MASUK.equals(type)
                ? getString(R.string.waktu_absen_masuk)
                : getString(R.string.waktu_absen_pulang));
        dialog.show();
    }

    private void showRingtonePicker(String type) {
        currentRingtoneType = type;

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_RINGTONE | RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pilih_nada));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);

        Uri currentUri = prefs.getRingtoneUri(type);
        if (currentUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri);
        }

        ringtonePickerLauncher.launch(intent);
    }

    private void updateRingtoneDisplay() {
        tvRingtoneMasuk.setText(prefs.getRingtoneName(AlarmPreferences.TYPE_MASUK));
        tvRingtonePulang.setText(prefs.getRingtoneName(AlarmPreferences.TYPE_PULANG));
    }

    private Set<Integer> getSelectedDays(String type) {
        Set<Integer> days = new HashSet<>();
        if (AlarmPreferences.TYPE_MASUK.equals(type)) {
            if (chipMasukSen.isChecked()) days.add(AlarmPreferences.MONDAY);
            if (chipMasukSel.isChecked()) days.add(AlarmPreferences.TUESDAY);
            if (chipMasukRab.isChecked()) days.add(AlarmPreferences.WEDNESDAY);
            if (chipMasukKam.isChecked()) days.add(AlarmPreferences.THURSDAY);
            if (chipMasukJum.isChecked()) days.add(AlarmPreferences.FRIDAY);
            if (chipMasukSab.isChecked()) days.add(AlarmPreferences.SATURDAY);
            if (chipMasukMin.isChecked()) days.add(AlarmPreferences.SUNDAY);
        } else {
            if (chipPulangSen.isChecked()) days.add(AlarmPreferences.MONDAY);
            if (chipPulangSel.isChecked()) days.add(AlarmPreferences.TUESDAY);
            if (chipPulangRab.isChecked()) days.add(AlarmPreferences.WEDNESDAY);
            if (chipPulangKam.isChecked()) days.add(AlarmPreferences.THURSDAY);
            if (chipPulangJum.isChecked()) days.add(AlarmPreferences.FRIDAY);
            if (chipPulangSab.isChecked()) days.add(AlarmPreferences.SATURDAY);
            if (chipPulangMin.isChecked()) days.add(AlarmPreferences.SUNDAY);
        }
        return days;
    }

    private void saveSettings() {
        // Save masuk settings
        prefs.setEnabled(AlarmPreferences.TYPE_MASUK, switchMasuk.isChecked());
        prefs.setDays(AlarmPreferences.TYPE_MASUK, getSelectedDays(AlarmPreferences.TYPE_MASUK));

        // Save pulang settings
        prefs.setEnabled(AlarmPreferences.TYPE_PULANG, switchPulang.isChecked());
        prefs.setDays(AlarmPreferences.TYPE_PULANG, getSelectedDays(AlarmPreferences.TYPE_PULANG));

        // Save URL
        String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
        if (!url.isEmpty()) {
            prefs.setUrl(url);
        }

        // Schedule/cancel alarms
        if (switchMasuk.isChecked()) {
            AlarmScheduler.scheduleAlarm(this, AlarmPreferences.TYPE_MASUK);
        } else {
            AlarmScheduler.cancelAlarm(this, AlarmPreferences.TYPE_MASUK);
        }

        if (switchPulang.isChecked()) {
            AlarmScheduler.scheduleAlarm(this, AlarmPreferences.TYPE_PULANG);
        } else {
            AlarmScheduler.cancelAlarm(this, AlarmPreferences.TYPE_PULANG);
        }

        updateStatusDisplay();

        Toast.makeText(this, getString(R.string.alarm_disimpan), Toast.LENGTH_SHORT).show();
    }

    private void updateStatusDisplay() {
        // Masuk status
        if (prefs.isEnabled(AlarmPreferences.TYPE_MASUK)) {
            String time = prefs.getTimeFormatted(AlarmPreferences.TYPE_MASUK);
            String days = prefs.getDaysFormatted(AlarmPreferences.TYPE_MASUK);
            tvStatusMasuk.setText("🟢 Alarm Masuk: " + time + " (" + days + ")");
        } else {
            tvStatusMasuk.setText("🔴 Alarm Masuk: Nonaktif");
        }

        // Pulang status
        if (prefs.isEnabled(AlarmPreferences.TYPE_PULANG)) {
            String time = prefs.getTimeFormatted(AlarmPreferences.TYPE_PULANG);
            String days = prefs.getDaysFormatted(AlarmPreferences.TYPE_PULANG);
            tvStatusPulang.setText("🟢 Alarm Pulang: " + time + " (" + days + ")");
        } else {
            tvStatusPulang.setText("🔴 Alarm Pulang: Nonaktif");
        }
    }

    // ==================== PERMISSIONS ====================

    private void requestPermissions() {
        // 1. Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // 2. Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Izin Alarm Tepat Waktu")
                        .setMessage("Aplikasi memerlukan izin untuk menjadwalkan alarm tepat waktu. Buka pengaturan?")
                        .setPositiveButton("Buka Pengaturan", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Nanti", null)
                        .show();
            }
        }

        // 3. Overlay permission (for showing over other apps)
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Izin Tampil di Atas Aplikasi")
                    .setMessage("Alarm perlu izin untuk tampil di atas semua aplikasi agar tidak terlewat. Buka pengaturan?")
                    .setPositiveButton("Buka Pengaturan", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Nanti", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusDisplay();
    }
}

