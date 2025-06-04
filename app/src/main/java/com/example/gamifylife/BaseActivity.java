package com.example.gamifylife;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.helpers.ThemeHelper;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Before super.attachBaseContext, update the context with the selected locale
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Zastosuj wybrany motyw PRZED super.onCreate()
        String themePref = ThemeHelper.getThemePreference(this);
        switch (themePref) {
            case ThemeHelper.THEME_LIGHT:
                setTheme(R.style.Theme_GamifyLife_Light);
                break;
            case ThemeHelper.THEME_DARK_VIOLET:
                setTheme(R.style.Theme_GamifyLife_DarkViolet);
                break;
            case ThemeHelper.THEME_DARK_ORANGE:
                setTheme(R.style.Theme_GamifyLife_DarkOrange);
                break;
            case ThemeHelper.THEME_HACKERMAN:
                setTheme(R.style.Theme_GamifyLife_Hackerman);
                break;
            default: // THEME_SYSTEM_DEFAULT lub nieznany
                setTheme(R.style.Theme_GamifyLife); // Używa Base.Theme.GamifyLife
                break;
        }
        super.onCreate(savedInstanceState);
    }
    // You can add other common functionality for your activities here
}