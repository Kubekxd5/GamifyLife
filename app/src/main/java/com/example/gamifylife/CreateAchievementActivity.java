package com.example.gamifylife;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.example.gamifylife.models.Achievement;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.DatePickerDialog;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateAchievementActivity extends BaseActivity {

    private static final String TAG = "CreateAchievement";

    private TextInputEditText editTextTitle, editTextDescription, editTextXp;
    private Spinner spinnerIcon;
    private ImageView imageViewSelectedIconPreview;
    private Button buttonSaveAchievement;
    private ProgressBar progressBar;
    private Button buttonSelectTargetDate;
    private TextView textViewTargetDate;

    private Calendar selectedDateCalendar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isInEditMode = false;
    private String editingAchievementId;
    private Achievement existingAchievement;

    private Map<String, String> iconMap;
    private String selectedIconName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_achievement);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextTitle = findViewById(R.id.editTextAchievementTitle);
        editTextDescription = findViewById(R.id.editTextAchievementDescription);
        editTextXp = findViewById(R.id.editTextAchievementXp);
        spinnerIcon = findViewById(R.id.spinnerAchievementIcon);
        imageViewSelectedIconPreview = findViewById(R.id.imageViewSelectedIconPreview);
        buttonSaveAchievement = findViewById(R.id.buttonSaveAchievement);
        progressBar = findViewById(R.id.progressBarCreateAchievement);
        buttonSelectTargetDate = findViewById(R.id.buttonSelectTargetDate);
        textViewTargetDate = findViewById(R.id.textViewTargetDate);

        selectedDateCalendar = Calendar.getInstance();

        if (getIntent().hasExtra("DEFAULT_TARGET_DATE") && !getIntent().hasExtra("EDIT_MODE")) {
            long defaultDateMillis = getIntent().getLongExtra("DEFAULT_TARGET_DATE", System.currentTimeMillis());
            selectedDateCalendar.setTimeInMillis(defaultDateMillis);
        }
        selectedDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
        selectedDateCalendar.set(Calendar.MINUTE, 0);
        selectedDateCalendar.set(Calendar.SECOND, 0);
        selectedDateCalendar.set(Calendar.MILLISECOND, 0);
        updateTargetDateLabel();

        if (getIntent().hasExtra("EDIT_MODE")) {
            isInEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
            editingAchievementId = getIntent().getStringExtra("ACHIEVEMENT_ID");
        }

        if (isInEditMode && editingAchievementId != null) {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.edit_achievement_title));
            }
            buttonSaveAchievement.setText(getString(R.string.button_update_achievement));
            loadAchievementData();
        } else {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.create_achievement_title));
            }
            buttonSaveAchievement.setText(getString(R.string.button_save_achievement));
        }

        buttonSelectTargetDate.setOnClickListener(v -> showDatePickerDialog());
        textViewTargetDate.setOnClickListener(v -> showDatePickerDialog());

        setupIconSpinner();
        buttonSaveAchievement.setOnClickListener(v -> handleSaveOrUpdate());
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
            selectedDateCalendar.set(Calendar.MINUTE, 0);
            selectedDateCalendar.set(Calendar.SECOND, 0);
            selectedDateCalendar.set(Calendar.MILLISECOND, 0);
            updateTargetDateLabel();
        };

        new DatePickerDialog(CreateAchievementActivity.this, dateSetListener,
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateTargetDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        textViewTargetDate.setText(sdf.format(selectedDateCalendar.getTime()));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupIconSpinner() {
        iconMap = new HashMap<>();
        iconMap.put(getString(R.string.icon_name_book), "ic_achievement_book");
        iconMap.put(getString(R.string.icon_name_sport), "ic_achievement_sport");
        iconMap.put(getString(R.string.icon_name_code), "ic_achievement_code");
        iconMap.put(getString(R.string.icon_name_default), "ic_achievement_default");
        iconMap.put(getString(R.string.icon_name_finance), "ic_achievement_finance");
        iconMap.put(getString(R.string.icon_name_health), "ic_achievement_health");
        iconMap.put(getString(R.string.icon_name_hobby), "ic_achievement_hobby");
        iconMap.put(getString(R.string.icon_name_work), "ic_achievement_work");
        iconMap.put(getString(R.string.icon_name_study), "ic_achievement_study");
        iconMap.put(getString(R.string.icon_name_gaming), "ic_achievement_game");



        List<String> displayNames = new ArrayList<>(iconMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIcon.setAdapter(adapter);

        spinnerIcon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String displayName = (String) parent.getItemAtPosition(position);
                selectedIconName = iconMap.get(displayName);
                if (selectedIconName != null) {
                    int resId = getResources().getIdentifier(selectedIconName, "drawable", getPackageName());
                    if (resId != 0) {
                        imageViewSelectedIconPreview.setImageResource(resId);
                    } else {
                        imageViewSelectedIconPreview.setImageResource(R.drawable.ic_placeholder_image);
                        Log.w(TAG, "Drawable not found for icon: " + selectedIconName);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedIconName = iconMap.get(getString(R.string.icon_name_default)); // Domyślna ikona
                if(selectedIconName != null) {
                    int resId = getResources().getIdentifier(selectedIconName, "drawable", getPackageName());
                    if (resId != 0) imageViewSelectedIconPreview.setImageResource(resId);
                    else imageViewSelectedIconPreview.setImageResource(R.drawable.ic_placeholder_image);
                } else {
                    imageViewSelectedIconPreview.setImageResource(R.drawable.ic_placeholder_image);
                }
            }
        });

        if (!isInEditMode && !displayNames.isEmpty()) {
            spinnerIcon.setSelection(displayNames.indexOf(getString(R.string.icon_name_default))); // Ustaw domyślną
        }
    }

    private void loadAchievementData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || editingAchievementId == null) {
            Toast.makeText(this, getString(R.string.error_loading_data_no_user_id), Toast.LENGTH_SHORT).show(); // Dodaj string
            finish();
            return;
        }
        setLoading(true);
        db.collection("users").document(currentUser.getUid())
                .collection("achievements").document(editingAchievementId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        existingAchievement = documentSnapshot.toObject(Achievement.class);
                        if (existingAchievement != null) {
                            // existingAchievement.setDocumentId(documentSnapshot.getId()); // Jeśli nie używasz @DocumentId

                            editTextTitle.setText(existingAchievement.getTitle());
                            editTextDescription.setText(existingAchievement.getDescription());
                            editTextXp.setText(String.valueOf(existingAchievement.getXpValue()));

                            String currentIconName = existingAchievement.getIconName();
                            if (currentIconName != null && iconMap != null) {
                                for (Map.Entry<String, String> entry : iconMap.entrySet()) {
                                    if (currentIconName.equals(entry.getValue())) {
                                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerIcon.getAdapter();
                                        if (adapter != null) {
                                            int position = adapter.getPosition(entry.getKey());
                                            if (position >= 0) {
                                                spinnerIcon.setSelection(position);
                                            } else {
                                                Log.w(TAG, "Icon display name not found in adapter for: " + entry.getKey());
                                                spinnerIcon.setSelection(0); // Fallback to first item
                                            }
                                        }
                                        break;
                                    }
                                }
                            } else {
                                spinnerIcon.setSelection(0); // Fallback if no icon name
                            }

                            if (existingAchievement.getTargetDate() != null) {
                                selectedDateCalendar.setTime(existingAchievement.getTargetDate());
                                updateTargetDateLabel();
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.error_loading_achievements), Toast.LENGTH_SHORT).show(); // Dodaj string
                            finish();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.achievement_not_found_for_editing), Toast.LENGTH_SHORT).show(); // Dodaj string
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_loading_achievement_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show(); // Dodaj string
                    Log.e(TAG, "Error loading achievement for edit", e);
                    finish();
                });
    }

    private void handleSaveOrUpdate() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String xpStr = editTextXp.getText().toString().trim();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_logged_in_error), Toast.LENGTH_LONG).show(); // Dodaj string
            return;
        }
        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError(getString(R.string.title_cannot_be_empty));
            editTextTitle.requestFocus();
            return;
        }
        if (selectedIconName == null || selectedIconName.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_icon), Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(xpStr)) {
            editTextXp.setError(getString(R.string.xp_cannot_be_empty));
            editTextXp.requestFocus();
            return;
        }

        int xpValue;
        try {
            xpValue = Integer.parseInt(xpStr);
            if (xpValue < 1 || xpValue > 100) {
                editTextXp.setError(getString(R.string.error_xp_out_of_range));
                editTextXp.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            editTextXp.setError(getString(R.string.error_invalid_xp));
            editTextXp.requestFocus();
            return;
        }

        // Prostsza walidacja daty - jeśli tekst jest domyślny (co nie powinno się zdarzyć, bo inicjujemy na dzisiaj)
        // lub jeśli selectedDateCalendar jest null (też nie powinno, ale dla bezpieczeństwa)
        if (selectedDateCalendar == null) {
            Toast.makeText(this, getString(R.string.please_select_target_date), Toast.LENGTH_SHORT).show();
            return;
        }
        Date targetDate = selectedDateCalendar.getTime();

        setLoading(true);

        if (isInEditMode && editingAchievementId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title);
            updates.put("description", description);
            updates.put("iconName", selectedIconName);
            updates.put("xpValue", xpValue);
            updates.put("targetDate", targetDate);

            if (existingAchievement != null) {
                updates.put("completed", existingAchievement.isCompleted());
                updates.put("completedAt", existingAchievement.isCompleted() ? existingAchievement.getCompletedAt() : null);
            } else {
                updates.put("completed", false); // Domyślnie, jeśli existingAchievement jest null
                updates.put("completedAt", null);
            }

            db.collection("users").document(currentUser.getUid())
                    .collection("achievements").document(editingAchievementId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        setLoading(false);
                        Toast.makeText(CreateAchievementActivity.this, getString(R.string.achievement_updated_successfully), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(CreateAchievementActivity.this, getString(R.string.achievement_update_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating document", e);
                    });
        } else {
            Achievement newAchievement = new Achievement(
                    title,
                    description,
                    selectedIconName,
                    xpValue,
                    targetDate
            );

            db.collection("users").document(currentUser.getUid()).collection("achievements")
                    .add(newAchievement)
                    .addOnSuccessListener(documentReference -> {
                        setLoading(false);
                        Toast.makeText(CreateAchievementActivity.this, getString(R.string.achievement_saved_successfully), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(CreateAchievementActivity.this, getString(R.string.achievement_save_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error adding document", e);
                    });
        }
    }

    private void setLoading(boolean isLoading) {
        editTextTitle.setEnabled(!isLoading);
        editTextDescription.setEnabled(!isLoading);
        editTextXp.setEnabled(!isLoading);
        spinnerIcon.setEnabled(!isLoading);
        buttonSaveAchievement.setEnabled(!isLoading);
        buttonSelectTargetDate.setEnabled(!isLoading);
        textViewTargetDate.setClickable(!isLoading);

        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonSaveAchievement.setText(getString(R.string.saving_button_text));
        } else {
            progressBar.setVisibility(View.GONE);
            buttonSaveAchievement.setText(isInEditMode ? getString(R.string.button_update_achievement) : getString(R.string.button_save_achievement));
        }
    }
}