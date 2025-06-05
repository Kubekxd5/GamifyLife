package com.example.gamifylife;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build; // Nadal używany w kodzie produkcyjnym
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.gamifylife.helpers.LocaleHelper;

public class LocaleHelperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock Context mockContext;
    @Mock SharedPreferences mockPrefs;
    @Mock SharedPreferences.Editor mockEditor;
    @Mock Resources mockResources;
    @Mock Configuration mockConfiguration;
    @Mock DisplayMetrics mockDisplayMetrics;
    @Mock Context mockCreatedContext; // Dla createConfigurationContext

    private MockedStatic<PreferenceManager> preferenceManagerMockedStatic;
    // Nie będziemy mockować Locale.setDefault ani Locale.getDefault w tym uproszczonym podejściu

    @Captor ArgumentCaptor<String> stringCaptor;
    @Captor ArgumentCaptor<Locale> localeCaptor;
    @Captor ArgumentCaptor<Configuration> configurationCaptor;


    @Before
    public void setUp() {
        preferenceManagerMockedStatic = mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);

        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getConfiguration()).thenReturn(mockConfiguration);
        when(mockResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(mockContext.createConfigurationContext(any(Configuration.class))).thenReturn(mockCreatedContext);
    }

    @After
    public void tearDown() {
        preferenceManagerMockedStatic.close();
    }

    @Test
    public void persist_SavesLanguageToPrefs() {
        // Dostęp package-private do metody persist, lub testuj przez publiczną setLocale
        // LocaleHelper.persist(mockContext, "pl"); // Jeśli persist byłby publiczny
        LocaleHelper.setLocale(mockContext, "pl"); // setLocale wywoła persist

        verify(mockEditor).putString(LocaleHelper.SELECTED_LANGUAGE_KEY, "pl");
        verify(mockEditor).apply();
    }

    @Test
    public void getLanguage_WhenPrefExists_ReturnsSavedLanguage() {
        // Zakładamy, że Locale.getDefault() zwróci coś, np. "en"
        String defaultSystemLang = Locale.getDefault().getLanguage();
        when(mockPrefs.getString(LocaleHelper.SELECTED_LANGUAGE_KEY, defaultSystemLang)).thenReturn("fr");
        assertEquals("fr", LocaleHelper.getLanguage(mockContext));
    }

    @Test
    public void getLanguage_WhenNoPref_ReturnsDefaultSystemLanguage() {
        String defaultSystemLang = Locale.getDefault().getLanguage();
        // Symuluj, że getString zwraca wartość domyślną (drugi argument)
        when(mockPrefs.getString(LocaleHelper.SELECTED_LANGUAGE_KEY, defaultSystemLang))
                .thenReturn(defaultSystemLang);
        assertEquals(defaultSystemLang, LocaleHelper.getLanguage(mockContext));
    }

    @Test
    public void setLocale_CallsPersist() {
        LocaleHelper.setLocale(mockContext, "de");
        verify(mockEditor).putString(LocaleHelper.SELECTED_LANGUAGE_KEY, "de");
        verify(mockEditor).apply();
    }

    @Test
    public void onAttach_CallsSetLocaleWithCorrectLanguage() {
        // Arrange
        // Scenariusz 1: Język jest w SharedPreferences
        String persistedLang = "jp";
        when(mockPrefs.getString(eq(LocaleHelper.SELECTED_LANGUAGE_KEY), anyString())).thenReturn(persistedLang);

        // Act
        LocaleHelper.onAttach(mockContext); // Wywołanie wersji bez domyślnego języka

        // Assert
        // Sprawdzamy, czy persist zostało wywołane z "jp" (bo setLocale wywołuje persist)
        verify(mockEditor).putString(LocaleHelper.SELECTED_LANGUAGE_KEY, persistedLang);
        verify(mockEditor).apply(); // Upewnij się, że apply jest wołane
        clearInvocations(mockEditor); // Wyczyść dla następnego scenariusza


        // Scenariusz 2: Brak języka w SharedPreferences, użyj domyślnego systemowego
        String defaultSystemLang = Locale.getDefault().getLanguage(); // Pobierz rzeczywisty domyślny
        when(mockPrefs.getString(eq(LocaleHelper.SELECTED_LANGUAGE_KEY), eq(defaultSystemLang))).thenReturn(defaultSystemLang);

        LocaleHelper.onAttach(mockContext);

        verify(mockEditor).putString(LocaleHelper.SELECTED_LANGUAGE_KEY, defaultSystemLang);
        verify(mockEditor).apply();
        clearInvocations(mockEditor);

        // Scenariusz 3: Użycie onAttach z podanym językiem domyślnym (np. "en"), gdy nic nie ma w SharedPreferences
        String passedDefaultLang = "en";
        when(mockPrefs.getString(eq(LocaleHelper.SELECTED_LANGUAGE_KEY), eq(passedDefaultLang))).thenReturn(passedDefaultLang);
        LocaleHelper.onAttach(mockContext, passedDefaultLang);
        verify(mockEditor).putString(LocaleHelper.SELECTED_LANGUAGE_KEY, passedDefaultLang);
        verify(mockEditor).apply();
    }
}