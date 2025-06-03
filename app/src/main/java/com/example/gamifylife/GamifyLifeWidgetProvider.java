package com.example.gamifylife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.os.Build; // Potrzebne dla flag PendingIntent

public class GamifyLifeWidgetProvider extends AppWidgetProvider {

    // Metoda do aktualizacji pojedynczego widżetu
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Tutaj pobierzesz dane, np. z SharedPreferences, bazy danych (SQLite/Room) lub Firebase.
        // Na razie użyjemy statycznego tekstu.
        CharSequence widgetTitle = context.getString(R.string.widget_title_dynamic); // Stwórz ten string
        CharSequence widgetContent = context.getString(R.string.widget_content_example, 5); // Stwórz ten string (np. "5 active achievements")

        // Stwórz RemoteViews, aby ustawić widoki w layoucie widżetu
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.gamifylife_widget_layout);
        views.setTextViewText(R.id.widget_title_textview, widgetTitle);
        views.setTextViewText(R.id.widget_content_textview, widgetContent);

        // Ustawienie PendingIntent do otwierania MainActivity po kliknięciu przycisku
        Intent intentOpenApp = new Intent(context, LoginActivity.class); // Zawsze zaczynamy od LoginActivity, która przekieruje do Main jeśli zalogowany
        intentOpenApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(context, appWidgetId, intentOpenApp, flags);
        views.setOnClickPendingIntent(R.id.widget_open_app_button, pendingIntentOpenApp);

        // Ustawienie PendingIntent do otwierania MainActivity po kliknięciu na cały widżet (opcjonalnie)
        // Ten sam intent co wyżej, ale dla innego ID
        PendingIntent pendingIntentWidgetClick = PendingIntent.getActivity(context, appWidgetId + 1000, intentOpenApp, flags); // Inne requestCode
        views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntentWidgetClick);


        // Poinformuj AppWidgetManager, aby zaktualizował widżet
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // Wywoływane, gdy widżet jest aktualizowany (np. co updatePeriodMillis lub ręcznie)
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Zaktualizuj wszystkie instancje tego widżetu
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // Wywoływane, gdy widżet jest pierwszy raz dodawany na ekran
    @Override
    public void onEnabled(Context context) {
        // Możesz tutaj uruchomić np. AlarmManager do regularnych aktualizacji, jeśli potrzebujesz
        // bardziej precyzyjnej kontroli niż updatePeriodMillis
    }

    // Wywoływane, gdy ostatnia instancja widżetu jest usuwana
    @Override
    public void onDisabled(Context context) {
        // Możesz tutaj anulować AlarmManager, jeśli go używałeś
    }

    // Wywoływane, gdy widżet jest usuwany z ekranu
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        // Możesz tutaj posprzątać zasoby związane z usuniętymi widżetami (np. z SharedPreferences)
    }
}