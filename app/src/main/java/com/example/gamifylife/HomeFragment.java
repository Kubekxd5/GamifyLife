package com.example.gamifylife;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat; // Dla ContextCompat.getColor
import androidx.fragment.app.Fragment;

import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.models.UserProfile;
import com.example.gamifylife.util.LevelUtils;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.charts.LineChart; // Nowy import
import com.github.mikephil.charting.data.Entry;       // Dla LineChart
import com.github.mikephil.charting.data.LineData;    // Dla LineChart
import com.github.mikephil.charting.data.LineDataSet; // Dla LineChart
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet; // Dla LineChart

import com.google.firebase.Timestamp; // Dla pola completedAt
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query; // Dla Query.Direction
import com.google.firebase.firestore.QueryDocumentSnapshot; // Ten może pozostać, jeśli używasz go gdzie indziej

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration userProfileListener;

    private TextView textViewNickname, textViewLevelValue, textViewAchievementsCompletedValue, textViewAccountCreatedValue;
    private TextView textViewXPCurrent, textViewXPTotalForLevel;
    private ProgressBar progressBarXP, progressBarHome;
    private ImageButton buttonEditNickname;
    private BarChart achievementsChart; // Wykres
    private LineChart xpHistoryChart;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        textViewNickname = view.findViewById(R.id.textViewNickname);
        buttonEditNickname = view.findViewById(R.id.buttonEditNickname);
        textViewLevelValue = view.findViewById(R.id.textViewLevelValue);
        progressBarXP = view.findViewById(R.id.progressBarXP);
        textViewXPCurrent = view.findViewById(R.id.textViewXPCurrent);
        textViewXPTotalForLevel = view.findViewById(R.id.textViewXPTotalForLevel);
        textViewAchievementsCompletedValue = view.findViewById(R.id.textViewAchievementsCompletedValue);
        textViewAccountCreatedValue = view.findViewById(R.id.textViewAccountCreatedValue);
        progressBarHome = view.findViewById(R.id.progressBarHome);
        achievementsChart = view.findViewById(R.id.achievementsChart); // Inicjalizacja wykresu
        xpHistoryChart = view.findViewById(R.id.xpHistoryChart);

        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            // Rozważ nawigację do ekranu logowania lub ukrycie zawartości
            return;
        }

        buttonEditNickname.setOnClickListener(v -> showEditNicknameDialog());
        // loadUserProfile() i loadChartData() są teraz w onStart()
    }

    private void loadUserProfile() {
        if (currentUser != null && db != null) {
            if (progressBarHome != null) progressBarHome.setVisibility(View.VISIBLE);

            DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());

            if (userProfileListener != null) {
                userProfileListener.remove();
                userProfileListener = null;
                Log.d(TAG, "Previous userProfileListener removed.");
            }

            Log.d(TAG, "Attaching new userProfileListener.");
            userProfileListener = userDocRef.addSnapshotListener((snapshot, e) -> {
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "HomeFragment not attached to activity or context is null, ignoring snapshot event.");
                    if (userProfileListener != null) {
                        userProfileListener.remove();
                        userProfileListener = null;
                    }
                    return;
                }

                if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);

                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    UserProfile userProfile = snapshot.toObject(UserProfile.class);
                    if (userProfile != null) {
                        updateUIWithProfile(userProfile);
                    } else {
                        Log.e(TAG, "User profile data is null after conversion from snapshot.");
                        Toast.makeText(getContext(), "Could not parse profile data.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, "Current profile data: null. Profile might not exist for UID: " + currentUser.getUid());
                    Toast.makeText(getContext(), "User profile not found.", Toast.LENGTH_LONG).show();
                    if (textViewNickname != null) textViewNickname.setText(getString(R.string.profile_not_found_placeholder));
                }
            });
        } else {
            Log.w(TAG, "Cannot load profile: currentUser or db is null.");
            if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);
        }
    }

    private void updateUIWithProfile(UserProfile profile) {
        // ... (bez zmian)
        if (textViewNickname == null || getContext() == null) return;

        textViewNickname.setText(profile.getNickname());
        textViewLevelValue.setText(String.valueOf(profile.getLevel()));
        textViewAchievementsCompletedValue.setText(String.valueOf(profile.getAchievementsCompleted()));

        if (profile.getAccountCreatedAt() != null) {
            String lang = LocaleHelper.getLanguage(getContext());
            Locale currentLocale = lang.equals("pl") ? new Locale("pl", "PL") : Locale.getDefault();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", currentLocale);
            textViewAccountCreatedValue.setText(sdf.format(profile.getAccountCreatedAt()));
        } else {
            textViewAccountCreatedValue.setText(getString(R.string.na_placeholder)); // Dodaj string "N/A"
        }

        long currentXpInLevel = LevelUtils.getXPProgressInCurrentLevel(profile.getTotalXp());
        int xpNeededForNext = LevelUtils.getXPTotalForCurrentLevelBar(profile.getTotalXp());

        textViewXPCurrent.setText(getString(R.string.xp_format, (int)currentXpInLevel));
        textViewXPTotalForLevel.setText(getString(R.string.xp_slash_total_format, xpNeededForNext));

        progressBarXP.setMax(xpNeededForNext > 0 ? xpNeededForNext : 100); // Unikaj dzielenia przez zero lub max=0
        progressBarXP.setProgress((int)currentXpInLevel);
    }

    private void showEditNicknameDialog() {
        // ... (bez zmian)
        if (getContext() == null || textViewNickname == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.dialog_edit_nickname_title));

        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_text, null);
        final EditText input = viewInflated.findViewById(R.id.editTextDialogInput);
        input.setHint(getString(R.string.nickname_hint));
        input.setText(textViewNickname.getText().toString());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        builder.setView(viewInflated);

        builder.setPositiveButton(getString(R.string.save_button), (dialog, which) -> {
            dialog.dismiss();
            String newNickname = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newNickname) && currentUser != null && !newNickname.equals(textViewNickname.getText().toString())) {
                updateNicknameInFirestore(newNickname);
            } else if (TextUtils.isEmpty(newNickname)) {
                Toast.makeText(getContext(), getString(R.string.nickname_cannot_be_empty), Toast.LENGTH_SHORT).show(); // Użyj string resource
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_button_text), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateNicknameInFirestore(String newNickname) {
        // ... (bez zmian)
        if (currentUser == null || db == null) return;
        if (progressBarHome != null) progressBarHome.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUser.getUid())
                .update("nickname", newNickname)
                .addOnSuccessListener(aVoid -> {
                    if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);
                    if (getContext() != null) Toast.makeText(getContext(), getString(R.string.nickname_updated), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);
                    if (getContext() != null) Toast.makeText(getContext(), getString(R.string.nickname_update_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadBarChartData() {
        if (currentUser == null || db == null || getContext() == null) {
            Log.w(TAG, "Cannot load chart data: user, db, or context is null.");
            if (achievementsChart != null) {
                achievementsChart.clear(); // Wyczyść wykres, jeśli dane nie mogą być załadowane
                AchievementCountMarkerView mv = new AchievementCountMarkerView(getContext(), R.layout.marker_view_achievements);
                mv.setChartView(achievementsChart); // Ustaw wykres dla markera
                achievementsChart.setMarker(mv); // Ustaw marker dla wykresu

                achievementsChart.invalidate();
            }
            return;
        }
        Log.d(TAG, "loadChartData: Fetching achievements for the last 7 days.");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date sevenDaysAgo = cal.getTime();
        Log.d(TAG, "loadChartData: Querying from date: " + sevenDaysAgo.toString());


        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .whereEqualTo("completed", true)
                .whereGreaterThanOrEqualTo("completedAt", sevenDaysAgo)
                .orderBy("completedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> { // queryDocumentSnapshots to QuerySnapshot
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "loadChartData success: Fragment not attached or context null, aborting UI update.");
                        return;
                    }
                    Log.d(TAG, "loadChartData: Successfully fetched " + queryDocumentSnapshots.size() + " completed achievements for chart.");
                    // queryDocumentSnapshots.getDocuments() zwraca List<DocumentSnapshot>
                    // QuerySnapshot sam w sobie jest iterowalny i zwraca QueryDocumentSnapshot
                    List<QueryDocumentSnapshot> documents = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) { // Iteruj po QuerySnapshot
                        documents.add(doc);
                    }
                    processBarChartData(documents); // Przekaż List<QueryDocumentSnapshot>
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "loadChartData failure: Fragment not attached or context null.");
                        return;
                    }
                    Log.e(TAG, "loadChartData: Error fetching chart data. PLEASE CHECK FIRESTORE INDEXES!", e);
                    Toast.makeText(getContext(), getString(R.string.error_loading_chart_data), Toast.LENGTH_SHORT).show(); // Użyj string resource
                    if (achievementsChart != null) {
                        achievementsChart.clear();
                        achievementsChart.invalidate();
                    }
                });
    }

    private void processBarChartData(List<QueryDocumentSnapshot> documents) {
        Log.d(TAG, "processChartData: Processing " + documents.size() + " documents.");
        Map<String, Integer> dailyCounts = new HashMap<>();
        final ArrayList<String> xLabels = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", LocaleHelper.getLanguage(getContext()).equals("pl") ? new Locale("pl", "PL") : Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) { // Ostatnie 7 dni, od najstarszego do dzisiaj
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dayLabel = dayFormat.format(cal.getTime());
            xLabels.add(dayLabel);
            dailyCounts.put(dayLabel, 0);
        }
        Log.d(TAG, "processChartData: Initialized xLabels: " + xLabels);

        for (QueryDocumentSnapshot doc : documents) {
            Timestamp completedTimestamp = doc.getTimestamp("completedAt");
            if (completedTimestamp != null) {
                Date completedDate = completedTimestamp.toDate();
                String dayLabel = dayFormat.format(completedDate);
                // Upewnij się, że etykieta dnia istnieje w mapie (powinna, jeśli zakres dat jest poprawny)
                if (dailyCounts.containsKey(dayLabel)) {
                    dailyCounts.put(dayLabel, dailyCounts.get(dayLabel) + 1);
                } else {
                    // To może się zdarzyć, jeśli dane z Firestore są spoza oczekiwanego zakresu 7 dni
                    // lub formatowanie etykiet jest niespójne.
                    Log.w(TAG, "processChartData: Encountered dayLabel '" + dayLabel + "' not in pre-initialized xLabels. Data: " + completedDate);
                    // Można dodać, jeśli chcemy być elastyczni, ale lepiej, aby zakresy się zgadzały
                    // dailyCounts.put(dayLabel, 1);
                    // if (!xLabels.contains(dayLabel)) xLabels.add(dayLabel); // Utrudnia sortowanie
                }
            }
        }
        Log.d(TAG, "processChartData: Final daily counts: " + dailyCounts);

        ArrayList<BarEntry> chartEntries = new ArrayList<>();
        for (int i = 0; i < xLabels.size(); i++) {
            String dayLabel = xLabels.get(i);
            // Pobierz wartość, domyślnie 0, jeśli z jakiegoś powodu klucza nie ma
            chartEntries.add(new BarEntry(i, dailyCounts.getOrDefault(dayLabel, 0)));
        }

        setupAndDisplayBarChart(chartEntries, xLabels);
    }

    private void setupAndDisplayBarChart(ArrayList<BarEntry> chartEntries, final ArrayList<String> xLabels) {
        if (achievementsChart == null || getContext() == null || chartEntries.isEmpty()) {
            Log.w(TAG, "setupAndDisplayChart: Chart, context, or entries null/empty. Clearing chart.");
            if (achievementsChart != null) {
                achievementsChart.clear();
                achievementsChart.setNoDataText(getString(R.string.no_chart_data_available));
                achievementsChart.invalidate();
            }
            return;
        }
        Log.d(TAG, "setupAndDisplayChart: Setting up chart with " + chartEntries.size() + " entries.");

        BarDataSet dataSet = new BarDataSet(chartEntries, getString(R.string.home_chart_legend_completed));

        // Użyj semantycznych nazw kolorów
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.chart_bar_color));
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));
        dataSet.setValueTextSize(10f);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        BarData barData = new BarData(dataSets);
        barData.setBarWidth(0.6f);

        achievementsChart.setData(barData);
        achievementsChart.getDescription().setEnabled(false);
        achievementsChart.getLegend().setEnabled(true);
        achievementsChart.getLegend().setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color)); // Użyj semantycznego koloru

        achievementsChart.setDrawGridBackground(false);
        achievementsChart.setFitBars(true);
        achievementsChart.setDrawValueAboveBar(true);
        achievementsChart.setTouchEnabled(true);
        achievementsChart.setDragEnabled(true);
        achievementsChart.setScaleEnabled(true);
        achievementsChart.setPinchZoom(false);

        XAxis xAxis = achievementsChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color)); // Użyj semantycznego koloru
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(xLabels.size(), false); // false, aby wymusić liczbę etykiet
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < xLabels.size()) {
                    return xLabels.get(index);
                }
                return "";
            }
        });

        YAxis leftAxis = achievementsChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(getContext(), R.color.chart_grid_line_color)); // Użyj semantycznego koloru
        leftAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color)); // Użyj semantycznego koloru
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setLabelCount(6, false);

        achievementsChart.getAxisRight().setEnabled(false);

        // Marker (jeśli używasz)
        if (getContext() != null) { // Dodatkowe sprawdzenie kontekstu dla MarkerView
            AchievementCountMarkerView mv = new AchievementCountMarkerView(getContext(), R.layout.marker_view_achievements);
            mv.setChartView(achievementsChart);
            achievementsChart.setMarker(mv);
        }

        achievementsChart.animateY(1200);
        achievementsChart.invalidate();
        Log.d(TAG, "setupAndDisplayChart: Chart invalidated.");
    }

    private void loadXpHistoryChartData() {
        if (currentUser == null || db == null || getContext() == null) {
            Log.w(TAG, "Cannot load XP history chart data: user, db, or context is null.");
            if (xpHistoryChart != null) {
                xpHistoryChart.clear();
                xpHistoryChart.setNoDataText(getString(R.string.no_chart_data_available));
                xpHistoryChart.invalidate();
            }
            return;
        }
        Log.d(TAG, "loadXpHistoryChartData: Fetching completed achievements for XP history (last 7 days).");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6); // Ostatnie 7 dni
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        Date sevenDaysAgo = cal.getTime();

        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .whereEqualTo("completed", true)
                .whereGreaterThanOrEqualTo("completedAt", sevenDaysAgo) // Filtrujemy po dacie ukończenia
                .orderBy("completedAt", Query.Direction.ASCENDING)      // Potrzebne do poprawnego grupowania po dniach
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.d(TAG, "loadXpHistoryChartData: Successfully fetched " + queryDocumentSnapshots.size() + " completed achievements for XP chart.");
                    processXpHistoryData(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "loadXpHistoryChartData: Error fetching data", e);
                    Toast.makeText(getContext(), getString(R.string.error_loading_chart_data), Toast.LENGTH_SHORT).show();
                    if (xpHistoryChart != null) {
                        xpHistoryChart.clear();
                        xpHistoryChart.invalidate();
                    }
                });
    }

    private void processXpHistoryData(List<DocumentSnapshot> documents) {
        Log.d(TAG, "processXpHistoryData: Processing " + documents.size() + " documents for XP chart.");
        Map<String, Integer> dailyXpSum = new HashMap<>(); // Klucz: "EEE" (np. Mon), Wartość: suma XP
        final ArrayList<String> xLabels = new ArrayList<>(); // Etykiety dla osi X (dni tygodnia)
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", LocaleHelper.getLanguage(getContext()).equals("pl") ? new Locale("pl", "PL") : Locale.getDefault());
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Do debugowania kluczy mapy

        Calendar cal = Calendar.getInstance();
        // Inicjalizuj ostatnie 7 dni na osi X i w mapie sum XP
        for (int i = 6; i >= 0; i--) { // Od najstarszego do dzisiaj
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dayLabel = dayFormat.format(cal.getTime());
            String fullDateKey = fullDateFormat.format(cal.getTime()); // Użyj pełnej daty jako klucza dla sum XP
            xLabels.add(dayLabel); // Etykieta dla osi X
            dailyXpSum.put(fullDateKey, 0); // Inicjuj sumę XP dla tego dnia
        }
        Log.d(TAG, "processXpHistoryData: Initialized xLabels: " + xLabels + ", initial dailyXpSum keys: " + dailyXpSum.keySet());

        // Sumuj XP dla każdego dnia
        for (DocumentSnapshot doc : documents) {
            Timestamp completedTimestamp = doc.getTimestamp("completedAt");
            if (completedTimestamp != null) {
                Date completedDate = completedTimestamp.toDate();
                String fullDateKey = fullDateFormat.format(completedDate); // Klucz do mapy sum XP
                int xpValue = doc.getLong("xpValue") != null ? doc.getLong("xpValue").intValue() : 0;

                if (dailyXpSum.containsKey(fullDateKey)) {
                    dailyXpSum.put(fullDateKey, dailyXpSum.get(fullDateKey) + xpValue);
                } else {
                    // To nie powinno się zdarzyć, jeśli zakres dat zapytania i inicjalizacji jest spójny
                    Log.w(TAG, "processXpHistoryData: Date key " + fullDateKey + " not found in dailyXpSum map. This might indicate data outside the 7-day range from query.");
                }
            }
        }
        Log.d(TAG, "processXpHistoryData: Final daily XP sums: " + dailyXpSum);

        // Przygotuj wpisy do wykresu liniowego
        ArrayList<Entry> chartEntries = new ArrayList<>();
        // Iteruj po dniach, dla których mamy etykiety, aby zachować kolejność
        cal.setTime(new Date()); // Zresetuj kalendarz do dzisiaj
        cal.add(Calendar.DAY_OF_YEAR, -6); // Zacznij od 6 dni temu

        for (int i = 0; i < xLabels.size(); i++) { // xLabels ma 7 elementów
            String fullDateKey = fullDateFormat.format(cal.getTime());
            int totalXpForDay = dailyXpSum.getOrDefault(fullDateKey, 0);
            chartEntries.add(new Entry(i, totalXpForDay)); // i to indeks (0-6), totalXpForDay to wartość Y
            cal.add(Calendar.DAY_OF_YEAR, 1); // Przejdź do następnego dnia
        }

        setupAndDisplayXpHistoryChart(chartEntries, xLabels);
    }

    private void setupAndDisplayXpHistoryChart(ArrayList<Entry> chartEntries, final ArrayList<String> xLabels) {
        if (xpHistoryChart == null || getContext() == null || chartEntries.isEmpty()) {
            Log.w(TAG, "setupAndDisplayXpHistoryChart: Chart, context, or entries null/empty. Clearing chart.");
            if (xpHistoryChart != null) {
                xpHistoryChart.clear();
                xpHistoryChart.setNoDataText(getString(R.string.no_chart_data_available));
                xpHistoryChart.invalidate();
            }
            return;
        }
        Log.d(TAG, "setupAndDisplayXpHistoryChart: Setting up XP chart with " + chartEntries.size() + " entries.");

        LineDataSet dataSet = new LineDataSet(chartEntries, getString(R.string.home_xp_chart_legend));
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.teal_700)); // Inny kolor dla odróżnienia
        dataSet.setCircleColor(ContextCompat.getColor(getContext(), R.color.teal_200));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color)); // Użyj semantycznego koloru
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Wygładzona linia
        dataSet.setDrawFilled(true); // Wypełnienie pod linią
        dataSet.setFillColor(ContextCompat.getColor(getContext(), R.color.teal_200));
        dataSet.setFillAlpha(100);


        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);

        xpHistoryChart.setData(lineData);
        xpHistoryChart.getDescription().setEnabled(false);
        xpHistoryChart.getLegend().setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));

        xpHistoryChart.setTouchEnabled(true);
        xpHistoryChart.setDragEnabled(true);
        xpHistoryChart.setScaleEnabled(true);
        xpHistoryChart.setPinchZoom(true);
        xpHistoryChart.setDrawGridBackground(false);

        XAxis xAxis = xpHistoryChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(xLabels.size(), false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < xLabels.size()) {
                    return xLabels.get(index);
                }
                return "";
            }
        });

        YAxis leftAxis = xpHistoryChart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(getContext(), R.color.chart_grid_line_color));
        leftAxis.setAxisMinimum(0f); // XP nie może być ujemne
        leftAxis.setGranularity(1f); // Dla XP może być potrzebna większa granularność lub automatyczna

        xpHistoryChart.getAxisRight().setEnabled(false);

        xpHistoryChart.animateX(1000); // Animacja osi X
        xpHistoryChart.invalidate();
        Log.d(TAG, "setupAndDisplayXpHistoryChart: XP History Chart invalidated.");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called in HomeFragment");
        if (currentUser != null) {
            loadUserProfile();
            loadBarChartData(); // Zmień nazwę dla jasności
            loadXpHistoryChartData(); // NOWA METODA
        } else {
            // ... (obsługa braku użytkownika)
            if (achievementsChart != null) {
                achievementsChart.clear();
                achievementsChart.setNoDataText(getString(R.string.log_in_to_see_chart));
                achievementsChart.invalidate();
            }
            if (xpHistoryChart != null) { // Wyczyść też nowy wykres
                xpHistoryChart.clear();
                xpHistoryChart.setNoDataText(getString(R.string.log_in_to_see_chart));
                xpHistoryChart.invalidate();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called in HomeFragment");
        if (userProfileListener != null) {
            Log.d(TAG, "Removing userProfileListener in onStop.");
            userProfileListener.remove();
            userProfileListener = null;
        }
    }
}