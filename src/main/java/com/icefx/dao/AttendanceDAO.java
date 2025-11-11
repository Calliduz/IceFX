package com.icefx.dao;

import com.icefx.config.DatabaseConfig;
import com.icefx.model.AttendanceLog;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for AttendanceLog entities.
 */
public class AttendanceDAO {
    
    /**
     * Log attendance (Time In or Time Out)
     */
    public int logAttendance(AttendanceLog log) throws SQLException {
        String sql = "INSERT INTO attendance_logs (person_id, event_time, event_type, camera_id, confidence, activity, snapshot) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, log.getUserId());
            ps.setTimestamp(2, Timestamp.valueOf(log.getEventTime()));
            ps.setString(3, log.getEventType());
            ps.setString(4, log.getCameraId());
            ps.setDouble(5, log.getConfidence());
            ps.setString(6, log.getActivity());
            ps.setBytes(7, null); // snapshot can be null or passed in log
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int logId = rs.getInt(1);
                        log.setLogId(logId);
                        return logId;
                    }
                }
            }
            
            throw new SQLException("Failed to log attendance, no ID obtained");
        }
    }
    
    /**
     * Get all attendance logs for a specific user
     */
    public List<AttendanceLog> findByUserId(int userId) throws SQLException {
        String sql = "SELECT al.log_id, al.person_id, p.full_name, al.event_time, al.event_type, " +
                    "al.activity, al.camera_id, al.confidence " +
                    "FROM attendance_logs al " +
                    "JOIN persons p ON al.person_id = p.person_id " +
                    "WHERE al.person_id = ? " +
                    "ORDER BY al.event_time DESC";
        
        return executeQuery(sql, userId);
    }
    
    /**
     * Get attendance logs for a user on a specific date
     */
    public List<AttendanceLog> findByUserIdAndDate(int userId, LocalDate date) throws SQLException {
        String sql = "SELECT al.log_id, al.person_id, p.full_name, al.event_time, al.event_type, " +
                    "al.activity, al.camera_id, al.confidence " +
                    "FROM attendance_logs al " +
                    "JOIN persons p ON al.person_id = p.person_id " +
                    "WHERE al.person_id = ? AND DATE(al.event_time) = ? " +
                    "ORDER BY al.event_time DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Get the last attendance record for today for a specific user and activity
     */
    public Optional<AttendanceLog> findLastTodayByUserAndActivity(int userId, String activity) throws SQLException {
        String sql = "SELECT al.log_id, al.person_id, p.full_name, al.event_time, al.event_type, " +
                    "al.activity, al.camera_id, al.confidence " +
                    "FROM attendance_logs al " +
                    "JOIN persons p ON al.person_id = p.person_id " +
                    "WHERE al.person_id = ? AND al.activity = ? AND DATE(al.event_time) = CURDATE() " +
                    "ORDER BY al.event_time DESC LIMIT 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, activity);
            
            List<AttendanceLog> results = mapResultSet(ps.executeQuery());
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }
    }
    
    /**
     * Get all attendance logs for today
     */
    public List<AttendanceLog> findAllToday() throws SQLException {
        String sql = "SELECT al.log_id, al.person_id, p.full_name, al.event_time, al.event_type, " +
                    "al.activity, al.camera_id, al.confidence " +
                    "FROM attendance_logs al " +
                    "JOIN persons p ON al.person_id = p.person_id " +
                    "WHERE DATE(al.event_time) = CURDATE() " +
                    "ORDER BY al.event_time DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return mapResultSet(rs);
        }
    }
    
    /**
     * Get all attendance logs within a date range
     */
    public List<AttendanceLog> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT al.log_id, al.person_id, p.full_name, al.event_time, al.event_type, " +
                    "al.activity, al.camera_id, al.confidence " +
                    "FROM attendance_logs al " +
                    "JOIN persons p ON al.person_id = p.person_id " +
                    "WHERE DATE(al.event_time) BETWEEN ? AND ? " +
                    "ORDER BY al.event_time DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Get attendance summary for a user (total days, time in count, time out count)
     */
    public AttendanceSummary getSummaryForUser(int userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT " +
                    "COUNT(DISTINCT DATE(event_time)) as total_days, " +
                    "SUM(CASE WHEN event_type = 'Time In' THEN 1 ELSE 0 END) as time_in_count, " +
                    "SUM(CASE WHEN event_type = 'Time Out' THEN 1 ELSE 0 END) as time_out_count " +
                    "FROM attendance_logs " +
                    "WHERE person_id = ? AND DATE(event_time) BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AttendanceSummary(
                        rs.getInt("total_days"),
                        rs.getInt("time_in_count"),
                        rs.getInt("time_out_count")
                    );
                }
            }
        }
        
        return new AttendanceSummary(0, 0, 0);
    }
    
    /**
     * Delete all attendance logs for a user
     */
    public boolean deleteByUserId(int userId) throws SQLException {
        String sql = "DELETE FROM attendance_logs WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Helper method to execute queries with a single int parameter
     */
    private List<AttendanceLog> executeQuery(String sql, int param) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, param);
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Map ResultSet to list of AttendanceLog objects
     */
    private List<AttendanceLog> mapResultSet(ResultSet rs) throws SQLException {
        List<AttendanceLog> logs = new ArrayList<>();
        
        while (rs.next()) {
            int logId = rs.getInt("log_id");
            int userId = rs.getInt("person_id");
            String userName = rs.getString("full_name");
            LocalDateTime eventTime = rs.getTimestamp("event_time").toLocalDateTime();
            String eventType = rs.getString("event_type");
            String activity = rs.getString("activity");
            String cameraId = rs.getString("camera_id");
            double confidence = rs.getDouble("confidence");
            
            logs.add(new AttendanceLog(logId, userId, userName, eventTime, 
                                      eventType, activity, cameraId, confidence));
        }
        
        return logs;
    }
    
    /**
     * Inner class for attendance summary data
     */
    public static class AttendanceSummary {
        private final int totalDays;
        private final int timeInCount;
        private final int timeOutCount;
        
        public AttendanceSummary(int totalDays, int timeInCount, int timeOutCount) {
            this.totalDays = totalDays;
            this.timeInCount = timeInCount;
            this.timeOutCount = timeOutCount;
        }
        
        public int getTotalDays() { return totalDays; }
        public int getTimeInCount() { return timeInCount; }
        public int getTimeOutCount() { return timeOutCount; }
    }
    
    /**
     * Get all attendance logs
     * 
     * @return List of all attendance logs
     * @throws SQLException if database error occurs
     */
    public List<AttendanceLog> getAllAttendanceLogs() throws SQLException {
        String sql = "SELECT a.log_id, a.person_id, a.event_time, a.event_type, " +
                    "a.camera_id, a.confidence, a.activity, u.full_name " +
                    "FROM attendance_logs a " +
                    "LEFT JOIN persons u ON a.person_id = u.person_id " +
                    "ORDER BY a.event_time DESC";
        
        return mapResultSet(DatabaseConfig.getConnection()
            .createStatement().executeQuery(sql));
    }
    
    /**
     * Get attendance logs by user ID
     * 
     * @param userId User ID to filter by
     * @return List of attendance logs for the user
     * @throws SQLException if database error occurs
     */
    public List<AttendanceLog> getAttendanceByUserId(int userId) throws SQLException {
        return executeQuery(
            "SELECT a.log_id, a.person_id, a.event_time, a.event_type, " +
            "a.camera_id, a.confidence, a.activity, u.full_name " +
            "FROM attendance_logs a " +
            "LEFT JOIN persons u ON a.person_id = u.person_id " +
            "WHERE a.person_id = ? " +
            "ORDER BY a.event_time DESC", userId);
    }
    
    /**
     * Get attendance logs by date range
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of attendance logs in the date range
     * @throws SQLException if database error occurs
     */
    public List<AttendanceLog> getAttendanceByDateRange(LocalDateTime startDate, LocalDateTime endDate) 
            throws SQLException {
        String sql = "SELECT a.log_id, a.person_id, a.event_time, a.event_type, " +
                    "a.camera_id, a.confidence, a.activity, u.full_name " +
                    "FROM attendance_logs a " +
                    "LEFT JOIN persons u ON a.person_id = u.person_id " +
                    "WHERE a.event_time BETWEEN ? AND ? " +
                    "ORDER BY a.event_time DESC";
        
        List<AttendanceLog> logs = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setTimestamp(1, Timestamp.valueOf(startDate));
            ps.setTimestamp(2, Timestamp.valueOf(endDate));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAttendanceLog(rs));
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Get attendance logs by activity
     * 
     * @param activity Activity name to filter by
     * @return List of attendance logs for the activity
     * @throws SQLException if database error occurs
     */
    public List<AttendanceLog> getAttendanceByActivity(String activity) throws SQLException {
        String sql = "SELECT a.log_id, a.person_id, a.event_time, a.event_type, " +
                    "a.camera_id, a.confidence, a.activity, u.full_name " +
                    "FROM attendance_logs a " +
                    "LEFT JOIN persons u ON a.person_id = u.person_id " +
                    "WHERE a.activity = ? " +
                    "ORDER BY a.event_time DESC";
        
        List<AttendanceLog> logs = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, activity);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAttendanceLog(rs));
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Helper method to map ResultSet row to AttendanceLog
     */
    private AttendanceLog mapResultSetToAttendanceLog(ResultSet rs) throws SQLException {
        int logId = rs.getInt("log_id");
        int userId = rs.getInt("person_id");
        String userName = rs.getString("full_name");
        LocalDateTime eventTime = rs.getTimestamp("event_time").toLocalDateTime();
        String eventType = rs.getString("event_type");
        String activity = rs.getString("activity");
        String cameraId = rs.getString("camera_id");
        double confidence = rs.getDouble("confidence");
        
        return new AttendanceLog(logId, userId, userName, eventTime, 
                                eventType, activity, cameraId, confidence);
    }
}
