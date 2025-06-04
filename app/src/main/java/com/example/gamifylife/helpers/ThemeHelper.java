package com.example.gamifylife.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class ThemeHelper {
    public static final String PREF_KEY_THEME = "pref_selected_theme";

    public static final String THEME_LIGHT = "light"; // Biało-pomarańczowy
    public static final String THEME_DARK_VIOLET = "dark_violet"; // Stary "dark"
    public static final String THEME_DARK_ORANGE = "dark_orange"; // Nowy
    public static final String THEME_HACKERMAN = "hackerman";   // Nowy
    public static final String THEME_SYSTEM_DEFAULT = "system";

    public static void applyTheme(String themePreference) {
        switch (themePreference) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                // Można też jawnie ustawić styl, jeśli Base.Theme.GamifyLife nie jest wystarczający
                // np. activity.setTheme(R.style.Theme_GamifyLife_Light);
                break;
            case THEME_DARK_VIOLET:
            case THEME_DARK_ORANGE:
            case THEME_HACKERMAN: // Wszystkie te będą traktowane jako tryb ciemny przez AppCompatDelegate
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                // A konkretne kolory zostaną załadowane przez setTheme w BaseActivity lub przez styl aplikacji.
                break;
            default: // THEME_SYSTEM_DEFAULT
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void setThemePreference(Context context, String themePreference) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREF_KEY_THEME, themePreference).apply();
    }

    public static String getThemePreference(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_KEY_THEME, THEME_SYSTEM_DEFAULT); // Default to system
    }
}