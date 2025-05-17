package application;

import java.time.LocalDateTime;

public class AttendanceRecord {
    private LocalDateTime eventTime;
    private String fullName;
    private String eventType;
    private String activity;

    public AttendanceRecord(LocalDateTime eventTime, String fullName, String eventType, String activity) {
        this.eventTime = eventTime;
        this.fullName = fullName;
        this.eventType = eventType;
        this.activity = activity;
    }

    public LocalDateTime getEventTime() { return eventTime; }
    public String getFullName() { return fullName; }
    public String getEventType() { return eventType; }
    public String getActivity() { return activity; }
}
