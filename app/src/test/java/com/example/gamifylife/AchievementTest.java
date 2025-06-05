package com.example.gamifylife;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.gamifylife.models.Achievement;

import java.util.Date;

public class AchievementTest {

    @Test
    public void emptyConstructor_createsObject() {
        Achievement achievement = new Achievement();
        assertNotNull(achievement);
    }

    @Test
    public void parameterizedConstructor_setsValuesCorrectly() {
        String title = "Read a Book";
        String description = "Finish reading 'The Pragmatic Programmer'";
        String iconName = "ic_book";
        int xpValue = 50;
        Date targetDate = new Date(); // Bieżąca data dla testu

        Achievement achievement = new Achievement(title, description, iconName, xpValue, targetDate);

        assertEquals(title, achievement.getTitle());
        assertEquals(description, achievement.getDescription());
        assertEquals(iconName, achievement.getIconName());
        assertEquals(xpValue, achievement.getXpValue());
        assertEquals(targetDate, achievement.getTargetDate());
        assertFalse("New achievement should not be completed by default", achievement.isCompleted());
        assertNull("createdAt should be null initially (set by ServerTimestamp)", achievement.getCreatedAt());
        assertNull("completedAt should be null initially", achievement.getCompletedAt());
    }

    @Test
    public void gettersAndSetters_workCorrectly() {
        Achievement achievement = new Achievement();
        Date now = new Date();

        achievement.setDocumentId("doc123");
        assertEquals("doc123", achievement.getDocumentId());

        achievement.setTitle("New Title");
        assertEquals("New Title", achievement.getTitle());

        achievement.setDescription("New Description");
        assertEquals("New Description", achievement.getDescription());

        achievement.setIconName("ic_new_icon");
        assertEquals("ic_new_icon", achievement.getIconName());

        achievement.setXpValue(75);
        assertEquals(75, achievement.getXpValue());

        achievement.setCompleted(true);
        assertTrue(achievement.isCompleted());

        achievement.setCreatedAt(now);
        assertEquals(now, achievement.getCreatedAt());

        achievement.setCompletedAt(now);
        assertEquals(now, achievement.getCompletedAt());

        achievement.setTargetDate(now);
        assertEquals(now, achievement.getTargetDate());
    }

    @Test
    public void defaultValues_forParameterizedConstructor() {
        Achievement achievement = new Achievement("Test", "Desc", "icon", 100, new Date());
        // Sprawdź wartości domyślne dla pól nieustawionych w konstruktorze
        assertFalse("completed should be false by default", achievement.isCompleted());
        assertNull("createdAt should be null by default (handled by Firestore)", achievement.getCreatedAt());
        assertNull("completedAt should be null by default", achievement.getCompletedAt());
        assertNull("documentId should be null by default (handled by Firestore or setter)", achievement.getDocumentId());
    }
}