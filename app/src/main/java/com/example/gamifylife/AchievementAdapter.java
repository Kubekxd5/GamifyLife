package com.example.gamifylife; // Make sure this package is correct

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
// import android.widget.Toast; // No longer needed here if logic moved out

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide
import com.example.gamifylife.models.Achievement; // Ensure this path is correct

// Removed unused imports like UserProfile, FirebaseFirestore related ones

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private static final String TAG = "AchievementAdapter";

    private Context context;
    private List<Achievement> achievementList;
    private OnAchievementClickListener listener;

    public interface OnAchievementClickListener {
        void onAchievementClick(Achievement achievement);
        void onAttemptCompleteAchievement(Achievement achievement, boolean isChecked); // This was for the transaction logic
        void onEditAchievementClick(Achievement achievement);
    }

    public AchievementAdapter(Context context, List<Achievement> achievementList, OnAchievementClickListener listener) {
        this.context = context;
        this.achievementList = achievementList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievementList.get(position);
        holder.titleTextView.setText(achievement.getTitle());

        if (achievement.getDescription() != null && !achievement.getDescription().isEmpty()) {
            holder.descriptionTextView.setText(achievement.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        // --- Image Loading with Glide ---
        if (achievement.getIconName() != null && !achievement.getIconName().isEmpty()) {
            int resId = context.getResources().getIdentifier(achievement.getIconName(), "drawable", context.getPackageName());
            if (resId != 0) {
                holder.iconImageView.setImageResource(resId);
            } else {
                Log.w(TAG, "Drawable resource not found for iconName: " + achievement.getIconName());
                holder.iconImageView.setImageResource(R.drawable.ic_placeholder_image); // Fallback
            }
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_placeholder_image); // Default placeholder
        }
        // --- End Image Loading ---

        // Setup CheckBox
        holder.completedCheckBox.setOnCheckedChangeListener(null); // Avoid listener firing during bind
        holder.completedCheckBox.setChecked(achievement.isCompleted());
        // Disable checkbox if already completed to prevent un-completing via checkbox directly
        // Un-completing should be a more deliberate action if allowed at all.
        holder.completedCheckBox.setEnabled(!achievement.isCompleted());


        holder.completedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Ważne, aby reagować tylko na interakcję użytkownika
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                    Achievement clickedAchievement = achievementList.get(currentPosition);
                    Log.d(TAG, "Adapter: CheckBox user interaction: " + clickedAchievement.getTitle() + ", isChecked: " + isChecked);
                    listener.onAttemptCompleteAchievement(clickedAchievement, isChecked); // Wywołanie poprawnej metody
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onAchievementClick(achievementList.get(currentPosition));
            }
        });

        holder.editButton.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onEditAchievementClick(achievementList.get(currentPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return achievementList.size();
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        CheckBox completedCheckBox;
        ImageButton editButton;

        AchievementViewHolder(View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewAchievementIconItem);
            titleTextView = itemView.findViewById(R.id.textViewAchievementTitleItem);
            descriptionTextView = itemView.findViewById(R.id.textViewAchievementDescriptionItem);
            completedCheckBox = itemView.findViewById(R.id.checkboxAchievementCompleted);
            editButton = itemView.findViewById(R.id.buttonEditAchievement);
        }
    }

    // The markAchievementComplete method has been REMOVED from here.
    // It should be implemented in the Fragment/Activity that uses this adapter.
}