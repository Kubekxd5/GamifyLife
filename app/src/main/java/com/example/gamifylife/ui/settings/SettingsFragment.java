package com.example.gamifylife.ui.settings; // lub com.example.gamifylife.ui

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.gamifylife.NotificationSettingsActivity;
import com.example.gamifylife.R;
import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.helpers.ThemeHelper;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button buttonChangeLanguage = view.findViewById(R.id.buttonChangeLanguage);
        Button buttonChangeTheme = view.findViewById(R.id.buttonChangeTheme);
        Button buttonNotificationSettings = view.findViewById(R.id.buttonNotificationSettings);

        buttonChangeLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
        buttonChangeTheme.setOnClickListener(v -> showThemeSelectionDialog());
        buttonNotificationSettings.setOnClickListener(v -> {
            // Tutaj otworzymy nową aktywność lub dialog dla ustawień powiadomień
            Toast.makeText(getContext(), "Notification settings coming soon!", Toast.LENGTH_SHORT).show();
        });

        buttonNotificationSettings.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), NotificationSettingsActivity.class);
                startActivity(intent);
            }
        });

        Button buttonResetSettings = view.findViewById(R.id.buttonResetSettings);
        buttonResetSettings.setOnClickListener(v -> showResetSettingsConfirmationDialog());
    }

    private void showLanguageSelectionDialog() {
        if (getContext() == null || getActivity() == null) return;

        final String[] languages = {getString(R.string.language_english), getString(R.string.language_polish)};
        final String[] languageCodes = {"en", "pl"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.select_language_title));

        String currentLangCode = LocaleHelper.getLanguage(getContext());
        int checkedItem = -1;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLangCode)) {
                checkedItem = i;
                break;
            }
        }

        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLangCode = languageCodes[which];
            if (!selectedLangCode.equals(LocaleHelper.getLanguage(getContext()))) {
                LocaleHelper.setLocale(getContext(), selectedLangCode);
                dialog.dismiss();
                getActivity().recreate(); // Restart activity to apply language
            } else {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_button_text), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showThemeSelectionDialog() {
        if (getContext() == null || getActivity() == null) return;

        final String[] themesDisplay = {getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system_default)};
        final String[] themeValues = {ThemeHelper.THEME_LIGHT, ThemeHelper.THEME_DARK, ThemeHelper.THEME_SYSTEM_DEFAULT};

        String currentTheme = ThemeHelper.getThemePreference(getContext());
        int checkedItem = -1;
        for (int i = 0; i < themeValues.length; i++) {
            if (themeValues[i].equals(currentTheme)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.select_theme_title));
        builder.setSingleChoiceItems(themesDisplay, checkedItem, (dialog, which) -> {
            String selectedThemeValue = themeValues[which];
            if (!selectedThemeValue.equals(ThemeHelper.getThemePreference(getContext()))) {
                // W ThemeHelper.java, metoda setTheme powinna wyglądać tak:
                // public static void setTheme(Context context, String themePreference) {
                //    ThemeHelper.setThemePreference(context, themePreference); // Zapisz preferencję
                //    ThemeHelper.applyTheme(themePreference); // Zastosuj motyw (przez AppCompatDelegate)
                // }
                ThemeHelper.setThemePreference(getContext(), selectedThemeValue); // Najpierw zapisz
                ThemeHelper.applyTheme(selectedThemeValue); // Potem zastosuj (to ustawi AppCompatDelegate)
                dialog.dismiss();
                getActivity().recreate(); // WAŻNE: Odtwórz aktywność
            } else {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_button_text), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showResetSettingsConfirmationDialog() {
        if (getContext() == null || getActivity() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_reset_settings_title)
                .setMessage(R.string.dialog_reset_settings_message)
                .setPositiveButton(R.string.reset_button_text, (dialog, which) -> {
                    resetAppSettings();
                })
                .setNegativeButton(R.string.cancel_button_text, null)
                .show();
    }

    private void resetAppSettings() {
        if (getContext() == null || getActivity() == null) return;

        // Resetuj SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        // Klucze do zresetowania (musisz je zebrać ze wszystkich miejsc, gdzie zapisujesz preferencje)
        // Język
        editor.remove(LocaleHelper.SELECTED_LANGUAGE_KEY); // Upewnij się, że klucz jest publiczny w LocaleHelper lub masz metodę reset
        // Motyw
        editor.remove(ThemeHelper.PREF_KEY_THEME); // Upewnij się, że klucz jest publiczny w ThemeHelper lub masz metodę reset
        // Powiadomienia
        editor.remove(NotificationSettingsActivity.PREF_NOTIFICATIONS_ENABLED);
        editor.remove(NotificationSettingsActivity.PREF_NOTIFICATION_HOUR);
        editor.remove(NotificationSettingsActivity.PREF_NOTIFICATION_MINUTE);
        editor.remove(NotificationSettingsActivity.PREF_NOTIFICATION_DAYS);
        editor.remove(NotificationSettingsActivity.PREF_NOTIFICATION_SOUND_URI);
        editor.remove(NotificationSettingsActivity.PREF_NOTIFICATION_VIBRATE);
        // Dodaj inne klucze, jeśli są

        editor.apply();

        // Zastosuj domyślne ustawienia (opcjonalnie, ale dobre dla natychmiastowego efektu przed restartem)
        // Język - systemowy (LocaleHelper.onAttach zrobi to przy restarcie, ale można wymusić)
        LocaleHelper.setLocale(getContext(), Locale.getDefault().getLanguage()); // Ustaw na systemowy

        // Motyw - systemowy
        ThemeHelper.setThemePreference(getContext(), ThemeHelper.THEME_SYSTEM_DEFAULT);
        ThemeHelper.applyTheme(ThemeHelper.THEME_SYSTEM_DEFAULT);


        // Anuluj zaplanowane powiadomienia (gdy NotificationScheduler będzie gotowy)
        // com.example.gamifylife.notifications.NotificationScheduler.cancelAllNotifications(getContext());
        Log.d("SettingsFragment", "All notifications cancelling would happen here.");


        Toast.makeText(getContext(), R.string.settings_reset_toast, Toast.LENGTH_LONG).show();

        // Wymuś restart aplikacji, aby wszystkie zmiany (zwłaszcza język) zostały poprawnie załadowane
        // To jest "brutalny" restart, ale najprostszy do zapewnienia spójności
        Intent i = getActivity().getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            getActivity().finishAffinity(); // Zamknij wszystkie aktywności bieżącej aplikacji
        }
    }
}