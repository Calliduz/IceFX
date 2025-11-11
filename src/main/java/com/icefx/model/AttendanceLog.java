package com.icefx.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AttendanceLog entity for tracking user check-ins and check-outs.
 */
public class AttendanceLog {
    private final IntegerProperty logId;
    private final IntegerProperty userId;
    private final StringProperty userName;
    private final ObjectProperty<LocalDateTime> eventTime;
    private final StringProperty eventType; // "Time In" or "Time Out"
    private final StringProperty activity;
    private final StringProperty cameraId;
    private final DoubleProperty confidence;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public AttendanceLog(int logId, int userId, String userName, LocalDateTime eventTime, 
                        String eventType, String activity, String cameraId, double confidence) {
        this.logId = new SimpleIntegerProperty(logId);
        this.userId = new SimpleIntegerProperty(userId);
        this.userName = new SimpleStringProperty(userName);
        this.eventTime = new SimpleObjectProperty<>(eventTime);
        this.eventType = new SimpleStringProperty(eventType);
        this.activity = new SimpleStringProperty(activity);
        this.cameraId = new SimpleStringProperty(cameraId);
        this.confidence = new SimpleDoubleProperty(confidence);
    }

    // Simplified constructor
    public AttendanceLog(LocalDateTime eventTime, String userName, String eventType, String activity) {
        this(0, 0, userName, eventTime, eventType, activity, "CAM1", 0.0);
    }

    // Getters
    public int getLogId() { return logId.get(); }
    public IntegerProperty logIdProperty() { return logId; }

    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }

    public String getUserName() { return userName.get(); }
    public StringProperty userNameProperty() { return userName; }

    public LocalDateTime getEventTime() { return eventTime.get(); }
    public ObjectProperty<LocalDateTime> eventTimeProperty() { return eventTime; }

    public String getEventType() { return eventType.get(); }
    public StringProperty eventTypeProperty() { return eventType; }

    public String getActivity() { return activity.get(); }
    public StringProperty activityProperty() { return activity; }

    public String getCameraId() { return cameraId.get(); }
    public StringProperty cameraIdProperty() { return cameraId; }

    public double getConfidence() { return confidence.get(); }
    public DoubleProperty confidenceProperty() { return confidence; }

    // Formatted properties for TableView display
    public String getFormattedTime() {
        return eventTime.get() != null ? eventTime.get().format(TIME_FORMATTER) : "";
    }

    public String getFormattedDate() {
        return eventTime.get() != null ? eventTime.get().format(DATE_FORMATTER) : "";
    }

    // Setters
    public void setLogId(int logId) { this.logId.set(logId); }
    public void setUserId(int userId) { this.userId.set(userId); }
    public void setUserName(String userName) { this.userName.set(userName); }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime.set(eventTime); }
    public void setEventType(String eventType) { this.eventType.set(eventType); }
    public void setActivity(String activity) { this.activity.set(activity); }
    public void setCameraId(String cameraId) { this.cameraId.set(cameraId); }
    public void setConfidence(double confidence) { this.confidence.set(confidence); }

    @Override
    public String toString() {
        return String.format("%s - %s [%s] at %s", 
            userName.get(), eventType.get(), activity.get(), getFormattedTime());
    }
}
