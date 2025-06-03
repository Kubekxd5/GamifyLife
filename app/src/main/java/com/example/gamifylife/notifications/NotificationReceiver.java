package com.example.gamifylife.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.example.gamifylife.MainActivity; // Aby otworzyć aplikację po kliknięciu
import com.example.gamifylife.MyApplication; // Dla ID kanału
import com.example.gamifylife.NotificationSettingsActivity; // Dla stałych PREF_
import com.example.gamifylife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";
    private static final int NOTIFICATION_ID = 1; // Unikalne ID dla powiadomienia

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationReceiver invoked!");

        // Sprawdź, czy użytkownik jest zalogowany i czy powiadomienia są włączone
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATIONS_ENABLED, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (!notificationsEnabled || currentUser == null) {
            Log.d(TAG, "Notifications disabled or user not logged in. Aborting.");
            // Nie planuj kolejnego, jeśli są wyłączone lub użytkownik wylogowany
            return;
        }

        // Sprawdź, czy dzisiaj jest dzień, na który zaplanowano powiadomienie
        Set<String> selectedDays = prefs.getStringSet(NotificationSettingsActivity.PREF_NOTIFICATION_DAYS, null);
        Calendar today = Calendar.getInstance();
        int currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK); // Calendar.SUNDAY=1, ..., Calendar.SATURDAY=7

        if (selectedDays == null || !selectedDays.contains(String.valueOf(currentDayOfWeek))) {
            Log.d(TAG, "Today is not a selected day for notifications. Aborting this instance.");
            // Ważne: Zaplanuj następne powiadomienie na kolejny pasujący dzień!
            NotificationScheduler.scheduleOrCancelNotifications(context);
            return;
        }


        // Pobierz nieukończone zadania na dzisiaj (lub ogólnie aktywne)
        fetchTasksAndShowNotification(context, currentUser.getUid());

        // Po wyświetleniu powiadomienia, zaplanuj następne
        // Ta logika jest teraz w NotificationScheduler, który jest bardziej inteligentny
        // w znajdowaniu następnego dokładnego czasu.
        NotificationScheduler.scheduleOrCancelNotifications(context);
    }

    private void fetchTasksAndShowNotification(Context context, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        AtomicInteger pendingTasksCount = new AtomicInteger(0);
        String firstPendingTaskTitle = ""; // Domyślnie

        // Zapytanie o nieukończone zadania z dzisiejszą datą docelową
        db.collection("users").document(userId).collection("achievements")
                .whereEqualTo("completed", false)
                .whereGreaterThanOrEqualTo("targetDate", todayStart)
                .whereLessThanOrEqualTo("targetDate", todayEnd)
                .limit(5) // Ogranicz, aby nie pobierać zbyt wielu danych
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        pendingTasksCount.set(queryDocumentSnapshots.size());
                        // Pobierz tytuł pierwszego zadania dla przykładu
                        firstPendingTaskTitle = queryDocumentSnapshots.getDocuments().get(0).getString("title");

                        String notificationTitle = context.getString(R.string.app_name); // Lub bardziej konkretny tytuł
                        String notificationText;
                        if (pendingTasksCount.get() == 1 && firstPendingTaskTitle != null && !firstPendingTaskTitle.isEmpty()) {
                            notificationText = context.getString(R.string.notification_single_task_reminder_detail, firstPendingTaskTitle);
                        } else {
                            notificationText = context.getString(R.string.notification_multiple_tasks_reminder, pendingTasksCount.get());
                        }
                        showNotification(context, notificationTitle, notificationText);
                    } else {
                        Log.d(TAG, "No pending tasks for today to notify about.");
                        // Możesz zdecydować, czy chcesz wysłać ogólne przypomnienie "Sprawdź swoje cele!"
                        // showNotification(context, context.getString(R.string.app_name), context.getString(R.string.notification_general_reminder));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching tasks for notification", e));
    }

    private void showNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Otwórz istniejącą instancję lub nową
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0 /* Request code */,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String soundUriString = prefs.getString(NotificationSettingsActivity.PREF_NOTIFICATION_SOUND_URI, null);
        Uri soundUri = null;
        if (soundUriString != null && !soundUriString.equals("silent")) {
            soundUri = Uri.parse(soundUriString);
        } else if (soundUriString == null) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        boolean vibrate = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATION_VIBRATE, true);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, MyApplication.DAILY_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_gamifylife_notification) // Stwórz tę ikonę! (biała na przezroczystym tle)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true) // Usuń powiadomienie po kliknięciu
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (vibrate) {
            notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
        } else {
            notificationBuilder.setVibrate(null);
        }

        // Sprawdzenie uprawnienia POST_NOTIFICATIONS dla Androida 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
                // W tym miejscu nie możesz prosić o uprawnienie, bo to BroadcastReceiver.
                // Uprawnienie powinno być uzyskane wcześniej w aktywności.
                return;
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        Log.d(TAG, "Notification shown: " + title + " - " + message);
    }

    private Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
}