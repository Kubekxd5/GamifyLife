package com.example.gamifylife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log; // Dodaj logowanie
import android.view.View;
import android.widget.RemoteViews;
import com.example.gamifylife.util.WidgetConstants; // Import

public class GamifyLifeWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "GamifyWidgetProvider"; // Tag dla logów

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateAppWidget called for widget ID: " + appWidgetId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.gamifylife_widget_layout);

        SharedPreferences prefs = context.getSharedPreferences(WidgetConstants.PREFS_WIDGET_DATA, Context.MODE_PRIVATE);
        String userId = prefs.getString(WidgetConstants.KEY_LAST_USER_ID, null);
        String nickname = prefs.getString(WidgetConstants.KEY_USER_NICKNAME_FOR_WIDGET, "");
        int pendingTasksCount = prefs.getInt(WidgetConstants.KEY_TODAY_PENDING_ACHIEVEMENTS_COUNT, -1); // -1 jako domyślny/błąd

        Log.d(TAG, "Widget data read: UserID=" + userId + ", Nickname=" + nickname + ", Tasks=" + pendingTasksCount);

        if (userId != null) { // Użytkownik jest/był zalogowany
            String titleText = nickname.isEmpty() ? context.getString(R.string.widget_title_achievements_today)
                    : context.getString(R.string.widget_title_user_achievements, nickname);
            views.setTextViewText(R.id.widget_title_textview, titleText);

            if (pendingTasksCount >= 0) {
                views.setTextViewText(R.id.widget_content_textview,
                        context.getResources().getQuantityString(R.plurals.widget_pending_achievements, pendingTasksCount, pendingTasksCount));
                views.setViewVisibility(R.id.widget_empty_textview, View.GONE);
                views.setViewVisibility(R.id.widget_content_textview, View.VISIBLE);
            } else if (pendingTasksCount == -1) { // Błąd pobierania danych
                views.setTextViewText(R.id.widget_empty_textview, context.getString(R.string.widget_data_error));
                views.setViewVisibility(R.id.widget_empty_textview, View.VISIBLE);
                views.setViewVisibility(R.id.widget_content_textview, View.GONE);
            }
            views.setTextViewText(R.id.widget_last_updated_textview,
                    context.getString(R.string.widget_last_updated,
                            new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date())));

        } else { // Nikt nie jest zalogowany lub brak danych
            views.setTextViewText(R.id.widget_title_textview, context.getString(R.string.app_name));
            views.setTextViewText(R.id.widget_empty_textview, context.getString(R.string.widget_please_log_in));
            views.setViewVisibility(R.id.widget_empty_textview, View.VISIBLE);
            views.setViewVisibility(R.id.widget_content_textview, View.GONE);
            views.setTextViewText(R.id.widget_last_updated_textview, "");
        }


        // Intent do otwierania aplikacji
        Intent intentOpenApp = new Intent(context, LoginActivity.class);
        intentOpenApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(context, appWidgetId, intentOpenApp, flags);
        views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntentOpenApp); // Kliknięcie na cały widget

        // Usunięcie przycisku, jeśli go nie ma w nowym layoucie
        // views.setOnClickPendingIntent(R.id.widget_open_app_button, pendingIntentOpenApp);

        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.d(TAG, "Widget ID " + appWidgetId + " updated.");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for " + appWidgetIds.length + " widgets.");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled: Widget provider enabled.");
        // Możesz tu zainicjować jakieś zadanie w tle, jeśli potrzebujesz bardziej regularnych aktualizacji
        // np. za pomocą WorkManager, aby co jakiś czas wywoływać triggerWidgetUpdate() z aplikacji głównej.
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled: Widget provider disabled.");
    }
}