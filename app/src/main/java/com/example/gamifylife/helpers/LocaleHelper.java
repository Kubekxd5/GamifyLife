package com.example.gamifylife.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager; // Or androidx.preference.PreferenceManager if you use AndroidX Preference library

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE_KEY = "Locale.Helper.Selected.Language";

    // Call this from your Application's attachBaseContext or BaseActivity's attachBaseContext
    public static Context onAttach(Context context) {
        String lang = getPersistedLocale(context);
        return setLocale(context, lang);
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedLocale(context, defaultLanguage);
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        return getPersistedLocale(context, Locale.getDefault().getLanguage());
    }

    public static Context setLocale(Context context, String languageCode) {
        persist(context, languageCode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, languageCode);
        }
        return updateResourcesLegacy(context, languageCode);
    }

    private static String getPersistedLocale(Context context, String defaultLanguage) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_LANGUAGE_KEY, defaultLanguage);
    }
    private static String getPersistedLocale(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Fallback to device default if no preference is set
        return preferences.getString(SELECTED_LANGUAGE_KEY, Locale.getDefault().getLanguage());
    }


    private static void persist(Context context, String languageCode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_LANGUAGE_KEY, languageCode);
        editor.apply();
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale; // Use configuration.setLocale(locale) for API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    // For API 24+
    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }
}