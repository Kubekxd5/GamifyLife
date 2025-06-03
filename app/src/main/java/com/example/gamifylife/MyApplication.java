package com.example.gamifylife;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration; // For onConfigurationChanged

import android.app.Application;
import android.app.NotificationChannel; // Import
import android.app.NotificationManager; // Import
import android.content.Context;
import android.graphics.Color; // Import (opcjonalnie dla LED)
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build; // Import
import android.util.Log;

import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.helpers.ThemeHelper;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MyApplication extends Application {
    public static final String DAILY_REMINDER_CHANNEL_ID = "gamifylife_daily_reminder_channel";
    public static final String DAILY_REMINDER_CHANNEL_NAME = "Daily Task Reminders";
    public static final String DAILY_REMINDER_CHANNEL_DESC = "Reminders for your daily achievements/tasks";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en")); // "en" as default
        // Or if you prefer to always fall back to device default if no preference:
        // super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply the saved theme
        String currentTheme = ThemeHelper.getThemePreference(this);
        ThemeHelper.applyTheme(currentTheme);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
        // Other initializations

        // Zainicjuj Mobile Ads SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // Tutaj możesz sprawdzić status inicjalizacji adapterów mediacji, jeśli ich używasz.
                // Na razie wystarczy sama inicjalizacja.
                // Log.d("AdMob", "Mobile Ads SDK Initialized.");
            }
        });

        createNotificationChannel(); // Wywołaj tworzenie kanału
    }

    // Optional: If you want to handle configuration changes at the application level
    // (though activities usually handle this more directly when they are recreated)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.onAttach(this); // Re-apply locale if configuration changes
    }

    private void createNotificationChannel() {
        // Kanały powiadomień są dostępne tylko na API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            String soundUriString = prefs.getString(NotificationSettingsActivity.PREF_NOTIFICATION_SOUND_URI, null);
            Uri soundUri = null;
            if (soundUriString != null && !soundUriString.equals("silent")) {
                soundUri = Uri.parse(soundUriString);
            } else if (soundUriString == null) { // Nie ma preferencji, użyj domyślnego
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } // Jeśli "silent", soundUri pozostaje null

            boolean vibrate = prefs.getBoolean(NotificationSettingsActivity.PREF_NOTIFICATION_VIBRATE, true);

            NotificationChannel channel = new NotificationChannel(
                    DAILY_REMINDER_CHANNEL_ID,
                    DAILY_REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT // Można zmienić na IMPORTANCE_HIGH dla bardziej natrętnych
            );
            channel.setDescription(DAILY_REMINDER_CHANNEL_DESC);

            if (soundUri != null) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build();
                channel.setSound(soundUri, audioAttributes);
            } else {
                channel.setSound(null, null); // Cisza
            }

            channel.enableVibration(vibrate);
            if (vibrate) {
                channel.setVibrationPattern(new long[]{0, 250, 250, 250}); // Prosty wzór wibracji
            }

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("MyApplication", "Notification channel created: " + DAILY_REMINDER_CHANNEL_ID);
            } else {
                Log.e("MyApplication", "NotificationManager is null, channel not created.");
            }
        }
    }
}