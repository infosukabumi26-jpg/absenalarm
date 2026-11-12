package com.appdroid.absenalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages alarm settings in SharedPreferences.
 * Stores separate settings for "masuk" (clock-in) and "pulang" (clock-out) alarms.
 */
public class AlarmPreferences {

    private static final String PREF_NAME = "absen_alarm_prefs";
    private static final String KEY_ENABLED = "_enabled";
    private static final String KEY_HOUR = "_hour";
    private static final String KEY_MINUTE = "_minute";
    private static final String KEY_DAYS = "_days";
    private static final String KEY_RINGTONE = "_ringtone";
    private static final String KEY_RINGTONE_NAME = "_ringtone_name";
    private static final String KEY_URL = "absen_url";

    public static final String TYPE_MASUK = "masuk";
    public static final String TYPE_PULANG = "pulang";

    // Day constants matching java.util.Calendar
    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    private final SharedPreferences prefs;

    public AlarmPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setEnabled(String type, boolean enabled) {
        prefs.edit().putBoolean(type + KEY_ENABLED, enabled).apply();
    }

    public boolean isEnabled(String type) {
        return prefs.getBoolean(type + KEY_ENABLED, false);
    }

    public void setTime(String type, int hour, int minute) {
        prefs.edit()
                .putInt(type + KEY_HOUR, hour)
                .putInt(type + KEY_MINUTE, minute)
                .apply();
    }

    public int getHour(String type) {
        int defaultHour = TYPE_MASUK.equals(type) ? 7 : 16;
        return prefs.getInt(type + KEY_HOUR, defaultHour);
    }

    public int getMinute(String type) {
        int defaultMinute = TYPE_MASUK.equals(type) ? 30 : 0;
        return prefs.getInt(type + KEY_MINUTE, defaultMinute);
    }

    public void setDays(String type, Set<Integer> days) {
        Set<String> dayStrings = new HashSet<>();
        for (Integer day : days) {
            dayStrings.add(String.valueOf(day));
        }
        prefs.edit().putStringSet(type + KEY_DAYS, dayStrings).apply();
    }

    public Set<Integer> getDays(String type) {
        // Default: Monday to Friday
        Set<String> defaultDays = new HashSet<>();
        defaultDays.add(String.valueOf(MONDAY));
        defaultDays.add(String.valueOf(TUESDAY));
        defaultDays.add(String.valueOf(WEDNESDAY));
        defaultDays.add(String.valueOf(THURSDAY));
        defaultDays.add(String.valueOf(FRIDAY));

        Set<String> dayStrings = prefs.getStringSet(type + KEY_DAYS, defaultDays);
        Set<Integer> days = new HashSet<>();
        if (dayStrings != null) {
            for (String s : dayStrings) {
                try {
                    days.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {}
            }
        }
        return days;
    }

    public void setRingtoneUri(String type, String uri) {
        prefs.edit().putString(type + KEY_RINGTONE, uri).apply();
    }

    public Uri getRingtoneUri(String type) {
        String uri = prefs.getString(type + KEY_RINGTONE, null);
        if (uri != null) {
            return Uri.parse(uri);
        }
        // Default alarm ringtone
        Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (defaultUri == null) {
            defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return defaultUri;
    }

    public void setRingtoneName(String type, String name) {
        prefs.edit().putString(type + KEY_RINGTONE_NAME, name).apply();
    }

    public String getRingtoneName(String type) {
        return prefs.getString(type + KEY_RINGTONE_NAME, "Nada Default");
    }

    public void setUrl(String url) {
        prefs.edit().putString(KEY_URL, url).apply();
    }

    public String getUrl() {
        return prefs.getString(KEY_URL, "https://star-asn.kemenimipas.go.id/authentication/login");
    }

    public String getTimeFormatted(String type) {
        return String.format("%02d:%02d", getHour(type), getMinute(type));
    }

    public String getDaysFormatted(String type) {
        Set<Integer> days = getDays(type);
        String[] names = {"", "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i <= 7; i++) { // Mon to Sat
            if (days.contains(i)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(names[i]);
            }
        }
        if (days.contains(1)) { // Sunday
            if (sb.length() > 0) sb.append(", ");
            sb.append(names[1]);
        }
        return sb.length() > 0 ? sb.toString() : "Tidak ada hari dipilih";
    }
}

