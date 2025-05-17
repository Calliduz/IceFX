package application;

public class Schedule {
    
    private String day;
    private String startTime;
    private String endTime;
    private String activity;

    // Constructor to initialize the schedule
    public Schedule(String day, String startTime, String endTime, String activity) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
    }

    // Getters for each field
    public String getDay() {
        return day;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getActivity() {
        return activity;
    }

    // Setters for each field (optional if you want to modify them later)
    public void setDay(String day) {
        this.day = day;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    @Override
    public String toString() {
        return "Schedule [day=" + day + ", startTime=" + startTime + ", endTime=" + endTime + ", activity=" + activity + "]";
    }
}
