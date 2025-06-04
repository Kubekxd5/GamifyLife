package com.example.gamifylife;

import com.example.gamifylife.models.Achievement;

public interface IOnAchievementClickListener {
    void onAchievementClick(Achievement achievement);
    void onAttemptCompleteAchievement(Achievement achievement, boolean isChecked);
    void onEditAchievementClick(Achievement achievement);
    void onDeleteAchievementClick(Achievement achievement, int position);
}
