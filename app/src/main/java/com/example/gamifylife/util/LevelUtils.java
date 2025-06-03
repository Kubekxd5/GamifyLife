package com.example.gamifylife.util;

public class LevelUtils {

    // XP needed to reach this level FROM the previous one
    public static int getXPForNextLevel(int currentLevel) {
        if (currentLevel <= 0) return 100; // XP to reach level 1 (or from level 0)
        return 100 * currentLevel;
    }

    // Total XP accumulated to reach this level
    public static long getTotalXPForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0; // Level 1 is 0 XP
        long totalXp = 0;
        for (int i = 1; i < targetLevel; i++) {
            totalXp += getXPForNextLevel(i);
        }
        return totalXp;
    }

    public static int calculateLevel(long totalXp) {
        int level = 1;
        long xpForNext = getXPForNextLevel(level);
        long accumulatedXpForLevel = 0;

        while (totalXp >= accumulatedXpForLevel + xpForNext) {
            accumulatedXpForLevel += xpForNext;
            level++;
            xpForNext = getXPForNextLevel(level);
        }
        return level;
    }

    // XP progressed within the current level
    public static long getXPProgressInCurrentLevel(long totalXp) {
        int currentLevel = calculateLevel(totalXp);
        long xpRequiredForCurrentLevel = getTotalXPForLevel(currentLevel);
        return totalXp - xpRequiredForCurrentLevel;
    }

    // Total XP needed for the current level to complete (i.e. to reach next level)
    public static int getXPTotalForCurrentLevelBar(long totalXp) {
        int currentLevel = calculateLevel(totalXp);
        return getXPForNextLevel(currentLevel);
    }
}