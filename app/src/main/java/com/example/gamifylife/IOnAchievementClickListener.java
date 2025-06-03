package com.example.gamifylife;

import com.example.gamifylife.models.Achievement;

public interface IOnAchievementClickListener {
    void onAchievementClick(Achievement achievement);
    void onAttemptCompleteAchievement(Achievement achievement, boolean isChecked); // This was for the transaction logic
    void onEditAchievementClick(Achievement achievement);
}
