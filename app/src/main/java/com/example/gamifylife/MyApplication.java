package com.example.gamifylife;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration; // For onConfigurationChanged

import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.helpers.ThemeHelper;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MyApplication extends Application {
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
    }

    // Optional: If you want to handle configuration changes at the application level
    // (though activities usually handle this more directly when they are recreated)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.onAttach(this); // Re-apply locale if configuration changes
    }
}