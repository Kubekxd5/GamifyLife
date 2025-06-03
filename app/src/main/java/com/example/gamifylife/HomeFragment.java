package com.example.gamifylife;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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

import com.example.gamifylife.models.UserProfile;
import com.example.gamifylife.util.LevelUtils;
import com.example.gamifylife.helpers.LocaleHelper; // Assuming your LocaleHelper is here
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.EventListener; // Import this

import java.text.SimpleDateFormat;
import java.util.Locale;


public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration userProfileListener;

    private TextView textViewNickname, textViewLevelValue, textViewAchievementsCompletedValue, textViewAccountCreatedValue;
    private TextView textViewXPCurrent, textViewXPTotalForLevel;
    private ProgressBar progressBarXP, progressBarHome; // Use progressBarHome for fragment loading
    private ImageButton buttonEditNickname;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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
        progressBarHome = view.findViewById(R.id.progressBarHome); // For fragment's own loading indication

        if (currentUser == null) {
            // This should ideally be handled by MainActivity or LoginActivity
            // If fragment is loaded somehow without a user, show an error or navigate
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        buttonEditNickname.setOnClickListener(v -> showEditNicknameDialog());
        // loadUserProfile(); // Called in onStart
    }

    private void loadUserProfile() {
        if (currentUser != null && db != null) {
            if (progressBarHome != null) progressBarHome.setVisibility(View.VISIBLE);

            DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());

            // If a listener already exists, remove it before adding a new one
            // This is crucial to prevent multiple listeners on the same document
            if (userProfileListener != null) {
                userProfileListener.remove();
                userProfileListener = null; // Good practice to nullify
                Log.d(TAG, "Previous userProfileListener removed.");
            }

            Log.d(TAG, "Attaching new userProfileListener.");
            userProfileListener = userDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    // Check if the fragment is still added to an activity and has a context
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "HomeFragment not attached to activity or context is null, ignoring snapshot event.");
                        if (userProfileListener != null) {
                            // If fragment is being destroyed, good to clean up immediately
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
                        // Consider what to do here: clear UI, show specific message
                        // For example, clear the nickname field if profile is null
                        if (textViewNickname != null) textViewNickname.setText(getString(R.string.profile_not_found_placeholder)); // Add new string
                    }
                }
            });
        } else {
            Log.w(TAG, "Cannot load profile: currentUser or db is null.");
            if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);
        }
    }

    private void updateUIWithProfile(UserProfile profile) {
        if (textViewNickname == null) return; // Views might not be initialized yet or fragment detached

        textViewNickname.setText(profile.getNickname());
        textViewLevelValue.setText(String.valueOf(profile.getLevel()));
        textViewAchievementsCompletedValue.setText(String.valueOf(profile.getAchievementsCompleted()));

        if (profile.getAccountCreatedAt() != null && getContext() != null) {
            // Determine locale for date format
            String lang = LocaleHelper.getLanguage(getContext());
            Locale currentLocale = lang.equals("pl") ? new Locale("pl", "PL") : Locale.getDefault();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", currentLocale);
            textViewAccountCreatedValue.setText(sdf.format(profile.getAccountCreatedAt()));
        } else {
            textViewAccountCreatedValue.setText("N/A");
        }

        long currentXpInLevel = LevelUtils.getXPProgressInCurrentLevel(profile.getTotalXp());
        int xpNeededForNext = LevelUtils.getXPTotalForCurrentLevelBar(profile.getTotalXp());

        textViewXPCurrent.setText(getString(R.string.xp_format, (int)currentXpInLevel));
        textViewXPTotalForLevel.setText(getString(R.string.xp_slash_total_format, xpNeededForNext));

        progressBarXP.setMax(xpNeededForNext > 0 ? xpNeededForNext : 100);
        progressBarXP.setProgress((int)currentXpInLevel);
    }

    private void showEditNicknameDialog() {
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
                Toast.makeText(getContext(), "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_button_text), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateNicknameInFirestore(String newNickname) {
        if (currentUser == null || db == null) return;
        if (progressBarHome != null) progressBarHome.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUser.getUid())
                .update("nickname", newNickname)
                .addOnSuccessListener(aVoid -> {
                    if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);
                    if (getContext() != null) Toast.makeText(getContext(), getString(R.string.nickname_updated), Toast.LENGTH_SHORT).show();
                    // UI will update via snapshot listener
                })
                .addOnFailureListener(e -> {
                    if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);
                    if (getContext() != null) Toast.makeText(getContext(), getString(R.string.nickname_update_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        if (currentUser != null) {
            // It's generally safe to attach listeners in onStart
            loadUserProfile();
        } else {
            // Handle user not logged in (e.g., navigate to login or show message)
            Log.w(TAG, "User is null in onStart, cannot load profile.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "User not signed in.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        if (userProfileListener != null) {
            Log.d(TAG, "Removing userProfileListener in onStop.");
            userProfileListener.remove();
            userProfileListener = null; // Important to allow re-attachment in onStart
        }
    }

}
