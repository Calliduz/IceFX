package com.icefx.dao;

import com.icefx.config.DatabaseConfig;
import com.icefx.model.Schedule;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Schedule entities.
 */
public class ScheduleDAO {
    
    /**
     * Create a new schedule for a user
     */
    public int createSchedule(int userId, Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules (person_id, day, start_time, end_time, activity) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, userId);
            ps.setString(2, schedule.getDayOfWeek().name());
            ps.setTime(3, Time.valueOf(schedule.getStartTime()));
            ps.setTime(4, Time.valueOf(schedule.getEndTime()));
            ps.setString(5, schedule.getActivity());
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int scheduleId = rs.getInt(1);
                        schedule.setScheduleId(scheduleId);
                        schedule.setUserId(userId);
                        return scheduleId;
                    }
                }
            }
            
            throw new SQLException("Failed to create schedule, no ID obtained");
        }
    }
    
    /**
     * Get all schedules for a user
     */
    public List<Schedule> findByUserId(int userId) throws SQLException {
        String sql = "SELECT schedule_id, person_id, day, start_time, end_time, activity " +
                    "FROM schedules WHERE person_id = ? ORDER BY " +
                    "FIELD(day, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'), " +
                    "start_time";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Get schedules for a user on a specific day
     */
    public List<Schedule> findByUserIdAndDay(int userId, DayOfWeek day) throws SQLException {
        String sql = "SELECT schedule_id, person_id, day, start_time, end_time, activity " +
                    "FROM schedules WHERE person_id = ? AND day = ? ORDER BY start_time";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, day.name());
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Get active schedule for a user at current time
     */
    public List<Schedule> findActiveSchedules(int userId) throws SQLException {
        DayOfWeek today = DayOfWeek.from(java.time.LocalDate.now());
        LocalTime now = LocalTime.now();
        
        String sql = "SELECT schedule_id, person_id, day, start_time, end_time, activity " +
                    "FROM schedules WHERE person_id = ? AND day = ? AND start_time <= ? AND end_time >= ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, today.name());
            ps.setTime(3, Time.valueOf(now));
            ps.setTime(4, Time.valueOf(now));
            
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Update a schedule
     */
    public boolean update(Schedule schedule) throws SQLException {
        String sql = "UPDATE schedules SET day = ?, start_time = ?, end_time = ?, activity = ? " +
                    "WHERE schedule_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, schedule.getDayOfWeek().name());
            ps.setTime(2, Time.valueOf(schedule.getStartTime()));
            ps.setTime(3, Time.valueOf(schedule.getEndTime()));
            ps.setString(4, schedule.getActivity());
            ps.setInt(5, schedule.getScheduleId());
            
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete a schedule
     */
    public boolean delete(int scheduleId) throws SQLException {
        String sql = "DELETE FROM schedules WHERE schedule_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, scheduleId);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete schedule by matching fields (for backward compatibility)
     */
    public boolean deleteByFields(int userId, Schedule schedule) throws SQLException {
        String sql = "DELETE FROM schedules WHERE person_id = ? AND day = ? AND start_time = ? AND end_time = ? AND activity = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, schedule.getDayOfWeek().name());
            ps.setTime(3, Time.valueOf(schedule.getStartTime()));
            ps.setTime(4, Time.valueOf(schedule.getEndTime()));
            ps.setString(5, schedule.getActivity());
            
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete all schedules for a user
     */
    public boolean deleteByUserId(int userId) throws SQLException {
        String sql = "DELETE FROM schedules WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Check if schedule conflicts with existing schedules
     */
    public boolean hasConflict(int userId, DayOfWeek day, LocalTime startTime, LocalTime endTime, int excludeScheduleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM schedules " +
                    "WHERE person_id = ? AND day = ? AND schedule_id != ? " +
                    "AND ((start_time <= ? AND end_time >= ?) OR " +
                    "(start_time <= ? AND end_time >= ?) OR " +
                    "(start_time >= ? AND end_time <= ?))";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, day.name());
            ps.setInt(3, excludeScheduleId);
            ps.setTime(4, Time.valueOf(startTime));
            ps.setTime(5, Time.valueOf(startTime));
            ps.setTime(6, Time.valueOf(endTime));
            ps.setTime(7, Time.valueOf(endTime));
            ps.setTime(8, Time.valueOf(startTime));
            ps.setTime(9, Time.valueOf(endTime));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to list of Schedule objects
     */
    private List<Schedule> mapResultSet(ResultSet rs) throws SQLException {
        List<Schedule> schedules = new ArrayList<>();
        
        while (rs.next()) {
            int scheduleId = rs.getInt("schedule_id");
            int userId = rs.getInt("person_id");
            DayOfWeek day = DayOfWeek.valueOf(rs.getString("day"));
            LocalTime startTime = rs.getTime("start_time").toLocalTime();
            LocalTime endTime = rs.getTime("end_time").toLocalTime();
            String activity = rs.getString("activity");
            
            schedules.add(new Schedule(scheduleId, userId, day, startTime, endTime, activity));
        }
        
        return schedules;
    }
}
