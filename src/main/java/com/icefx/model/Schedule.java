package com.icefx.model;

import javafx.beans.property.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Schedule entity representing a user's scheduled activity.
 */
public class Schedule {
    private final IntegerProperty scheduleId;
    private final IntegerProperty userId;
    private final ObjectProperty<DayOfWeek> dayOfWeek;
    private final ObjectProperty<LocalTime> startTime;
    private final ObjectProperty<LocalTime> endTime;
    private final StringProperty activity;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public Schedule(int scheduleId, int userId, DayOfWeek dayOfWeek, 
                   LocalTime startTime, LocalTime endTime, String activity) {
        this.scheduleId = new SimpleIntegerProperty(scheduleId);
        this.userId = new SimpleIntegerProperty(userId);
        this.dayOfWeek = new SimpleObjectProperty<>(dayOfWeek);
        this.startTime = new SimpleObjectProperty<>(startTime);
        this.endTime = new SimpleObjectProperty<>(endTime);
        this.activity = new SimpleStringProperty(activity);
    }

    // Constructor with string time format (for backward compatibility)
    public Schedule(String day, String startTime, String endTime, String activity) {
        this(0, 0, parseDayOfWeek(day), parseTime(startTime), parseTime(endTime), activity);
    }

    // Getters
    public int getScheduleId() { return scheduleId.get(); }
    public IntegerProperty scheduleIdProperty() { return scheduleId; }

    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek.get(); }
    public ObjectProperty<DayOfWeek> dayOfWeekProperty() { return dayOfWeek; }

    public LocalTime getStartTime() { return startTime.get(); }
    public ObjectProperty<LocalTime> startTimeProperty() { return startTime; }

    public LocalTime getEndTime() { return endTime.get(); }
    public ObjectProperty<LocalTime> endTimeProperty() { return endTime; }

    public String getActivity() { return activity.get(); }
    public StringProperty activityProperty() { return activity; }

    // Formatted getters for display
    public String getDay() {
        return dayOfWeek.get() != null ? dayOfWeek.get().toString() : "";
    }

    public String getFormattedStartTime() {
        return startTime.get() != null ? startTime.get().format(TIME_FORMATTER) : "";
    }

    public String getFormattedEndTime() {
        return endTime.get() != null ? endTime.get().format(TIME_FORMATTER) : "";
    }

    // Setters
    public void setScheduleId(int scheduleId) { this.scheduleId.set(scheduleId); }
    public void setUserId(int userId) { this.userId.set(userId); }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek.set(dayOfWeek); }
    public void setStartTime(LocalTime startTime) { this.startTime.set(startTime); }
    public void setEndTime(LocalTime endTime) { this.endTime.set(endTime); }
    public void setActivity(String activity) { this.activity.set(activity); }

    // Utility methods
    public boolean isActiveNow() {
        LocalTime now = LocalTime.now();
        DayOfWeek today = DayOfWeek.from(java.time.LocalDate.now());
        return dayOfWeek.get() == today && 
               !now.isBefore(startTime.get()) && 
               !now.isAfter(endTime.get());
    }

    private static DayOfWeek parseDayOfWeek(String day) {
        try {
            return DayOfWeek.valueOf(day.toUpperCase());
        } catch (Exception e) {
            // Try to match partial names
            for (DayOfWeek dow : DayOfWeek.values()) {
                if (dow.name().toLowerCase().startsWith(day.toLowerCase())) {
                    return dow;
                }
            }
            return DayOfWeek.MONDAY; // default
        }
    }

    private static LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception e) {
            return LocalTime.of(8, 0); // default 8:00 AM
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s-%s: %s", 
            getDay(), getFormattedStartTime(), getFormattedEndTime(), activity.get());
    }
}
