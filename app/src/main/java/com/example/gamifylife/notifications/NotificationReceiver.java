package com.example.gamifylife.notifications;

import android.Manifest; // Dodaj ten import
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

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat; // Dodaj ten import
import androidx.preference.PreferenceManager;

import com.example.gamifylife.MainActivity;
import com.example.gamifylife.util.MyApplication;
import com.example.gamifylife.NotificationSettingsActivity;
import com.example.gamifylife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Import dla Query
import com.google.firebase.firestore.QueryDocumentSnapshot; // Import dla QueryDocumentSnapshot


import java.text.SimpleDateFormat; // Dla formatowania daty w logach
import java.util.Calendar;
import java.util.Date;
import java.util.Locale; // Dla formatowania daty w logach
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";
    private static final int NOTIFICATION_ID = 1;
    private static final SimpleDateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive CALLED! Current time: " + DEBUG_DATE_FORMAT.format(new Date()));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATIONS_ENABLED, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Prefs: notificationsEnabled=" + notificationsEnabled + ", currentUser=" + (currentUser != null ? currentUser.getUid() : "null"));

        if (!notificationsEnabled || currentUser == null) {
            Log.w(TAG, "Notifications disabled or user not logged in. Aborting onReceive.");
            return;
        }

        Set<String> selectedDays = prefs.getStringSet(NotificationSettingsActivity.PREF_NOTIFICATION_DAYS, null);
        Calendar today = Calendar.getInstance();
        int currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        Log.d(TAG, "Prefs: selectedDays=" + selectedDays + ", currentDayOfWeek=" + currentDayOfWeek + " (1=Sun, 7=Sat)");

        if (selectedDays == null || !selectedDays.contains(String.valueOf(currentDayOfWeek))) {
            Log.i(TAG, "Today (" + currentDayOfWeek + ") is not a selected day for notifications. Rescheduling for next valid day.");
            NotificationScheduler.scheduleOrCancelNotifications(context); // Zaplanuj na następny poprawny termin
            return;
        }

        Log.d(TAG, "Proceeding to fetch tasks and show notification.");
        fetchTasksAndShowNotification(context, currentUser.getUid());

        // Po obsłużeniu obecnego alarmu, zaplanuj następny.
        Log.d(TAG, "Rescheduling for the next occurrence after this one.");
        NotificationScheduler.scheduleOrCancelNotifications(context);
    }

    private void fetchTasksAndShowNotification(Context context, String userId) {
        Log.d(TAG, "fetchTasksAndShowNotification for userId: " + userId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date todayDate = new Date();
        Date todayStart = getStartOfDay(todayDate);
        Date todayEnd = getEndOfDay(todayDate);
        Log.d(TAG, "Fetching tasks for date range: " + DEBUG_DATE_FORMAT.format(todayStart) + " to " + DEBUG_DATE_FORMAT.format(todayEnd));


        AtomicInteger pendingTasksCount = new AtomicInteger(0);
        AtomicReference<String> firstPendingTaskTitle = new AtomicReference<>("");

        db.collection("users").document(userId).collection("achievements")
                .whereEqualTo("completed", false)
                .whereGreaterThanOrEqualTo("targetDate", todayStart)
                .whereLessThanOrEqualTo("targetDate", todayEnd)
                .orderBy("targetDate", Query.Direction.ASCENDING) // Dodano kierunek sortowania
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "fetchTasks: Firestore query successful. Snapshot empty: " + queryDocumentSnapshots.isEmpty() + ", Size: " + queryDocumentSnapshots.size());
                    if (!queryDocumentSnapshots.isEmpty()) {
                        pendingTasksCount.set(queryDocumentSnapshots.size());
                        QueryDocumentSnapshot firstDoc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        firstPendingTaskTitle.set(firstDoc.getString("title"));
                        Log.d(TAG, "fetchTasks: Found " + pendingTasksCount.get() + " pending task(s). First title: '" + firstPendingTaskTitle.get() + "'");

                        String notificationTitle = context.getString(R.string.app_name);
                        String notificationText;
                        if (pendingTasksCount.get() == 1 && firstPendingTaskTitle.get() != null && !firstPendingTaskTitle.get().isEmpty()) {
                            notificationText = context.getString(R.string.notification_single_task_reminder_detail, firstPendingTaskTitle.get());
                        } else {
                            notificationText = context.getString(R.string.notification_multiple_tasks_reminder, pendingTasksCount.get());
                        }
                        Log.d(TAG, "fetchTasks: Preparing to show notification. Title: '" + notificationTitle + "', Text: '" + notificationText + "'");
                        showNotification(context, notificationTitle, notificationText);
                    } else {
                        Log.d(TAG, "fetchTasks: No pending tasks for today to notify about.");
                        // Można rozważyć wysłanie ogólnego powiadomienia, jeśli taka jest intencja
                        // showNotification(context, context.getString(R.string.app_name), context.getString(R.string.notification_general_reminder));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchTasks: Error fetching tasks for notification. PLEASE CHECK FIRESTORE INDEXES!", e);
                    // W tym miejscu NIE planujemy ponownie, bo NotificationScheduler zrobi to po onReceive.
                    // Można by wysłać powiadomienie o błędzie, jeśli to krytyczne.
                });
    }

    private void showNotification(Context context, String title, String message) {
        Log.d(TAG, "showNotification: Title='" + title + "', Message='" + message + "'");

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String soundUriString = prefs.getString(NotificationSettingsActivity.PREF_NOTIFICATION_SOUND_URI, null);
        Uri soundUri = null;
        if (soundUriString != null && !soundUriString.equals("silent")) {
            soundUri = Uri.parse(soundUriString);
        } else if (soundUriString == null) { // Nie ma preferencji (pierwsze uruchomienie), użyj domyślnego
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        // Jeśli soundUriString == "silent", soundUri pozostaje null (cisza)
        Log.d(TAG, "showNotification: Sound URI: " + (soundUri != null ? soundUri.toString() : "Silent"));


        boolean vibrate = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATION_VIBRATE, true);
        Log.d(TAG, "showNotification: Vibrate: " + vibrate);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, MyApplication.DAILY_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_gamifylife_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(soundUri) // setSound(null) jeśli soundUri jest null
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (vibrate) {
            notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
        } else {
            notificationBuilder.setVibrate(null); // Wyraźnie ustaw na null, jeśli nie ma wibrować
        }

        boolean canPostNotifications = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "showNotification: POST_NOTIFICATIONS permission NOT granted. Cannot show notification.");
                canPostNotifications = false;
            } else {
                Log.d(TAG, "showNotification: POST_NOTIFICATIONS permission GRANTED.");
            }
        }

        if (canPostNotifications) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            try {
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                Log.i(TAG, "Notification successfully posted with ID: " + NOTIFICATION_ID);
            } catch (Exception e) {
                Log.e(TAG, "Error posting notification", e);
            }
        } else {
            Log.w(TAG, "Notification not posted due to missing POST_NOTIFICATIONS permission.");
        }
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