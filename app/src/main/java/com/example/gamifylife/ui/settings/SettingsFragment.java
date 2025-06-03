package com.example.gamifylife.ui.settings; // lub com.example.gamifylife.ui

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.gamifylife.NotificationSettingsActivity;
import com.example.gamifylife.R;
import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.helpers.ThemeHelper;

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
}