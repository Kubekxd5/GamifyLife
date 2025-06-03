package com.example.gamifylife; // lub twój pakiet dla aktywności

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceManager; // Jeśli używasz androidx.preference

import com.google.android.material.materialswitch.MaterialSwitch; // Poprawny import

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotificationSettingsActivity extends BaseActivity {

    public static final String PREF_NOTIFICATIONS_ENABLED = "pref_notifications_enabled";
    public static final String PREF_NOTIFICATION_HOUR = "pref_notification_hour";
    public static final String PREF_NOTIFICATION_MINUTE = "pref_notification_minute";
    public static final String PREF_NOTIFICATION_DAYS = "pref_notification_days"; // Set<String>

    private MaterialSwitch switchEnableNotifications;
    private LinearLayout layoutNotificationTime;
    private TextView textViewNotificationTimeValue;
    private Button buttonSaveSettings;

    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private CheckBox[] dayCheckBoxes;


    private SharedPreferences prefs;
    private int selectedHour, selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_activity_notification_settings));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        switchEnableNotifications = findViewById(R.id.switchEnableNotifications);
        layoutNotificationTime = findViewById(R.id.layoutNotificationTime);
        textViewNotificationTimeValue = findViewById(R.id.textViewNotificationTimeValue);
        buttonSaveSettings = findViewById(R.id.buttonSaveNotificationSettings);

        cbMon = findViewById(R.id.checkboxDayMon);
        cbTue = findViewById(R.id.checkboxDayTue);
        cbWed = findViewById(R.id.checkboxDayWed);
        cbThu = findViewById(R.id.checkboxDayThu);
        cbFri = findViewById(R.id.checkboxDayFri);
        cbSat = findViewById(R.id.checkboxDaySat);
        cbSun = findViewById(R.id.checkboxDaySun);
        dayCheckBoxes = new CheckBox[]{cbSun, cbMon, cbTue, cbWed, cbThu, cbFri, cbSat}; // Kolejność Calendar.DAY_OF_WEEK

        loadSettings();
        updateNotificationTimeState(); // Update visibility based on switch

        switchEnableNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationTimeState();
        });

        layoutNotificationTime.setOnClickListener(v -> showTimePickerDialog());
        textViewNotificationTimeValue.setOnClickListener(v -> showTimePickerDialog()); // Allow clicking text too

        buttonSaveSettings.setOnClickListener(v -> saveSettingsAndSchedule());
    }

    private void updateNotificationTimeState() {
        boolean enabled = switchEnableNotifications.isChecked();
        layoutNotificationTime.setEnabled(enabled);
        textViewNotificationTimeValue.setEnabled(enabled);
        for (CheckBox cb : dayCheckBoxes) {
            cb.setEnabled(enabled);
        }
        // Można też zmienić alpha, aby wizualnie pokazać, że opcje są nieaktywne
        float alpha = enabled ? 1.0f : 0.5f;
        layoutNotificationTime.setAlpha(alpha);
        findViewById(R.id.checkboxDayMon).getRootView().setAlpha(alpha); // Apply to parent of checkboxes
    }


    private void loadSettings() {
        switchEnableNotifications.setChecked(prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false));
        selectedHour = prefs.getInt(PREF_NOTIFICATION_HOUR, 9); // Domyślnie 9
        selectedMinute = prefs.getInt(PREF_NOTIFICATION_MINUTE, 0); // Domyślnie 00
        updateNotificationTimeDisplay();

        Set<String> selectedDaysPrefs = prefs.getStringSet(PREF_NOTIFICATION_DAYS, getDefaultSelectedDays());
        for (int i = 0; i < dayCheckBoxes.length; i++) {
            // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
            // Nasza tablica dayCheckBoxes[0] to niedziela, dayCheckBoxes[1] to poniedziałek itd.
            // Konwertujemy i+1 na string, aby pasowało do wartości Calendar.DAY_OF_WEEK
            if (selectedDaysPrefs.contains(String.valueOf(i + 1))) {
                dayCheckBoxes[i].setChecked(true);
            }
        }
    }

    private Set<String> getDefaultSelectedDays() {
        // Domyślnie zaznacz wszystkie dni
        Set<String> defaultDays = new HashSet<>();
        for (int i = 1; i <= 7; i++) { // Od Calendar.SUNDAY do Calendar.SATURDAY
            defaultDays.add(String.valueOf(i));
        }
        return defaultDays;
    }


    private void updateNotificationTimeDisplay() {
        textViewNotificationTimeValue.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateNotificationTimeDisplay();
                }, selectedHour, selectedMinute, true); // true dla formatu 24-godzinnego
        timePickerDialog.show();
    }

    private void saveSettingsAndSchedule() {
        SharedPreferences.Editor editor = prefs.edit();
        boolean enabled = switchEnableNotifications.isChecked();
        editor.putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled);
        editor.putInt(PREF_NOTIFICATION_HOUR, selectedHour);
        editor.putInt(PREF_NOTIFICATION_MINUTE, selectedMinute);

        Set<String> daysToSave = new HashSet<>();
        for (int i = 0; i < dayCheckBoxes.length; i++) {
            if (dayCheckBoxes[i].isChecked()) {
                daysToSave.add(String.valueOf(i + 1)); // Calendar.DAY_OF_WEEK values
            }
        }
        editor.putStringSet(PREF_NOTIFICATION_DAYS, daysToSave);

        editor.apply();

        if (enabled) {
            // Tutaj wywołaj metodę planującą powiadomienia z AlarmManager
            // NotificationScheduler.scheduleNotifications(this); // Stworzymy tę klasę
            Toast.makeText(this, R.string.notification_settings_saved, Toast.LENGTH_SHORT).show();
            // Na razie tylko toast, scheduling w następnym kroku
            Log.d("NotificationSettings", "Scheduling would happen here for " + selectedHour + ":" + selectedMinute + " on days: " + daysToSave);
            //com.example.gamifylife.notifications.NotificationScheduler.scheduleOrCancelNotifications(this); // Odwołanie do NotificationScheduler
        } else {
            // Tutaj wywołaj metodę anulującą powiadomienia
            // NotificationScheduler.cancelNotifications(this);
            Toast.makeText(this, getString(R.string.notification_settings_saved) + " (Reminders disabled)", Toast.LENGTH_SHORT).show();
            //com.example.gamifylife.notifications.NotificationScheduler.scheduleOrCancelNotifications(this); // Odwołanie do NotificationScheduler
        }
        finish(); // Wróć do SettingsFragment
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}