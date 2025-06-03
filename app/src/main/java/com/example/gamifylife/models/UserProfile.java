package com.example.gamifylife.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class UserProfile {
    private String userId; // Same as Firebase Auth UID
    private String nickname;
    private long totalXp;
    private int level;
    private long achievementsCompleted;
    @ServerTimestamp
    private Date accountCreatedAt; // Set when the profile is first created

    // Constructors
    public UserProfile() {}

    public UserProfile(String userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
        this.totalXp = 0;
        this.level = 1; // Start at level 1
        this.achievementsCompleted = 0;
        // accountCreatedAt will be set by @ServerTimestamp
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public long getTotalXp() { return totalXp; }
    public void setTotalXp(long totalXp) { this.totalXp = totalXp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public long getAchievementsCompleted() { return achievementsCompleted; }
    public void setAchievementsCompleted(long achievementsCompleted) { this.achievementsCompleted = achievementsCompleted; }

    public Date getAccountCreatedAt() { return accountCreatedAt; }
    public void setAccountCreatedAt(Date accountCreatedAt) { this.accountCreatedAt = accountCreatedAt; }
}