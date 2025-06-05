package com.example.gamifylife;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamifylife.models.Achievement;
import com.example.gamifylife.models.UserProfile;
import com.example.gamifylife.util.LevelUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AchievementsFragment extends Fragment {

    private static final String TAG = "AchievementsFragment";

    private FloatingActionButton fabAddAchievement;
    private RecyclerView recyclerViewAchievements;
    TextView textViewEmpty;
    private CalendarView calendarView;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    AchievementAdapter achievementAdapter;
    List<Achievement> achievementList;

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
                if (getContext() == null || achievement == null) return;
                String title = achievement.getTitle() != null ? achievement.getTitle() : "";
                Toast.makeText(getContext(), getString(R.string.clicked_details_toast, title), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAttemptCompleteAchievement(Achievement achievement, boolean isChecked) {
                if (!isAdded() || getContext() == null || achievement == null) return;

                if (isChecked && !achievement.isCompleted()) {
                    Log.d(TAG, "Fragment: Attempting to complete: " + achievement.getTitle() + " with XP: " + achievement.getXpValue());
                    if (achievement.getXpValue() <= 0) {
                        Log.e(TAG, "Critical: xpValue is " + achievement.getXpValue() + " for achievement. Cannot add XP.");
                        Toast.makeText(getContext(), getString(R.string.error_invalid_xp_for_completion), Toast.LENGTH_LONG).show();
                        revertCheckBoxState(achievement.getDocumentId(), false);
                        return;
                    }
                    completeAchievementWithProfileUpdate(achievement);
                } else if (!isChecked && achievement.isCompleted()) {
                    Log.d(TAG, "Fragment: Attempting to UN-complete: " + achievement.getTitle());
                    Toast.makeText(getContext(), getString(R.string.uncompleting_not_implemented_toast), Toast.LENGTH_SHORT).show();
                    revertCheckBoxState(achievement.getDocumentId(), true);
                }
            }

            @Override
            public void onEditAchievementClick(Achievement achievement) {
                if (!isAdded() || getActivity() == null || achievement == null) return;

                Log.d(TAG, "Edit clicked for: " + achievement.getTitle());
                Intent intent = new Intent(getActivity(), CreateAchievementActivity.class);
                intent.putExtra("EDIT_MODE", true);
                intent.putExtra("ACHIEVEMENT_ID", achievement.getDocumentId());
                intent.putExtra("ACHIEVEMENT_TITLE", achievement.getTitle());
                intent.putExtra("ACHIEVEMENT_DESC", achievement.getDescription());
                intent.putExtra("ACHIEVEMENT_ICON_NAME", achievement.getIconName());
                if (achievement.getTargetDate() != null) {
                    intent.putExtra("ACHIEVEMENT_TARGET_DATE", achievement.getTargetDate().getTime());
                }
                intent.putExtra("ACHIEVEMENT_XP", achievement.getXpValue());
                startActivity(intent);
            }

            @Override
            public void onDeleteAchievementClick(Achievement achievement, int position) {
                if (!isAdded() || getContext() == null || achievement == null) return;
                Log.d(TAG, "Delete clicked for: " + achievement.getTitle() + " at position " + position);
                showDeleteConfirmationDialog(achievement, position);
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
        if (calendarView != null) {
            calendarView.setDate(selectedDate.getTime(), false, true);
        }

        loadAchievementsForDate(selectedDate);

        if (calendarView != null) {
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
        }

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
        if (getActivity() == null) return;
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
        if (getContext() == null) { Log.w(TAG, "exportAchievementsToPdf: Context is null."); return; }
        Toast.makeText(getContext(), getString(R.string.exporting_to_pdf_toast), Toast.LENGTH_SHORT).show();
        generateAndSharePdf();
    }

    private void generateAndSharePdf() {
        Log.d(TAG, "generateAndSharePdf called");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || getContext() == null) {
            if(getContext() != null) Toast.makeText(getContext(), getString(R.string.user_not_logged_in_error), Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .orderBy("targetDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getContext() == null || !isAdded()) return;
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.no_achievements_to_export), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Achievement> achievementsToExport = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots){
                        Achievement ach = doc.toObject(Achievement.class);
                        ach.setDocumentId(doc.getId());
                        achievementsToExport.add(ach);
                    }
                    Log.d(TAG, "Fetched " + achievementsToExport.size() + " achievements to export.");
                    try {
                        File pdfFile = createPdfFile(achievementsToExport);
                        if (pdfFile != null) {
                            sharePdfFile(pdfFile);
                        } else {
                            Toast.makeText(getContext(), getString(R.string.failed_to_create_pdf), Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error generating or sharing PDF", e);
                        Toast.makeText(getContext(), getString(R.string.error_generating_pdf, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    Log.e(TAG, "Error fetching achievements for PDF export", e);
                    Toast.makeText(getContext(), getString(R.string.error_loading_achievements) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private File createPdfFile(List<Achievement> achievements) throws IOException {
        if (getContext() == null) { Log.e(TAG, "createPdfFile: Context is null."); return null; }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595; int pageHeight = 842; int margin = 40; int contentWidth = pageWidth - 2 * margin;
        TextPaint titlePaint = new TextPaint(); titlePaint.setColor(Color.BLACK); titlePaint.setTextSize(18); titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        TextPaint textPaint = new TextPaint(); textPaint.setColor(Color.DKGRAY); textPaint.setTextSize(12);
        TextPaint smallTextPaint = new TextPaint(); smallTextPaint.setColor(Color.GRAY); smallTextPaint.setTextSize(10);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas(); int yPosition = margin;
        canvas.drawText(getString(R.string.pdf_title_my_achievements), margin, yPosition, titlePaint); yPosition += 40;

        for (Achievement achievement : achievements) {
            if (yPosition > pageHeight - margin - 80) { // Check space for next entry
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo); canvas = page.getCanvas(); yPosition = margin;
                canvas.drawText(getString(R.string.pdf_title_my_achievements_cont), margin, yPosition, titlePaint); yPosition += 40;
            }
            canvas.drawText(achievement.getTitle(), margin, yPosition, textPaint); yPosition += 20;
            if (achievement.getDescription() != null && !achievement.getDescription().isEmpty()) {
                StaticLayout descriptionLayout = StaticLayout.Builder.obtain(achievement.getDescription(), 0, achievement.getDescription().length(), textPaint, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0, 1.0f).setIncludePad(false).build();
                canvas.save(); canvas.translate(margin, yPosition); descriptionLayout.draw(canvas); canvas.restore();
                yPosition += descriptionLayout.getHeight() + 10;
            }
            String status = achievement.isCompleted() ? getString(R.string.pdf_status_completed) : getString(R.string.pdf_status_pending);
            if (achievement.getTargetDate() != null) status += " (" + getString(R.string.pdf_target_date_label) + dateFormat.format(achievement.getTargetDate()) + ")";
            if (achievement.isCompleted() && achievement.getCompletedAt() != null) status += " - " + getString(R.string.pdf_done_on_label) + dateFormat.format(achievement.getCompletedAt());
            canvas.drawText(status, margin, yPosition, smallTextPaint); yPosition += 15;
            canvas.drawText(getString(R.string.pdf_xp_label) + achievement.getXpValue(), margin, yPosition, smallTextPaint); yPosition += 25;
        }
        document.finishPage(page);
        File pdfDirPath = new File(getContext().getCacheDir(), "pdfs");
        if (!pdfDirPath.exists()) { if(!pdfDirPath.mkdirs()){ Log.e(TAG, "Failed to create PDF directory."); document.close(); return null; } }
        File file = new File(pdfDirPath, "GamifyLife_Achievements_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) { document.writeTo(fos); }
        catch (IOException e) { Log.e(TAG, "Error writing PDF", e); document.close(); throw e; }
        finally { document.close(); }
        Log.i(TAG, "PDF created: " + file.getAbsolutePath()); return file;
    }

    private void sharePdfFile(File pdfFile) {
        if (getContext() == null || pdfFile == null || !pdfFile.exists()) {
            Log.e(TAG, "Cannot share PDF: invalid state.");
            if(getContext() != null) Toast.makeText(getContext(), getString(R.string.error_sharing_pdf_general), Toast.LENGTH_LONG).show();
            return;
        }
        Uri pdfUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", pdfFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf"); shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.pdf_share_chooser_title));
        if (chooserIntent.resolveActivity(getContext().getPackageManager()) != null) { startActivity(chooserIntent); }
        else { Log.w(TAG, "No app to handle PDF share."); if(getContext() != null) Toast.makeText(getContext(), getString(R.string.no_app_to_share_pdf), Toast.LENGTH_LONG).show(); }
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

    // W AchievementsFragment.java
    public void loadAchievementsForDate(Date date) {
        Log.d(TAG, "loadAchievementsForDate called with date: " + (date != null ? date.toString() : "null"));
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (achievementList == null) {
            achievementList = new ArrayList<>();
        }

        if (currentUser == null) {
            Log.d(TAG, "User is null. Updating UI for logged out state.");
            achievementList.clear(); // Czyść, bo nie będzie danych
            if (textViewEmpty != null) {
                textViewEmpty.setVisibility(View.VISIBLE);
                textViewEmpty.setText((isAdded() && getContext() != null) ? getString(R.string.please_log_in) : null);
            }
            if (achievementAdapter != null) {
                achievementAdapter.notifyDataSetChanged();
            }
            return;
        }

        Calendar calStart = Calendar.getInstance(); calStart.setTime(date);
        calStart.set(Calendar.HOUR_OF_DAY, 0); calStart.set(Calendar.MINUTE, 0); calStart.set(Calendar.SECOND, 0); calStart.set(Calendar.MILLISECOND, 0);
        Date dayStart = calStart.getTime();

        Calendar calEnd = Calendar.getInstance(); calEnd.setTime(date);
        calEnd.set(Calendar.HOUR_OF_DAY, 23); calEnd.set(Calendar.MINUTE, 59); calEnd.set(Calendar.SECOND, 59); calEnd.set(Calendar.MILLISECOND, 999);
        Date dayEnd = calEnd.getTime();

        Log.d(TAG, "Loading achievements for user: " + currentUser.getUid() + " for date range: " + dayStart + " to " + dayEnd);

        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .whereGreaterThanOrEqualTo("targetDate", dayStart)
                .whereLessThanOrEqualTo("targetDate", dayEnd)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment not attached or context is null after Firestore call. Aborting UI update.");
                        return;
                    }

                    achievementList.clear(); // Czyść listę ZAWSZE przed jej potencjalnym wypełnieniem lub pokazaniem błędu

                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "Firestore query successful. Documents found: " + task.getResult().size());
                        if (task.getResult().isEmpty()) {
                            Log.d(TAG, "No achievements found for this date.");
                            if (textViewEmpty != null) {
                                textViewEmpty.setText(getString(R.string.no_achievements_for_date));
                                textViewEmpty.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (textViewEmpty != null) textViewEmpty.setVisibility(View.GONE);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Achievement achievement = document.toObject(Achievement.class);
                                    if (achievement != null) {
                                        if (achievement.getDocumentId() == null) { // Podwójne sprawdzenie
                                            achievement.setDocumentId(document.getId());
                                        }
                                        achievementList.add(achievement);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Achievement object: " + document.getId(), e);
                                }
                            }
                            Log.d(TAG, "Populated achievementList with " + achievementList.size() + " items.");
                        }
                    } else { // Błąd Firestore
                        Log.e(TAG, "Error getting documents from Firestore: ", task.getException());
                        // Lista została już wyczyszczona na początku addOnCompleteListener
                        if (textViewEmpty != null) {
                            textViewEmpty.setText(getString(R.string.error_loading_achievements));
                            textViewEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    if (achievementAdapter != null) {
                        achievementAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Adapter notified of data set change (end of onComplete).");
                    } else {
                        Log.w(TAG, "achievementAdapter is null, cannot notify (end of onComplete).");
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

    // NOWE METODY DLA USUWANIA
    private void showDeleteConfirmationDialog(final Achievement achievement, final int position) {
        if (getContext() == null || achievement == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.dialog_delete_achievement_title))
                .setMessage(getString(R.string.dialog_delete_achievement_message, achievement.getTitle()))
                .setPositiveButton(getString(R.string.delete_button_text), (dialog, which) -> {
                    deleteAchievementAndUpdateProfile(achievement, position);
                })
                .setNegativeButton(getString(R.string.cancel_button_text), null)
                .setIcon(R.drawable.ic_delete) // Użyj ikony kosza
                .show();
    }

    private void deleteAchievementAndUpdateProfile(final Achievement achievementToDelete, final int positionInAdapter) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || achievementToDelete.getDocumentId() == null || getContext() == null || !isAdded()) {
            Log.w(TAG, "Cannot delete: Invalid state (user, achievementId, context, or fragment not added).");
            if (getContext() != null) Toast.makeText(getContext(), getString(R.string.failed_to_delete_achievement) + ": Invalid data.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Attempting to delete: " + achievementToDelete.getTitle() + ", XP: " + achievementToDelete.getXpValue() + ", Completed: " + achievementToDelete.isCompleted() + ", ID: " + achievementToDelete.getDocumentId());

        final String userId = currentUser.getUid();
        final String achievementId = achievementToDelete.getDocumentId();

        DocumentReference userProfileRef = db.collection("users").document(userId);
        DocumentReference achievementRef = db.collection("users").document(userId).collection("achievements").document(achievementId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(userProfileRef);
            UserProfile userProfile = userSnapshot.toObject(UserProfile.class);
            if (userProfile == null) throw new FirebaseFirestoreException("User profile not found.", FirebaseFirestoreException.Code.ABORTED);

            DocumentSnapshot achievementSnapshot = transaction.get(achievementRef);
            Achievement currentDbAchievement = null; // Zmienna na odczytane z bazy osiągnięcie
            boolean wasCompletedInDb = false;
            long xpFromDb = 0;

            if (achievementSnapshot.exists()) {
                currentDbAchievement = achievementSnapshot.toObject(Achievement.class);
                if (currentDbAchievement != null) {
                    currentDbAchievement.setDocumentId(achievementSnapshot.getId()); // Ważne dla spójności
                    wasCompletedInDb = currentDbAchievement.isCompleted();
                    xpFromDb = currentDbAchievement.getXpValue();
                    Log.d(TAG, "Transaction: Fetched DB achievement: " + currentDbAchievement.getTitle() + ", Completed: " + wasCompletedInDb + ", XP: " + xpFromDb);
                } else {
                    Log.w(TAG, "Transaction: Achievement snapshot exists but could not be converted to object. ID: " + achievementId);
                }
            } else {
                Log.w(TAG, "Transaction: Achievement document " + achievementId + " does not exist in DB. Cannot update profile based on its state.");
                // Jeśli dokument nie istnieje, po prostu go usuwamy (lub próbujemy, jeśli został usunięty w międzyczasie)
                // Nie zmieniamy profilu w tym przypadku.
            }

            if (wasCompletedInDb) { // Modyfikuj profil tylko jeśli osiągnięcie było ukończone W BAZIE
                if (xpFromDb > 0) {
                    long newTotalXp = Math.max(0, userProfile.getTotalXp() - xpFromDb);
                    Log.d(TAG, "Transaction: Updating profile. OldTotalXP: " + userProfile.getTotalXp() +
                            ", Subtracting XP: " + xpFromDb + " -> NewTotalXP: " + newTotalXp);
                    userProfile.setTotalXp(newTotalXp);
                    userProfile.setLevel(LevelUtils.calculateLevel(newTotalXp));
                }
                userProfile.setAchievementsCompleted(Math.max(0, userProfile.getAchievementsCompleted() - 1));
                transaction.set(userProfileRef, userProfile);
                Log.d(TAG, "Transaction: User profile updated. New Level: " + userProfile.getLevel() + ", AchievementsCompleted: " + userProfile.getAchievementsCompleted());
            } else {
                Log.d(TAG, "Transaction: DB Achievement was not completed or not found, no XP/count change for profile.");
            }

            Log.d(TAG, "Transaction: Deleting achievement document: " + achievementId);
            transaction.delete(achievementRef); // Zawsze próbuj usunąć
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (getContext() == null || !isAdded()) return;
            Log.i(TAG, "SUCCESS: Transaction for deleting '" + achievementToDelete.getTitle() + "' and profile update succeeded.");
            Toast.makeText(getContext(), getString(R.string.achievement_deleted_toast), Toast.LENGTH_SHORT).show();

            int actualPositionToRemove = -1;
            for (int i = 0; i < achievementList.size(); i++) {
                if (achievementId.equals(achievementList.get(i).getDocumentId())) {
                    actualPositionToRemove = i;
                    break;
                }
            }

            if (actualPositionToRemove != -1) {
                achievementList.remove(actualPositionToRemove);
                achievementAdapter.notifyItemRemoved(actualPositionToRemove);
                achievementAdapter.notifyItemRangeChanged(actualPositionToRemove, achievementList.size()); // Zaktualizuj pozostałe pozycje
                Log.d(TAG, "Local item removed from adapter at actual position: " + actualPositionToRemove);
            } else {
                Log.w(TAG, "Could not find item by ID " + achievementId + " in local list to remove UI after delete. Original adapter position was " + positionInAdapter + ". Reloading list for safety.");
                loadAchievementsForDate(selectedDate);
            }

            if (achievementList.isEmpty() && textViewEmpty != null) {
                textViewEmpty.setText(getString(R.string.no_achievements_for_date));
                textViewEmpty.setVisibility(View.VISIBLE);
            }

        }).addOnFailureListener(e -> {
            if (getContext() == null || !isAdded()) return;
            Log.e(TAG, "FAILURE: Transaction for deleting '" + achievementToDelete.getTitle() + "' failed.", e);
            Toast.makeText(getContext(), getString(R.string.failed_to_delete_achievement) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}