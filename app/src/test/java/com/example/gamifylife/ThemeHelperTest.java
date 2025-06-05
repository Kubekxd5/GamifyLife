package com.example.gamifylife;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.gamifylife.helpers.ThemeHelper;

public class ThemeHelperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock Context mockContext;
    @Mock SharedPreferences mockPrefs;
    @Mock SharedPreferences.Editor mockEditor;

    private MockedStatic<PreferenceManager> preferenceManagerMockedStatic;
    private MockedStatic<AppCompatDelegate> appCompatDelegateMockedStatic;

    @Before
    public void setUp() {
        preferenceManagerMockedStatic = Mockito.mockStatic(PreferenceManager.class);
        appCompatDelegateMockedStatic = Mockito.mockStatic(AppCompatDelegate.class);

        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
    }

    @After
    public void tearDown() {
        preferenceManagerMockedStatic.close();
        appCompatDelegateMockedStatic.close();
    }

    @Test
    public void setThemePreference_savesCorrectPreference() {
        ThemeHelper.setThemePreference(mockContext, ThemeHelper.THEME_DARK_VIOLET);
        verify(mockEditor).putString(ThemeHelper.PREF_KEY_THEME, ThemeHelper.THEME_DARK_VIOLET);
        verify(mockEditor).apply();
    }

    @Test
    public void getThemePreference_whenPrefExists_returnsSavedTheme() {
        when(mockPrefs.getString(ThemeHelper.PREF_KEY_THEME, ThemeHelper.THEME_SYSTEM_DEFAULT))
                .thenReturn(ThemeHelper.THEME_HACKERMAN);
        assertEquals(ThemeHelper.THEME_HACKERMAN, ThemeHelper.getThemePreference(mockContext));
    }

    @Test
    public void getThemePreference_whenNoPref_returnsSystemDefault() {
        when(mockPrefs.getString(ThemeHelper.PREF_KEY_THEME, ThemeHelper.THEME_SYSTEM_DEFAULT))
                .thenReturn(ThemeHelper.THEME_SYSTEM_DEFAULT); // Symuluj brak zapisanego
        assertEquals(ThemeHelper.THEME_SYSTEM_DEFAULT, ThemeHelper.getThemePreference(mockContext));
    }

    @Test
    public void applyTheme_lightTheme_setsModeNightNo() {
        ThemeHelper.applyTheme(ThemeHelper.THEME_LIGHT);
        appCompatDelegateMockedStatic.verify(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO));
    }

    @Test
    public void applyTheme_darkVioletTheme_setsModeNightYes() {
        ThemeHelper.applyTheme(ThemeHelper.THEME_DARK_VIOLET);
        appCompatDelegateMockedStatic.verify(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
    }

    @Test
    public void applyTheme_darkOrangeTheme_setsModeNightYes() {
        ThemeHelper.applyTheme(ThemeHelper.THEME_DARK_ORANGE);
        appCompatDelegateMockedStatic.verify(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
    }

    @Test
    public void applyTheme_hackermanTheme_setsModeNightYes() {
        ThemeHelper.applyTheme(ThemeHelper.THEME_HACKERMAN);
        appCompatDelegateMockedStatic.verify(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
    }

    @Test
    public void applyTheme_systemDefaultTheme_setsModeNightFollowSystem() {
        ThemeHelper.applyTheme(ThemeHelper.THEME_SYSTEM_DEFAULT);
        appCompatDelegateMockedStatic.verify(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    @Test
    public void applyTheme_unknownTheme_fallsBackToSystemDefault() {
        ThemeHelper.applyTheme("some_unknown_theme_value");
        appCompatDelegateMockedStatic.verify(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }
}