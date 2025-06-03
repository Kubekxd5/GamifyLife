package com.example.gamifylife;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gamifylife.models.UserProfile;
import com.example.gamifylife.util.LevelUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;

import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.os.Environment; // Dla sprawdzania stanu pamięci, ale ostrożnie z zapisem bezpośrednim
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List; // Upewnij się, że jest
import com.google.firebase.firestore.Query; // Jeśli potrzebujesz Query.Direction

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.core.view.MenuProvider; // Dla nowoczesnej obsługi menu we fragmentach
import androidx.lifecycle.Lifecycle;

import com.example.gamifylife.AchievementAdapter;
import com.example.gamifylife.models.Achievement;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementsFragment extends Fragment {

    private static final String TAG = "AchievementsFragment";

    private FloatingActionButton fabAddAchievement;
    private RecyclerView recyclerViewAchievements;
    private TextView textViewEmpty;
    private CalendarView calendarView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private AchievementAdapter achievementAdapter;
    private List<Achievement> achievementList;

    private Date selectedDate;

    public AchievementsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_achievements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        calendarView = view.findViewById(R.id.calendarViewAchievements);
        recyclerViewAchievements = view.findViewById(R.id.recyclerViewAchievements);
        textViewEmpty = view.findViewById(R.id.textViewEmptyAchievements);
        fabAddAchievement = view.findViewById(R.id.fabAddAchievement);

        achievementList = new ArrayList<>();

        achievementAdapter = new AchievementAdapter(getContext(), achievementList, new AchievementAdapter.OnAchievementClickListener() {
            @SuppressLint("StringFormatInvalid")
            @Override
            public void onAchievementClick(Achievement achievement) {
                Toast.makeText(getContext(), getString(R.string.clicked_details_toast, achievement.getTitle()), Toast.LENGTH_SHORT).show(); // Użyj string resource
            }

            @Override
            public void onAttemptCompleteAchievement(Achievement achievement, boolean isChecked) {
                if (!isAdded() || getContext() == null) return; // Fragment nie jest już aktywny

                if (isChecked && !achievement.isCompleted()) {
                    Log.d(TAG, "Fragment: Attempting to complete: " + achievement.getTitle() + " with XP: " + achievement.getXpValue());
                    if (achievement.getXpValue() <= 0) {
                        Log.e(TAG, "Critical: xpValue is " + achievement.getXpValue() + " for achievement. Cannot add XP.");
                        Toast.makeText(getContext(), getString(R.string.error_invalid_xp_for_completion), Toast.LENGTH_LONG).show(); // Dodaj string
                        revertCheckBoxState(achievement.getDocumentId(), false);
                        return;
                    }
                    completeAchievementWithProfileUpdate(achievement);
                } else if (!isChecked && achievement.isCompleted()) {
                    Log.d(TAG, "Fragment: Attempting to UN-complete: " + achievement.getTitle());
                    Toast.makeText(getContext(), getString(R.string.uncompleting_not_implemented_toast), Toast.LENGTH_SHORT).show(); // Dodaj string
                    revertCheckBoxState(achievement.getDocumentId(), true);
                }
            }

            @Override
            public void onEditAchievementClick(Achievement achievement) {
                if (!isAdded() || getActivity() == null) return;

                Log.d(TAG, "Edit clicked for: " + achievement.getTitle());
                Intent intent = new Intent(getActivity(), CreateAchievementActivity.class);
                intent.putExtra("EDIT_MODE", true);
                intent.putExtra("ACHIEVEMENT_ID", achievement.getDocumentId());
                intent.putExtra("ACHIEVEMENT_TITLE", achievement.getTitle());
                intent.putExtra("ACHIEVEMENT_DESC", achievement.getDescription());
                intent.putExtra("ACHIEVEMENT_ICON_NAME", achievement.getIconName()); // Zgodnie z modelem
                if (achievement.getTargetDate() != null) {
                    intent.putExtra("ACHIEVEMENT_TARGET_DATE", achievement.getTargetDate().getTime());
                }
                intent.putExtra("ACHIEVEMENT_XP", achievement.getXpValue());
                startActivity(intent);
            }
        });

        recyclerViewAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAchievements.setAdapter(achievementAdapter);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDate = cal.getTime();
        calendarView.setDate(selectedDate.getTime(), false, true);

        loadAchievementsForDate(selectedDate);

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            selectedDate = calendar.getTime();
            Log.d(TAG, "Calendar date selected: " + selectedDate);
            loadAchievementsForDate(selectedDate);
        });

        fabAddAchievement.setOnClickListener(v -> {
            if (!isAdded() || getActivity() == null) return;
            Intent intent = new Intent(getActivity(), CreateAchievementActivity.class);
            if (selectedDate != null) {
                intent.putExtra("DEFAULT_TARGET_DATE", selectedDate.getTime());
            }
            startActivity(intent);
        });

        setupMenu();
    }

    private void setupMenu() {
        // Nowoczesny sposób dodawania menu do fragmentu
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.achievements_fragment_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_export_pdf) {
                    Log.d(TAG, "Export to PDF action clicked.");
                    exportAchievementsToPdf();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void exportAchievementsToPdf() {
        // Tutaj będzie logika generowania i udostępniania PDF
        // Na razie tylko Toast
        if (getContext() != null) {
            Toast.makeText(getContext(), "Exporting to PDF...", Toast.LENGTH_SHORT).show();
            // Wywołaj metodę, którą zaraz stworzymy
            generateAndSharePdf();
        }
    }

    private void generateAndSharePdf() {
        Log.d(TAG, "generateAndSharePdf called");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pokaż jakiś wskaźnik ładowania, jeśli pobieranie zadań może chwilę potrwać
        // ProgressBar progressBarPdf = view.findViewById(R.id.progressBarPdf); // Musiałbyś dodać do layoutu
        // if (progressBarPdf != null) progressBarPdf.setVisibility(View.VISIBLE);

        // Pobierz wszystkie osiągnięcia (lub tylko z wybranego zakresu, np. ukończone)
        // Dla przykładu pobierzemy wszystkie, posortowane.
        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .orderBy("targetDate", Query.Direction.DESCENDING) // Lub createdAt
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // if (progressBarPdf != null) progressBarPdf.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No achievements to export.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Achievement> achievementsToExport = queryDocumentSnapshots.toObjects(Achievement.class);
                    Log.d(TAG, "Fetched " + achievementsToExport.size() + " achievements to export.");

                    try {
                        File pdfFile = createPdfFile(achievementsToExport);
                        if (pdfFile != null) {
                            sharePdfFile(pdfFile);
                        } else {
                            Toast.makeText(getContext(), "Failed to create PDF file.", Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error generating or sharing PDF", e);
                        Toast.makeText(getContext(), "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // if (progressBarPdf != null) progressBarPdf.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching achievements for PDF export", e);
                    Toast.makeText(getContext(), "Error fetching achievements: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private File createPdfFile(List<Achievement> achievements) throws IOException {
        if (getContext() == null) return null;

        // Użyjemy PdfDocument z Android SDK
        PdfDocument document = new PdfDocument();
        // Rozmiary strony (A4 w punktach, 1 cal = 72 punkty)
        // A4: 210mm x 297mm => 8.27in x 11.69in => 595 x 842 punktów
        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 40; // Marginesy w punktach
        int contentWidth = pageWidth - 2 * margin;

        // Ustawienia tekstu
        TextPaint titlePaint = new TextPaint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(18);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(12);

        TextPaint smallTextPaint = new TextPaint();
        smallTextPaint.setColor(Color.GRAY);
        smallTextPaint.setTextSize(10);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // --- Rysowanie na stronie ---
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int yPosition = margin; // Aktualna pozycja Y na stronie

        // Tytuł dokumentu
        canvas.drawText("My Achievements - GamifyLife", margin, yPosition, titlePaint);
        yPosition += 40;

        for (Achievement achievement : achievements) {
            if (yPosition > pageHeight - margin - 80) { // Sprawdź, czy jest miejsce na następny wpis (zapas 80pt)
                document.finishPage(page); // Zakończ bieżącą stronę
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo); // Rozpocznij nową stronę
                canvas = page.getCanvas();
                yPosition = margin; // Zresetuj Y
                // Można tu dodać nagłówek na nowych stronach
                canvas.drawText("My Achievements (cont.)", margin, yPosition, titlePaint);
                yPosition += 40;
            }

            // Tytuł osiągnięcia
            canvas.drawText(achievement.getTitle(), margin, yPosition, textPaint);
            yPosition += 20;

            // Opis (z obsługą wielu linii)
            if (achievement.getDescription() != null && !achievement.getDescription().isEmpty()) {
                StaticLayout descriptionLayout = StaticLayout.Builder.obtain(achievement.getDescription(), 0, achievement.getDescription().length(), textPaint, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0, 1.0f)
                        .setIncludePad(false)
                        .build();
                canvas.save();
                canvas.translate(margin, yPosition);
                descriptionLayout.draw(canvas);
                canvas.restore();
                yPosition += descriptionLayout.getHeight() + 10;
            }

            // Data docelowa i status
            String status = achievement.isCompleted() ? "Completed" : "Pending";
            if (achievement.getTargetDate() != null) {
                status += " (Target: " + dateFormat.format(achievement.getTargetDate()) + ")";
            }
            if (achievement.isCompleted() && achievement.getCompletedAt() != null) {
                status += " - Done on: " + dateFormat.format(achievement.getCompletedAt());
            }
            canvas.drawText(status, margin, yPosition, smallTextPaint);
            yPosition += 15;

            // XP
            canvas.drawText("XP: " + achievement.getXpValue(), margin, yPosition, smallTextPaint);
            yPosition += 25; // Większy odstęp przed następnym osiągnięciem
        }

        document.finishPage(page);

        // --- Zapis pliku PDF ---
        // Zapisz do katalogu podręcznego aplikacji, podkatalog 'pdfs'
        File pdfDirPath = new File(getContext().getCacheDir(), "pdfs");
        if (!pdfDirPath.exists()) {
            pdfDirPath.mkdirs();
        }
        File file = new File(pdfDirPath, "GamifyLife_Achievements_" + System.currentTimeMillis() + ".pdf");
        Log.d(TAG, "Attempting to save PDF to: " + file.getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
            Log.i(TAG, "PDF file created successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF to file", e);
            throw e; // Rzuć dalej, aby obsłużyć w wywołującej metodzie
        } finally {
            document.close(); // Zawsze zamykaj dokument
        }
        return file;
    }

    private void sharePdfFile(File pdfFile) {
        if (getContext() == null || pdfFile == null || !pdfFile.exists()) {
            Log.e(TAG, "Cannot share PDF: context is null or file does not exist.");
            Toast.makeText(getContext(), "Error: PDF file not found for sharing.", Toast.LENGTH_LONG).show();
            return;
        }

        // Użyj FileProvider do uzyskania content URI
        Uri pdfUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", pdfFile);
        Log.d(TAG, "Sharing PDF with URI: " + pdfUri.toString());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Ważne!

        // Opcjonalnie: dodaj tytuł do chooser'a
        Intent chooserIntent = Intent.createChooser(shareIntent, "Share Achievements PDF");

        // Sprawdź, czy jest jakaś aplikacja, która może obsłużyć ten intent
        if (chooserIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Log.w(TAG, "No app found to handle PDF sharing intent.");
            Toast.makeText(getContext(), "No app found to share PDF.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedDate != null) {
            Log.d(TAG, "onResume: reloading achievements for " + selectedDate);
            loadAchievementsForDate(selectedDate);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            selectedDate = cal.getTime();
            if (calendarView != null) {
                calendarView.setDate(selectedDate.getTime(), false, true);
            }
            loadAchievementsForDate(selectedDate);
        }
    }

    private void loadAchievementsForDate(Date date) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (textViewEmpty != null) {
                textViewEmpty.setVisibility(View.VISIBLE);
                textViewEmpty.setText(getString(R.string.please_log_in));
            }
            if (achievementList != null) achievementList.clear();
            if (achievementAdapter != null) achievementAdapter.notifyDataSetChanged();
            return;
        }

        Calendar calStart = Calendar.getInstance();
        calStart.setTime(date);
        calStart.set(Calendar.HOUR_OF_DAY, 0); calStart.set(Calendar.MINUTE, 0); calStart.set(Calendar.SECOND, 0); calStart.set(Calendar.MILLISECOND, 0);
        Date dayStart = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(date);
        calEnd.set(Calendar.HOUR_OF_DAY, 23); calEnd.set(Calendar.MINUTE, 59); calEnd.set(Calendar.SECOND, 59); calEnd.set(Calendar.MILLISECOND, 999);
        Date dayEnd = calEnd.getTime();

        Log.d(TAG, "Loading achievements for date range: " + dayStart + " to " + dayEnd);

        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .whereGreaterThanOrEqualTo("targetDate", dayStart)
                .whereLessThanOrEqualTo("targetDate", dayEnd)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "loadAchievements: Fragment not attached or context null, aborting update.");
                return;
            }
            if (task.isSuccessful() && task.getResult() != null) {
                achievementList.clear();
                if (task.getResult().isEmpty()) {
                    Log.d(TAG, "No achievements found for this date.");
                    textViewEmpty.setText(getString(R.string.no_achievements_for_date));
                    textViewEmpty.setVisibility(View.VISIBLE);
                } else {
                    textViewEmpty.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            Achievement achievement = document.toObject(Achievement.class); // Spróbuj zdeserializować

                            // ----- KLUCZOWA ZMIANA/SPRAWDZENIE -----
                            if (achievement.getDocumentId() == null) { // Jeśli @DocumentId nie zadziałało
                                Log.w(TAG, "@@DocumentId FAILED for document: " + document.getId() + ". Setting ID manually.");
                                achievement.setDocumentId(document.getId()); // Ustaw ID ręcznie
                            } else {
                                Log.d(TAG, "@@DocumentId WORKED for document: " + document.getId() + ". ID from model: " + achievement.getDocumentId());
                            }
                            // ----- KONIEC KLUCZOWEJ ZMIANY -----

                            achievementList.add(achievement);
                            Log.d(TAG, "Fetched and processed: " + achievement.getTitle() +
                                    " (ID: " + achievement.getDocumentId() +
                                    ") targetDate: " + achievement.getTargetDate() +
                                    " completed: " + achievement.isCompleted() +
                                    " XP: " + achievement.getXpValue());

                        } catch (Exception e) { // Złap potencjalne błędy deserializacji
                            Log.e(TAG, "Error converting document to Achievement object: " + document.getId(), e);
                        }
                    }
                }
                achievementAdapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
                // ... reszta obsługi błędu ...
            }
        });
    }

    private void completeAchievementWithProfileUpdate(final Achievement achievementToComplete) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || achievementToComplete.getDocumentId() == null) {
            Log.w(TAG, "Cannot complete: User not logged in or achievement ID is null for: " + achievementToComplete.getTitle());
            revertCheckBoxState(achievementToComplete.getDocumentId(), false);
            return;
        }
        Log.i(TAG, "Attempting to complete (pre-check): " + achievementToComplete.getTitle() +
                ", Current XP Value: " + achievementToComplete.getXpValue() +
                ", Is Completed (local): " + achievementToComplete.isCompleted() +
                ", Document ID: " + achievementToComplete.getDocumentId());
        if (achievementToComplete.getXpValue() <= 0) {
            Log.e(TAG, "CRITICAL: xpValue is " + achievementToComplete.getXpValue() + " for achievement. Cannot add XP.");
            if (getContext() != null) Toast.makeText(getContext(), getString(R.string.error_invalid_xp_for_completion), Toast.LENGTH_LONG).show();
            revertCheckBoxState(achievementToComplete.getDocumentId(), false);
            return;
        }


        final String userId = currentUser.getUid();
        final String achievementId = achievementToComplete.getDocumentId();

        Log.d(TAG, "Starting transaction to complete achievement: " + achievementId + " Title: " + achievementToComplete.getTitle());

        DocumentReference userProfileRef = db.collection("users").document(userId);
        DocumentReference achievementRef = db.collection("users").document(userId)
                .collection("achievements").document(achievementId);

        db.runTransaction(transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(userProfileRef);
            UserProfile userProfile = userSnapshot.toObject(UserProfile.class);
            if (userProfile == null) {
                Log.e(TAG, "Transaction aborted: User profile not found for UID: " + userId);
                throw new FirebaseFirestoreException("User profile not found. Please ensure profile exists.", FirebaseFirestoreException.Code.ABORTED);
            }

            DocumentSnapshot currentAchievementSnap = transaction.get(achievementRef);
            Achievement currentDbAchievement = currentAchievementSnap.toObject(Achievement.class);
            if (currentDbAchievement == null) {
                Log.e(TAG, "Transaction aborted: Achievement " + achievementId + " not found in DB during transaction.");
                throw new FirebaseFirestoreException("Achievement not found in database.", FirebaseFirestoreException.Code.ABORTED);
            }
            if (currentDbAchievement.isCompleted()) {
                Log.w(TAG, "Transaction: Achievement " + achievementId + " ("+currentDbAchievement.getTitle()+") already marked completed in DB. Updating local UI if needed.");
                int index = findAchievementIndexById(achievementId);
                if (index != -1 && !achievementList.get(index).isCompleted()) {
                    achievementList.get(index).setCompleted(true);
                    achievementList.get(index).setCompletedAt(currentDbAchievement.getCompletedAt() != null ? currentDbAchievement.getCompletedAt() : new Date());
                    // Uruchom na głównym wątku, jeśli transakcja nie jest na nim
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> achievementAdapter.notifyItemChanged(index));
                    }
                }
                return null;
            }

            userProfile.setAchievementsCompleted(userProfile.getAchievementsCompleted() + 1);
            long achievementXp = achievementToComplete.getXpValue(); // Użyj xpValue z obiektu przekazanego do metody
            if (achievementXp <= 0) { // Dodatkowe zabezpieczenie w transakcji
                Log.w(TAG, "Transaction: xpValue is " + achievementXp + " for achievement " + achievementId + ". Not adding XP.");
                achievementXp = 0; // Nie dodawaj ujemnego ani zerowego XP
            }
            long newTotalXp = userProfile.getTotalXp() + achievementXp;
            userProfile.setTotalXp(newTotalXp);
            userProfile.setLevel(LevelUtils.calculateLevel(newTotalXp));
            Log.d(TAG, "Transaction: Updating profile. OldTotalXP: " + (newTotalXp-achievementXp) + ", AchievementXP: " + achievementXp + " -> NewTotalXP: " + newTotalXp + ", NewLevel: " + userProfile.getLevel());
            transaction.set(userProfileRef, userProfile);

            Map<String, Object> achievementUpdates = new HashMap<>();
            achievementUpdates.put("completed", true);
            achievementUpdates.put("completedAt", FieldValue.serverTimestamp());
            Log.d(TAG, "Transaction: Updating achievement " + achievementId + " to completed status.");
            transaction.update(achievementRef, achievementUpdates);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.i(TAG, "SUCCESS: Transaction for completing '" + achievementToComplete.getTitle() + "' (ID: " + achievementId + ") and profile update succeeded.");
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.achievement_completed_toast, achievementToComplete.getXpValue()), Toast.LENGTH_SHORT).show();
            }
            // Aktualizacja lokalnej listy jest ważna, ale rób to ostrożnie.
            // Jeśli Firestore listener dla listy osiągnięć jest aktywny, może on zająć się odświeżeniem.
            // Dla pewności, można zaktualizować konkretny element.
            int index = findAchievementIndexById(achievementId);
            if (index != -1) {
                achievementList.get(index).setCompleted(true);
                achievementList.get(index).setCompletedAt(new Date());
                achievementAdapter.notifyItemChanged(index);
                Log.d(TAG, "Local item updated and adapter notified for index: " + index);
            } else {
                Log.w(TAG, "Completed achievement " + achievementId + " not found in local list post-transaction. Consider reloading.");
                loadAchievementsForDate(selectedDate); // Ostateczność, jeśli coś pójdzie nie tak z indeksem
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "FAILURE_DETAILS: Transaction for completing '" + achievementToComplete.getTitle() + "' (ID: " + achievementId + ") failed.", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.failed_to_complete_achievement) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            revertCheckBoxState(achievementId, false);
        });
    }

    private int findAchievementIndexById(String docId) {
        if (docId == null) return -1;
        for (int i = 0; i < achievementList.size(); i++) {
            if (docId.equals(achievementList.get(i).getDocumentId())) {
                return i;
            }
        }
        return -1;
    }

    private void revertCheckBoxState(String achievementId, boolean revertToCompletedState) {
        final int index = findAchievementIndexById(achievementId); // final dla runOnUiThread
        if (index != -1) {
            Achievement itemInList = achievementList.get(index);
            if (itemInList.isCompleted() != revertToCompletedState) {
                itemInList.setCompleted(revertToCompletedState);
                if (!revertToCompletedState) {
                    itemInList.setCompletedAt(null);
                }
            }
            Log.d(TAG, "Reverting CheckBox UI for '" + itemInList.getTitle() + "' to completed: " + revertToCompletedState);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> { // Upewnij się, że notify jest na głównym wątku
                    if (achievementAdapter != null) { // Sprawdź czy adapter nie jest null
                        achievementAdapter.notifyItemChanged(index);
                    }
                });
            }
        } else {
            Log.w(TAG, "revertCheckBoxState: Achievement " + achievementId + " not found in local list to revert UI.");
        }
    }
}