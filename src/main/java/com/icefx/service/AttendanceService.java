package com.icefx.service;

import com.icefx.config.AppConfig;
import com.icefx.dao.AttendanceDAO;
import com.icefx.dao.UserDAO;
import com.icefx.model.AttendanceLog;
import com.icefx.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Attendance service for business logic and validation.
 */
public class AttendanceService {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);
    
    private final long duplicatePreventionMinutes;
    
    private final AttendanceDAO attendanceDAO;
    private final UserDAO userDAO;
    
    public static class AttendanceResult {
        public enum Status {
            SUCCESS, DUPLICATE, USER_NOT_FOUND, ERROR
        }
        
        private final Status status;
        private final String message;
        private final AttendanceLog attendanceLog;
        
        private AttendanceResult(Status status, String message, AttendanceLog log) {
            this.status = status;
            this.message = message;
            this.attendanceLog = log;
        }
        
        public static AttendanceResult success(String msg, AttendanceLog log) {
            return new AttendanceResult(Status.SUCCESS, msg, log);
        }
        
        public static AttendanceResult duplicate(String userName) {
            return new AttendanceResult(Status.DUPLICATE, 
                "Attendance already logged for " + userName + " within the last hour", null);
        }
        
        public static AttendanceResult userNotFound(int userId) {
            return new AttendanceResult(Status.USER_NOT_FOUND, 
                "User ID " + userId + " not found", null);
        }
        
        public static AttendanceResult error(String errorMsg) {
            return new AttendanceResult(Status.ERROR, "Error: " + errorMsg, null);
        }
        
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public AttendanceLog getAttendanceLog() { return attendanceLog; }
        public boolean isSuccess() { return status == Status.SUCCESS; }
        
        @Override
        public String toString() {
            return message;
        }
    }
    
    public AttendanceService(AttendanceDAO attendanceDAO, UserDAO userDAO) {
        this.attendanceDAO = attendanceDAO;
        this.userDAO = userDAO;
        this.duplicatePreventionMinutes = AppConfig.getInt("attendance.duplicate.prevention.minutes", 60);
        logger.info("AttendanceService initialized (duplicate prevention: {} minutes)", duplicatePreventionMinutes);
    }
    
    public AttendanceResult logAttendance(int userId, double confidence) {
        try {
            Optional<User> userOpt = userDAO.findById(userId);
            if (!userOpt.isPresent()) {
                logger.warn("Attempted to log attendance for non-existent user: {}", userId);
                return AttendanceResult.userNotFound(userId);
            }
            
            User user = userOpt.get();
            logger.info("Logging attendance for user: {} (ID: {})", user.getFullName(), userId);
            
            if (isDuplicateAttendance(userId)) {
                logger.info("Duplicate attendance detected for user: {}", user.getFullName());
                return AttendanceResult.duplicate(user.getFullName());
            }
            
            LocalDateTime now = LocalDateTime.now();
            AttendanceLog log = new AttendanceLog(
                0, userId, user.getFullName(), now,
                "Time In", "Facial Recognition", "CAM1", confidence
            );
            
            int logId = attendanceDAO.logAttendance(log);
            
            logger.info("✅ Attendance logged successfully for {} at {} (Log ID: {})", 
                user.getFullName(), now, logId);
            
            return AttendanceResult.success("Attendance recorded successfully", log);
            
        } catch (Exception e) {
            logger.error("Failed to log attendance for user {}", userId, e);
            return AttendanceResult.error(e.getMessage());
        }
    }
    
    private boolean isDuplicateAttendance(int userId) {
        try {
            List<AttendanceLog> todayLogs = attendanceDAO.findByUserIdAndDate(userId, LocalDate.now());
            
            if (todayLogs.isEmpty()) {
                return false;
            }
            
            AttendanceLog mostRecent = todayLogs.get(0);
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(duplicatePreventionMinutes);
            
            boolean isDuplicate = mostRecent.getEventTime().isAfter(cutoff);
            
            if (isDuplicate) {
                long minutesAgo = ChronoUnit.MINUTES.between(mostRecent.getEventTime(), LocalDateTime.now());
                logger.debug("Last attendance was {} minutes ago (threshold: {})", 
                    minutesAgo, duplicatePreventionMinutes);
            }
            
            return isDuplicate;
            
        } catch (Exception e) {
            logger.error("Error checking duplicate attendance", e);
            return false;
        }
    }
    
    public List<AttendanceLog> getAttendanceByUser(int userId) throws SQLException {
        return attendanceDAO.findByUserId(userId);
    }
    
    public List<AttendanceLog> getAttendanceByUserAndDate(int userId, LocalDate date) throws SQLException {
        return attendanceDAO.findByUserIdAndDate(userId, date);
    }
    
    public List<AttendanceLog> getTodayAttendance() throws SQLException {
        return attendanceDAO.findAllToday();
    }
    
    public List<AttendanceLog> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        return attendanceDAO.findByDateRange(startDate, endDate);
    }
    
    public AttendanceDAO.AttendanceSummary getAttendanceSummary(int userId, LocalDate startDate, LocalDate endDate) 
            throws SQLException {
        return attendanceDAO.getSummaryForUser(userId, startDate, endDate);
    }
    
    public boolean deleteUserAttendance(int userId) throws SQLException {
        return attendanceDAO.deleteByUserId(userId);
    }
}
