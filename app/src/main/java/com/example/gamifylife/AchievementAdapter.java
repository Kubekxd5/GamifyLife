package com.example.gamifylife; // Upewnij się, że pakiet jest poprawny

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamifylife.models.Achievement;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private static final String TAG = "AchievementAdapter";

    private Context context;
    private List<Achievement> achievementList;
    private OnAchievementClickListener listener;

    public interface OnAchievementClickListener {
        void onAchievementClick(Achievement achievement);
        void onAttemptCompleteAchievement(Achievement achievement, boolean isChecked);
        void onEditAchievementClick(Achievement achievement);
        void onDeleteAchievementClick(Achievement achievement, int position);
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

        // Wyświetlanie XP
        if (achievement.getXpValue() > 0) {
            // Użyj string resource dla formatowania, np. "XP: %d" lub "%d XP"
            holder.xpTextView.setText(context.getString(R.string.list_item_xp_label, achievement.getXpValue()));
            holder.xpTextView.setVisibility(View.VISIBLE);
        } else {
            holder.xpTextView.setVisibility(View.GONE); // Ukryj, jeśli XP to 0 lub mniej
        }


        if (achievement.getIconName() != null && !achievement.getIconName().isEmpty()) {
            int resId = context.getResources().getIdentifier(achievement.getIconName(), "drawable", context.getPackageName());
            if (resId != 0) {
                holder.iconImageView.setImageResource(resId);
            } else {
                Log.w(TAG, "Drawable resource not found for iconName: " + achievement.getIconName());
                holder.iconImageView.setImageResource(R.drawable.ic_placeholder_image);
            }
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        holder.completedCheckBox.setOnCheckedChangeListener(null);
        holder.completedCheckBox.setChecked(achievement.isCompleted());
        holder.completedCheckBox.setEnabled(!achievement.isCompleted());

        holder.completedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                    Achievement clickedAchievement = achievementList.get(currentPosition);
                    listener.onAttemptCompleteAchievement(clickedAchievement, isChecked);
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

        holder.deleteButton.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onDeleteAchievementClick(achievementList.get(currentPosition), currentPosition);
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
        TextView xpTextView; // <<--- NOWE POLE
        CheckBox completedCheckBox;
        ImageButton editButton;
        ImageButton deleteButton;

        AchievementViewHolder(View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewAchievementIconItem);
            titleTextView = itemView.findViewById(R.id.textViewAchievementTitleItem);
            descriptionTextView = itemView.findViewById(R.id.textViewAchievementDescriptionItem);
            xpTextView = itemView.findViewById(R.id.textViewAchievementXpItem); // <<--- INICJALIZACJA
            completedCheckBox = itemView.findViewById(R.id.checkboxAchievementCompleted);
            editButton = itemView.findViewById(R.id.buttonEditAchievement);
            deleteButton = itemView.findViewById(R.id.buttonDeleteAchievement);
        }
    }
}