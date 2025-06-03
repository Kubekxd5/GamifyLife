package com.example.gamifylife.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast; // Opcjonalnie dla debugowania

import androidx.preference.PreferenceManager;

import com.example.gamifylife.NotificationSettingsActivity; // Dla stałych PREF_
import com.example.gamifylife.R; // Dla stringów

import java.util.Calendar;
import java.util.Set;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    public static final int DAILY_REMINDER_REQUEST_CODE = 1001;

    public static void scheduleOrCancelNotifications(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean areNotificationsEnabled = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATIONS_ENABLED, false);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        // Użyj FLAG_IMMUTABLE, ponieważ zawartość Intent się nie zmienia
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (areNotificationsEnabled) {
            int hour = prefs.getInt(NotificationSettingsActivity.PREF_NOTIFICATION_HOUR, 9);
            int minute = prefs.getInt(NotificationSettingsActivity.PREF_NOTIFICATION_MINUTE, 0);
            Set<String> selectedDays = prefs.getStringSet(NotificationSettingsActivity.PREF_NOTIFICATION_DAYS, null);

            if (selectedDays == null || selectedDays.isEmpty()) {
                Log.w(TAG, "Notifications enabled but no days selected. Cancelling any existing alarms.");
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
                return;
            }

            // Znajdź następny zaplanowany czas
            Calendar nextNotificationTime = getNextNotificationCalendar(hour, minute, selectedDays);

            if (nextNotificationTime == null) {
                Log.w(TAG, "Could not determine next notification time (no valid days selected or all in past for today?). Cancelling alarms.");
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
                return;
            }


            if (alarmManager != null) {
                try {
                    // Dla Androida 12+ SCHEDULE_EXACT_ALARM jest potrzebne dla setExactAndAllowWhileIdle
                    // Rozważ użycie setWindow lub setInexactRepeating dla mniejszego zużycia baterii,
                    // jeśli precyzja co do minuty nie jest absolutnie krytyczna.
                    // Na razie użyjemy setExactAndAllowWhileIdle dla większej precyzji.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextNotificationTime.getTimeInMillis(), pendingIntent);
                            Log.i(TAG, "Exact alarm scheduled for: " + nextNotificationTime.getTime().toString());
                        } else {
                            Log.w(TAG, "Cannot schedule exact alarms. User needs to grant permission or use inexact.");
                            // Można tu ustawić inexact alarm jako fallback lub poinformować użytkownika
                            // alarmManager.setWindow(AlarmManager.RTC_WAKEUP, nextNotificationTime.getTimeInMillis(), 60000, pendingIntent); // Okno 1 minuty
                            Toast.makeText(context, context.getString(R.string.exact_alarm_permission_needed), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextNotificationTime.getTimeInMillis(), pendingIntent);
                        Log.i(TAG, "Alarm scheduled for: " + nextNotificationTime.getTime().toString());
                    }
                    //Toast.makeText(context, "Reminder set for: " + nextNotificationTime.getTime().toString(), Toast.LENGTH_LONG).show(); // For debugging
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException: Missing SCHEDULE_EXACT_ALARM permission?", se);
                    Toast.makeText(context, context.getString(R.string.exact_alarm_permission_missing_error), Toast.LENGTH_LONG).show(); // Dodaj stringa
                }
            }
        } else {
            // Anuluj alarm, jeśli powiadomienia są wyłączone
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.i(TAG, "Daily reminders cancelled.");
                //Toast.makeText(context, "Reminders cancelled.", Toast.LENGTH_SHORT).show(); // For debugging
            }
        }
    }

    // Ta metoda będzie musiała być bardziej inteligentna, aby znaleźć *następny* pasujący dzień i godzinę
    private static Calendar getNextNotificationCalendar(int hour, int minute, Set<String> selectedDays) {
        Calendar now = Calendar.getInstance();
        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.set(Calendar.HOUR_OF_DAY, hour);
        nextAlarm.set(Calendar.MINUTE, minute);
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.set(Calendar.MILLISECOND, 0);

        if (selectedDays == null || selectedDays.isEmpty()) return null;

        // Sprawdź 7 kolejnych dni, zaczynając od dzisiaj
        for (int i = 0; i < 7; i++) {
            Calendar potentialAlarmDay = (Calendar) now.clone();
            potentialAlarmDay.add(Calendar.DAY_OF_YEAR, i); // Przesuń o 'i' dni

            int dayOfWeek = potentialAlarmDay.get(Calendar.DAY_OF_WEEK); // Calendar.SUNDAY=1, ..., Calendar.SATURDAY=7

            if (selectedDays.contains(String.valueOf(dayOfWeek))) {
                // Ten dzień tygodnia jest zaznaczony
                Calendar currentCandidate = (Calendar) potentialAlarmDay.clone();
                currentCandidate.set(Calendar.HOUR_OF_DAY, hour);
                currentCandidate.set(Calendar.MINUTE, minute);
                currentCandidate.set(Calendar.SECOND, 0);
                currentCandidate.set(Calendar.MILLISECOND, 0);

                if (currentCandidate.after(now)) {
                    // Znaleziono najbliższy przyszły termin
                    Log.d(TAG, "Next alarm determined for: " + currentCandidate.getTime().toString());
                    return currentCandidate;
                }
            }
        }
        // Jeśli pętla się zakończyła, oznacza to, że wszystkie zaznaczone dni w tym tygodniu już minęły
        // lub dzisiaj jest zaznaczony dzień, ale godzina już minęła.
        // W takim przypadku znajdź pierwszy zaznaczony dzień w przyszłym tygodniu.
        for (int i = 1; i <= 7; i++) { // Szukaj od jutra w następnym tygodniu
            Calendar nextWeekCandidate = Calendar.getInstance();
            nextWeekCandidate.add(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_WEEK) <= i ? (i - now.get(Calendar.DAY_OF_WEEK) + 7) : (i - now.get(Calendar.DAY_OF_WEEK)));
            nextWeekCandidate.add(Calendar.WEEK_OF_YEAR, 1); // Upewnij się, że to przyszły tydzień
            int dayOfWeek = nextWeekCandidate.get(Calendar.DAY_OF_WEEK);
            if (selectedDays.contains(String.valueOf(dayOfWeek))) {
                nextWeekCandidate.set(Calendar.HOUR_OF_DAY, hour);
                nextWeekCandidate.set(Calendar.MINUTE, minute);
                nextWeekCandidate.set(Calendar.SECOND, 0);
                nextWeekCandidate.set(Calendar.MILLISECOND, 0);
                Log.d(TAG, "Next alarm determined for next week: " + nextWeekCandidate.getTime().toString());
                return nextWeekCandidate;
            }
        }


        Log.w(TAG, "Could not find a suitable next alarm time. This shouldn't happen if days are selected.");
        return null; // Nie powinno się zdarzyć, jeśli jakieś dni są zaznaczone
    }


    // Metoda do anulowania wszystkich powiadomień (przydatna przy resecie ustawień)
    public static void cancelAllNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, com.example.gamifylife.notifications.NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE, aby tylko sprawdzić i anulować
        );

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel(); // Również anuluj PendingIntent
            Log.i(TAG, "All daily reminders cancelled.");
        }
    }
}