package com.example.gamifylife.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Achievement {
    @DocumentId // <<-- DODAJ TĘ ADNOTACJĘ
    private String documentId; // Nazwa pola może być dowolna, np. id, documentId
    private String title;
    private String description;
    private String iconName; // Dla lokalnych drawable
    private int xpValue;
    private boolean completed;
    @ServerTimestamp
    private Date createdAt;
    private Date completedAt;
    private Date targetDate;

    public Achievement() {}

    public Achievement(String title, String description, String iconName, int xpValue, Date targetDate) {
        this.title = title;
        this.description = description;
        this.iconName = iconName;
        this.xpValue = xpValue;
        this.targetDate = targetDate;
        this.completed = false;
    }

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // Gettery i Settery dla wszystkich pól...
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public int getXpValue() { return xpValue; }
    public void setXpValue(int xpValue) { this.xpValue = xpValue; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    public Date getTargetDate() { return targetDate; }
    public void setTargetDate(Date targetDate) { this.targetDate = targetDate; }

}