package com.example.gamifylife;

import android.annotation.SuppressLint;
import android.content.Intent;
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