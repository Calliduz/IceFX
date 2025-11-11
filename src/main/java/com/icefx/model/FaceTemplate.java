package com.icefx.model;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * FaceTemplate entity storing facial recognition data for a user.
 */
public class FaceTemplate {
    private int templateId;
    private int userId;
    private byte[] templateData;
    private LocalDateTime createdAt;
    private boolean isPrimary;

    public FaceTemplate(int templateId, int userId, byte[] templateData, 
                       LocalDateTime createdAt, boolean isPrimary) {
        this.templateId = templateId;
        this.userId = userId;
        this.templateData = templateData;
        this.createdAt = createdAt;
        this.isPrimary = isPrimary;
    }

    public FaceTemplate(int userId, byte[] templateData) {
        this(0, userId, templateData, LocalDateTime.now(), true);
    }

    // Getters
    public int getTemplateId() { return templateId; }
    public int getUserId() { return userId; }
    public byte[] getTemplateData() { return templateData; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isPrimary() { return isPrimary; }

    // Setters
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTemplateData(byte[] templateData) { this.templateData = templateData; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaceTemplate that = (FaceTemplate) o;
        return templateId == that.templateId && Arrays.equals(templateData, that.templateData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(templateData);
    }
}
