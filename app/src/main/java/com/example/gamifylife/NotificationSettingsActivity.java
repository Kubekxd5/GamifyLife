package com.example.gamifylife;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.gamifylife.notifications.NotificationScheduler;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotificationSettingsActivity extends BaseActivity {

    private static final String TAG = "NotificationSettings";

    public static final String PREF_NOTIFICATIONS_ENABLED = "pref_notifications_enabled";
    public static final String PREF_NOTIFICATION_HOUR = "pref_notification_hour";
    public static final String PREF_NOTIFICATION_MINUTE = "pref_notification_minute";
    public static final String PREF_NOTIFICATION_DAYS = "pref_notification_days";
    public static final String PREF_NOTIFICATION_SOUND_URI = "pref_notification_sound_uri";
    public static final String PREF_NOTIFICATION_VIBRATE = "pref_notification_vibrate";

    MaterialSwitch switchEnableNotifications;
    private LinearLayout layoutNotificationTime;
    TextView textViewNotificationTimeValue;
    private Button buttonSaveSettings;
    private LinearLayout layoutNotificationSound;
    TextView textViewNotificationSoundValue;
    MaterialSwitch switchNotificationVibrate;
    private LinearLayout actualLayoutCheckboxesDays;

    private CheckBox cbSun, cbMon, cbTue, cbWed, cbThu, cbFri, cbSat; // Zmieniona kolejność dla łatwiejszego mapowania
    CheckBox[] dayCheckBoxes;

    Uri selectedSoundUri;
    SharedPreferences prefs;
    int selectedHour;
    int selectedMinute;

    private ActivityResultLauncher<String> requestPostNotificationsPermissionLauncher;
    private ActivityResultLauncher<Intent> requestExactAlarmSettingsLauncher;

    private final ActivityResultLauncher<Intent> ringtonePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    selectedSoundUri = uri;
                    updateNotificationSoundDisplay();
                    Log.d(TAG, "Ringtone selected: " + (selectedSoundUri != null ? selectedSoundUri.toString() : "Silent"));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        Log.d(TAG, "onCreate started");

        requestPostNotificationsPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            Log.d(TAG, "POST_NOTIFICATIONS permission result: " + isGranted);
            if (isGranted) {
                if (switchEnableNotifications.isChecked()) {
                    savePreferencesAndAttemptSchedule(true); // Spróbuj zapisać i zaplanować po przyznaniu
                }
            } else {
                Toast.makeText(this, "Reminders cannot be shown without notification permission.", Toast.LENGTH_LONG).show();
                if (switchEnableNotifications.isChecked()) {
                    switchEnableNotifications.setChecked(false);
                    prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, false).apply();
                    NotificationScheduler.scheduleOrCancelNotifications(this);
                }
            }
        });

        requestExactAlarmSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Returned from exact alarm settings screen. Attempting to save/schedule again.");
                    // Niezależnie od result, savePreferencesAndAttemptSchedule sprawdzi uprawnienia ponownie
                    // Wywołaj to tylko, jeśli switch był zaznaczony, aby uniknąć niepotrzebnego zapisu
                    if (switchEnableNotifications.isChecked()) {
                        savePreferencesAndAttemptSchedule(true); // true - bo użytkownik próbował włączyć/zapisać
                    }
                }
        );

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
        layoutNotificationSound = findViewById(R.id.layoutNotificationSound);
        textViewNotificationSoundValue = findViewById(R.id.textViewNotificationSoundValue);
        switchNotificationVibrate = findViewById(R.id.switchNotificationVibrate);
        actualLayoutCheckboxesDays = findViewById(R.id.idlayoutCheckboxesDays);

        // Kolejność w XML: Sun, Mon, Tue, Wed, Thu, Fri, Sat
        // Calendar.DAY_OF_WEEK: SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        cbSun = findViewById(R.id.checkboxDaySun);
        cbMon = findViewById(R.id.checkboxDayMon);
        cbTue = findViewById(R.id.checkboxDayTue);
        cbWed = findViewById(R.id.checkboxDayWed);
        cbThu = findViewById(R.id.checkboxDayThu);
        cbFri = findViewById(R.id.checkboxDayFri);
        cbSat = findViewById(R.id.checkboxDaySat);
        dayCheckBoxes = new CheckBox[]{cbSun, cbMon, cbTue, cbWed, cbThu, cbFri, cbSat}; // Indeks 0=Niedz, 1=Pon...

        loadSettings();
        Log.d(TAG, "Settings loaded in onCreate");

        switchEnableNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "switchEnableNotifications changed, isChecked: " + isChecked);
            updateNotificationOptionsState();
            if (isChecked) {
                checkAndRequestPostNotificationPermission(); // To zainicjuje kaskadę sprawdzania uprawnień
            } else {
                // Jeśli użytkownik wyłącza, zapisz i anuluj od razu
                savePreferencesAndAttemptSchedule(false);
            }
        });

        layoutNotificationTime.setOnClickListener(v -> { if (switchEnableNotifications.isChecked()) showTimePickerDialog(); });
        textViewNotificationTimeValue.setOnClickListener(v -> { if (switchEnableNotifications.isChecked()) showTimePickerDialog(); });
        layoutNotificationSound.setOnClickListener(v -> { if (switchEnableNotifications.isChecked()) openRingtonePicker(); });
        buttonSaveSettings.setOnClickListener(v -> savePreferencesAndAttemptSchedule(true)); // true - bo to akcja zapisu
        Log.d(TAG, "onCreate finished");
    }

    private void checkAndRequestPostNotificationPermission() {
        Log.d(TAG, "checkAndRequestPostNotificationPermission called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission NOT granted. Requesting...");
                requestPostNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission ALREADY granted.");
                // Jeśli to uprawnienie jest, a switch jest włączony, możemy przejść do sprawdzania SCHEDULE_EXACT_ALARM
                // To się stanie w savePreferencesAndAttemptSchedule
            }
        }
    }

    // Dodano parametr `isSavingExplicitly`, aby odróżnić automatyczne wywołania od kliknięcia "Save"
    void savePreferencesAndAttemptSchedule(boolean isSavingExplicitly) {
        Log.d(TAG, "savePreferencesAndAttemptSchedule called. isSavingExplicitly: " + isSavingExplicitly);
        SharedPreferences.Editor editor = prefs.edit();
        boolean intendedEnableState = switchEnableNotifications.isChecked();
        boolean finalCanEnable = intendedEnableState;
        Log.d(TAG, "Initial intendedEnableState: " + intendedEnableState);

        // 1. Sprawdź POST_NOTIFICATIONS
        if (intendedEnableState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission still not granted. Disabling reminders.");
                finalCanEnable = false;
                // Jeśli to nie jest jawny zapis, a switch był włączony, odznacz go
                if (!isSavingExplicitly && switchEnableNotifications.isChecked()) {
                    switchEnableNotifications.setChecked(false);
                }
            }
        }

        // 2. Sprawdź SCHEDULE_EXACT_ALARM - tylko jeśli nadal chcemy włączyć
        if (finalCanEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted.");
                if (intendedEnableState) { // Pokaż dialog tylko, jeśli użytkownik aktywnie próbuje włączyć
                    Log.d(TAG, "Showing SCHEDULE_EXACT_ALARM permission dialog.");
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.exact_alarm_permission_title))
                            .setMessage(getString(R.string.exact_alarm_permission_needed_dialog_message))
                            .setPositiveButton(getString(R.string.go_to_settings_button), (dialog, which) -> {
                                Log.d(TAG, "User clicked 'Go to Settings' for exact alarms.");
                                try {
                                    Intent intentSettings = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                    requestExactAlarmSettingsLauncher.launch(intentSettings);
                                } catch (Exception e) {
                                    Log.e(TAG, "Could not open exact alarm settings from dialog", e);
                                    Toast.makeText(this, getString(R.string.exact_alarm_permission_needed), Toast.LENGTH_LONG).show();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                                Log.d(TAG, "User cancelled exact alarm permission dialog.");
                                Toast.makeText(NotificationSettingsActivity.this, "Precise reminders disabled as permission was not granted.", Toast.LENGTH_SHORT).show();
                                if (switchEnableNotifications.isChecked()) {
                                    switchEnableNotifications.setChecked(false);
                                    // Zapisz stan natychmiast, bo użytkownik podjął decyzję
                                    prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, false).apply();
                                    NotificationScheduler.scheduleOrCancelNotifications(NotificationSettingsActivity.this);
                                }
                            })
                            .setCancelable(false)
                            .show();
                    return; // Czekaj na interakcję użytkownika
                }
                finalCanEnable = false; // Nie można włączyć bez tego uprawnienia
            } else if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null, cannot check canScheduleExactAlarms. Disabling reminders.");
                finalCanEnable = false;
            }
        }

        // Ostatecznie zaktualizuj UI switcha, jeśli logika uprawnień go zmieniła
        if (switchEnableNotifications.isChecked() != finalCanEnable) {
            Log.d(TAG, "Switch state (" + switchEnableNotifications.isChecked() + ") differs from finalCanEnable (" + finalCanEnable + "). Updating switch.");
            switchEnableNotifications.setChecked(finalCanEnable);
        }

        Log.d(TAG, "Saving preferences. Final PREF_NOTIFICATIONS_ENABLED: " + finalCanEnable);
        editor.putBoolean(PREF_NOTIFICATIONS_ENABLED, finalCanEnable);
        editor.putInt(PREF_NOTIFICATION_HOUR, selectedHour);
        editor.putInt(PREF_NOTIFICATION_MINUTE, selectedMinute);
        Set<String> daysToSave = new HashSet<>();
        for (int i = 0; i < dayCheckBoxes.length; i++) {
            if (dayCheckBoxes[i].isChecked()) {
                // dayCheckBoxes[0] to Niedziela (Calendar.SUNDAY = 1)
                // dayCheckBoxes[1] to Poniedziałek (Calendar.MONDAY = 2) ...
                daysToSave.add(String.valueOf(i + 1));
            }
        }
        Log.d(TAG, "Saving days: " + daysToSave);
        editor.putStringSet(PREF_NOTIFICATION_DAYS, daysToSave);

        if (selectedSoundUri != null) {
            editor.putString(PREF_NOTIFICATION_SOUND_URI, selectedSoundUri.toString());
        } else {
            editor.putString(PREF_NOTIFICATION_SOUND_URI, "silent");
        }
        editor.putBoolean(PREF_NOTIFICATION_VIBRATE, switchNotificationVibrate.isChecked());
        editor.apply();

        if (isSavingExplicitly) { // Pokaż toast tylko przy jawnym zapisie
            if (finalCanEnable) {
                Toast.makeText(this, R.string.notification_settings_saved, Toast.LENGTH_SHORT).show();
            } else {
                // Jeśli finalCanEnable jest false, ale intendedEnableState było true,
                // oznacza to, że uprawnienia przeszkodziły. Toast o tym był już pokazany.
                if (!intendedEnableState) { // Użytkownik sam wyłączył
                    Toast.makeText(this, getString(R.string.notification_settings_saved) + " (Reminders disabled)", Toast.LENGTH_SHORT).show();
                }
            }
        }
        Log.d(TAG, "Calling NotificationScheduler.scheduleOrCancelNotifications.");
        NotificationScheduler.scheduleOrCancelNotifications(this);
    }

    private void updateNotificationOptionsState() {
        boolean enabled = switchEnableNotifications.isChecked();
        Log.d(TAG, "updateNotificationOptionsState, enabled: " + enabled);
        layoutNotificationTime.setEnabled(enabled);
        textViewNotificationTimeValue.setEnabled(enabled);
        layoutNotificationSound.setEnabled(enabled);
        textViewNotificationSoundValue.setEnabled(enabled);
        switchNotificationVibrate.setEnabled(enabled);

        for (CheckBox cb : dayCheckBoxes) {
            cb.setEnabled(enabled);
        }

        float alpha = enabled ? 1.0f : 0.5f;
        layoutNotificationTime.setAlpha(alpha);
        layoutNotificationSound.setAlpha(alpha);
        switchNotificationVibrate.setAlpha(alpha);
        if (actualLayoutCheckboxesDays != null) {
            actualLayoutCheckboxesDays.setAlpha(alpha);
        } else {
            Log.w(TAG, "actualLayoutCheckboxesDays is null in updateNotificationOptionsState");
        }
    }

    void loadSettings() {
        Log.d(TAG, "loadSettings started");
        switchEnableNotifications.setChecked(prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false));
        selectedHour = prefs.getInt(PREF_NOTIFICATION_HOUR, 9);
        selectedMinute = prefs.getInt(PREF_NOTIFICATION_MINUTE, 0);
        updateNotificationTimeDisplay();

        Set<String> selectedDaysPrefs = prefs.getStringSet(PREF_NOTIFICATION_DAYS, getDefaultSelectedDays());
        Log.d(TAG, "Loaded days from prefs: " + selectedDaysPrefs);
        for (int i = 0; i < dayCheckBoxes.length; i++) {
            if (dayCheckBoxes[i] != null) {
                // Indeks i w dayCheckBoxes (0-6) odpowiada Calendar.DAY_OF_WEEK - 1 (0=Niedz, 1=Pon...)
                // Wartość w selectedDaysPrefs to String.valueOf(Calendar.DAY_OF_WEEK) (1-7)
                dayCheckBoxes[i].setChecked(selectedDaysPrefs.contains(String.valueOf(i + 1)));
            }
        }
        loadSoundSettings();
        updateNotificationOptionsState(); // Ważne, aby było po załadowaniu wszystkiego
        Log.d(TAG, "loadSettings finished. Initial switch state: " + switchEnableNotifications.isChecked());
    }

    private Set<String> getDefaultSelectedDays() {
        Set<String> defaultDays = new HashSet<>();
        for (int i = 1; i <= 7; i++) { // Calendar.SUNDAY to Calendar.SATURDAY
            defaultDays.add(String.valueOf(i));
        }
        Log.d(TAG, "getDefaultSelectedDays: " + defaultDays);
        return defaultDays;
    }

    private void updateNotificationTimeDisplay() {
        if (textViewNotificationTimeValue != null) {
            textViewNotificationTimeValue.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
        }
    }

    private void showTimePickerDialog() {
        Log.d(TAG, "showTimePickerDialog called");
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    Log.d(TAG, "Time picked: " + hourOfDay + ":" + minute);
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateNotificationTimeDisplay();
                }, selectedHour, selectedMinute, true);
        timePickerDialog.show();
    }

    private void loadSoundSettings() {
        Log.d(TAG, "loadSoundSettings started");
        String soundUriString = prefs.getString(PREF_NOTIFICATION_SOUND_URI, null);
        if (soundUriString != null) {
            if (soundUriString.equals("silent")) {
                selectedSoundUri = null;
            } else {
                selectedSoundUri = Uri.parse(soundUriString);
            }
        } else {
            selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (switchNotificationVibrate != null) {
            switchNotificationVibrate.setChecked(prefs.getBoolean(PREF_NOTIFICATION_VIBRATE, true));
        }
        updateNotificationSoundDisplay();
        Log.d(TAG, "loadSoundSettings finished. Sound: " + (selectedSoundUri != null ? selectedSoundUri.toString() : "Silent") + ", Vibrate: " + switchNotificationVibrate.isChecked());
    }

    private void updateNotificationSoundDisplay() {
        if (textViewNotificationSoundValue == null) return;
        if (selectedSoundUri != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(this, selectedSoundUri);
            if (ringtone != null) {
                textViewNotificationSoundValue.setText(ringtone.getTitle(this));
            } else {
                textViewNotificationSoundValue.setText(getString(R.string.sound_default));
                selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone defaultRingtone = RingtoneManager.getRingtone(this, selectedSoundUri);
                if (defaultRingtone != null) {
                    textViewNotificationSoundValue.setText(defaultRingtone.getTitle(this));
                }
            }
        } else {
            textViewNotificationSoundValue.setText(getString(R.string.sound_silent));
        }
    }

    private void openRingtonePicker() {
        Log.d(TAG, "openRingtonePicker called");
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri);
        ringtonePickerLauncher.launch(intent);
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