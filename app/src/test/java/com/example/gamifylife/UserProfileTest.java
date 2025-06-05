package com.example.gamifylife;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.gamifylife.models.UserProfile;

import java.util.Date;

public class UserProfileTest {

    @Test
    public void emptyConstructor_createsObject() {
        UserProfile userProfile = new UserProfile();
        assertNotNull(userProfile);
    }

    @Test
    public void parameterizedConstructor_setsValuesCorrectly() {
        String userId = "user123";
        String nickname = "TestUser";

        UserProfile userProfile = new UserProfile(userId, nickname);

        assertEquals(userId, userProfile.getUserId());
        assertEquals(nickname, userProfile.getNickname());
        assertEquals("Initial totalXp should be 0", 0, userProfile.getTotalXp());
        assertEquals("Initial level should be 1", 1, userProfile.getLevel());
        assertEquals("Initial achievementsCompleted should be 0", 0, userProfile.getAchievementsCompleted());
        assertNull("accountCreatedAt should be null initially (set by ServerTimestamp)", userProfile.getAccountCreatedAt());
    }

    @Test
    public void gettersAndSetters_workCorrectly() {
        UserProfile userProfile = new UserProfile();
        Date now = new Date();

        userProfile.setUserId("newUser");
        assertEquals("newUser", userProfile.getUserId());

        userProfile.setNickname("NewNick");
        assertEquals("NewNick", userProfile.getNickname());

        userProfile.setTotalXp(1500);
        assertEquals(1500, userProfile.getTotalXp());

        userProfile.setLevel(5);
        assertEquals(5, userProfile.getLevel());

        userProfile.setAchievementsCompleted(10);
        assertEquals(10, userProfile.getAchievementsCompleted());

        userProfile.setAccountCreatedAt(now);
        assertEquals(now, userProfile.getAccountCreatedAt());
    }

    @Test
    public void defaultValues_forParameterizedConstructor() {
        UserProfile userProfile = new UserProfile("uid", "nick");
        assertEquals(0, userProfile.getTotalXp());
        assertEquals(1, userProfile.getLevel());
        assertEquals(0, userProfile.getAchievementsCompleted());
        assertNull(userProfile.getAccountCreatedAt());
    }
}