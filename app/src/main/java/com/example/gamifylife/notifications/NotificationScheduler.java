package com.example.gamifylife.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.example.gamifylife.NotificationSettingsActivity;
import com.example.gamifylife.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    public static final int DAILY_REMINDER_REQUEST_CODE = 1001;
    private static final SimpleDateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


    public static void scheduleOrCancelNotifications(Context context) {
        // ... (reszta metody scheduleOrCancelNotifications pozostaje bez zmian jak w poprzedniej wersji) ...
        Log.d(TAG, "scheduleOrCancelNotifications called");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean areNotificationsEnabled = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATIONS_ENABLED, false);
        Log.d(TAG, "Notifications enabled in prefs: " + areNotificationsEnabled);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule or cancel notifications.");
            return;
        }

        if (areNotificationsEnabled) {
            int hour = prefs.getInt(NotificationSettingsActivity.PREF_NOTIFICATION_HOUR, 9);
            int minute = prefs.getInt(NotificationSettingsActivity.PREF_NOTIFICATION_MINUTE, 0);
            Set<String> selectedDaysPref = prefs.getStringSet(NotificationSettingsActivity.PREF_NOTIFICATION_DAYS, null);
            Log.d(TAG, "Prefs: Hour=" + hour + ", Minute=" + minute + ", SelectedDays=" + selectedDaysPref);

            if (selectedDaysPref == null || selectedDaysPref.isEmpty()) {
                Log.w(TAG, "Notifications enabled but no days selected. Cancelling existing alarms.");
                alarmManager.cancel(pendingIntent);
                return;
            }

            Calendar nextNotificationTime = getNextNotificationCalendar(hour, minute, selectedDaysPref);

            if (nextNotificationTime == null) {
                Log.w(TAG, "getNextNotificationCalendar returned null. Cancelling alarms.");
                alarmManager.cancel(pendingIntent);
                return;
            }

            Log.d(TAG, "Calculated nextNotificationTime: " + DEBUG_DATE_FORMAT.format(nextNotificationTime.getTime()));
            Log.d(TAG, "Current time: " + DEBUG_DATE_FORMAT.format(Calendar.getInstance().getTime()));
            Log.d(TAG, "Next notification time in millis: " + nextNotificationTime.getTimeInMillis());

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextNotificationTime.getTimeInMillis(), pendingIntent);
                        Log.i(TAG, "Exact alarm (API 31+) scheduled for: " + DEBUG_DATE_FORMAT.format(nextNotificationTime.getTime()));
                    } else {
                        Log.w(TAG, "Cannot schedule exact alarms on API 31+. User needs to grant permission or app needs to use inexact alarms. Alarm NOT set by this logic branch.");
                    }
                } else { // Poniżej Androida S (Android 12)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6+
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextNotificationTime.getTimeInMillis(), pendingIntent);
                    } else { // Poniżej Androida M (Android 6)
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextNotificationTime.getTimeInMillis(), pendingIntent);
                    }
                    Log.i(TAG, "Alarm scheduled (pre-S) for: " + DEBUG_DATE_FORMAT.format(nextNotificationTime.getTime()));
                }
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException while scheduling alarm.", se);
                Toast.makeText(context, context.getString(R.string.exact_alarm_permission_missing_error), Toast.LENGTH_LONG).show();
            }
        } else {
            alarmManager.cancel(pendingIntent);
            Log.i(TAG, "Daily reminders explicitly cancelled by user setting or permissions.");
        }
    }

    private static Calendar getNextNotificationCalendar(int hour, int minute, Set<String> selectedDaysPref) {
        Calendar now = Calendar.getInstance();
        Log.d(TAG, "getNextNotificationCalendar: Now = " + DEBUG_DATE_FORMAT.format(now.getTime()) +
                ", Desired H:M = " + hour + ":" + minute + ", SelectedDaysPref = " + selectedDaysPref);

        List<Integer> selectedDaysOfWeekInt = new ArrayList<>();
        if (selectedDaysPref != null) {
            for (String dayStr : selectedDaysPref) {
                try {
                    selectedDaysOfWeekInt.add(Integer.parseInt(dayStr)); // Calendar.SUNDAY=1, MONDAY=2, etc.
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid day string in preferences: " + dayStr);
                }
            }
        }
        Collections.sort(selectedDaysOfWeekInt); // Sortowanie pomaga w znalezieniu następnego dnia
        Log.d(TAG, "Parsed and sorted selectedDaysOfWeekInt: " + selectedDaysOfWeekInt);

        if (selectedDaysOfWeekInt.isEmpty()) {
            Log.w(TAG, "No days selected, returning null.");
            return null;
        }

        // Pętla sprawdzająca 7 kolejnych dni, zaczynając od dzisiaj
        for (int i = 0; i < 7; i++) {
            Calendar candidateTime = (Calendar) now.clone();
            if (i > 0) { // Dla i > 0, przechodzimy do następnego dnia
                candidateTime.add(Calendar.DAY_OF_YEAR, i);
            }
            // Ustawiamy żądaną godzinę i minutę dla dnia kandydata
            candidateTime.set(Calendar.HOUR_OF_DAY, hour);
            candidateTime.set(Calendar.MINUTE, minute);
            candidateTime.set(Calendar.SECOND, 0);
            candidateTime.set(Calendar.MILLISECOND, 0);

            int candidateDayOfWeek = candidateTime.get(Calendar.DAY_OF_WEEK);
            Log.d(TAG, "Checking candidate day (offset " + i + "): " + DEBUG_DATE_FORMAT.format(candidateTime.getTime()) +
                    " (DayOfWeek: " + candidateDayOfWeek + ")");

            // Sprawdź, czy ten dzień tygodnia jest wybrany PRZEZ UŻYTKOWNIKA
            // ORAZ czy obliczony czas kandydata jest W PRZYSZŁOŚCI względem 'now'
            if (selectedDaysOfWeekInt.contains(candidateDayOfWeek) && candidateTime.after(now)) {
                Log.i(TAG, "-> Found next alarm: " + DEBUG_DATE_FORMAT.format(candidateTime.getTime()));
                return candidateTime;
            }
        }

        // Jeśli pętla przeszła przez 7 dni i nic nie znalazła (co nie powinno się zdarzyć, jeśli jakiś dzień jest wybrany
        // i logika jest poprawna), oznacza to, że wszystkie możliwe terminy w tym "oknie" 7 dni już minęły.
        // W takim przypadku musimy znaleźć pierwszy dostępny termin w "następnym cyklu" wybranych dni.
        // To jest bardziej skomplikowane, ale obecna pętla powinna znaleźć najbliższy przyszły.
        // Jeśli powyższa pętla zawiedzie, ten log pomoże to zdiagnozować.
        Log.w(TAG, "Could not find a suitable future alarm time within the next 7 day checks. " +
                "This might indicate an issue if days are selected or all selected times are in the past " +
                "without a wrap-around to the next week being effectively found by the 7-day loop.");
        return null;
    }

    public static void cancelAllNotifications(Context context) {
        // ... (bez zmian) ...
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null in cancelAllNotifications.");
            return;
        }
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.i(TAG, "All daily reminders cancelled via cancelAllNotifications.");
        } else {
            Log.w(TAG, "PendingIntent for cancellation was null (alarm might not have been set).");
        }
    }
}