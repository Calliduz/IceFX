package com.icefx.service;

import com.icefx.dao.AttendanceDAO;
import com.icefx.model.AttendanceLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting attendance data to CSV format
 * 
 * @author IceFX Team
 * @version 2.0
 * @since JDK 23.0.1
 */
public class ExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final AttendanceDAO attendanceDAO;
    private final String exportDirectory;
    
    /**
     * Creates ExportService with default export directory
     * 
     * @param attendanceDAO DAO for fetching attendance data
     */
    public ExportService(AttendanceDAO attendanceDAO) {
        this(attendanceDAO, "exports");
    }
    
    /**
     * Creates ExportService with custom export directory
     * 
     * @param attendanceDAO DAO for fetching attendance data
     * @param exportDirectory directory path for exports
     */
    public ExportService(AttendanceDAO attendanceDAO, String exportDirectory) {
        this.attendanceDAO = attendanceDAO;
        this.exportDirectory = exportDirectory;
        ensureExportDirectoryExists();
    }
    
    /**
     * Ensures the export directory exists
     */
    private void ensureExportDirectoryExists() {
        try {
            Path path = Paths.get(exportDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created export directory: {}", exportDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create export directory: {}", exportDirectory, e);
        }
    }
    
    /**
     * Exports all attendance logs to CSV
     * 
     * @return File object of the created CSV file
     * @throws IOException if file write fails
     * @throws SQLException if database query fails
     */
    public File exportAllAttendance() throws IOException, SQLException {
        logger.info("Exporting all attendance records...");
        List<AttendanceLog> logs = attendanceDAO.getAllAttendanceLogs();
        String filename = String.format("attendance_all_%s.csv", 
            LocalDate.now().format(DATE_FORMATTER));
        return exportToCSV(logs, filename);
    }
    
    /**
     * Exports attendance logs for a specific user
     * 
     * @param userId User ID to filter by
     * @return File object of the created CSV file
     * @throws IOException if file write fails
     * @throws SQLException if database query fails
     */
    public File exportUserAttendance(int userId) throws IOException, SQLException {
        logger.info("Exporting attendance for user ID: {}", userId);
        List<AttendanceLog> logs = attendanceDAO.getAttendanceByUserId(userId);
        String filename = String.format("attendance_user_%d_%s.csv", 
            userId, LocalDate.now().format(DATE_FORMATTER));
        return exportToCSV(logs, filename);
    }
    
    /**
     * Exports attendance logs for a date range
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return File object of the created CSV file
     * @throws IOException if file write fails
     * @throws SQLException if database query fails
     */
    public File exportDateRangeAttendance(LocalDate startDate, LocalDate endDate) 
            throws IOException, SQLException {
        logger.info("Exporting attendance from {} to {}", startDate, endDate);
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        List<AttendanceLog> logs = attendanceDAO.getAttendanceByDateRange(start, end);
        String filename = String.format("attendance_%s_to_%s.csv", 
            startDate.format(DATE_FORMATTER), 
            endDate.format(DATE_FORMATTER));
        return exportToCSV(logs, filename);
    }
    
    /**
     * Exports today's attendance logs
     * 
     * @return File object of the created CSV file
     * @throws IOException if file write fails
     * @throws SQLException if database query fails
     */
    public File exportTodayAttendance() throws IOException, SQLException {
        logger.info("Exporting today's attendance...");
        LocalDate today = LocalDate.now();
        return exportDateRangeAttendance(today, today);
    }
    
    /**
     * Exports attendance logs filtered by activity
     * 
     * @param activity Activity name to filter by
     * @return File object of the created CSV file
     * @throws IOException if file write fails
     * @throws SQLException if database query fails
     */
    public File exportByActivity(String activity) throws IOException, SQLException {
        logger.info("Exporting attendance for activity: {}", activity);
        List<AttendanceLog> logs = attendanceDAO.getAttendanceByActivity(activity);
        String filename = String.format("attendance_%s_%s.csv", 
            activity.replaceAll("\\s+", "_"), 
            LocalDate.now().format(DATE_FORMATTER));
        return exportToCSV(logs, filename);
    }
    
    /**
     * Core method to write attendance logs to CSV file
     * 
     * @param logs List of attendance logs to export
     * @param filename Name of the CSV file
     * @return File object of the created CSV file
     * @throws IOException if file write fails
     */
    private File exportToCSV(List<AttendanceLog> logs, String filename) throws IOException {
        File csvFile = new File(exportDirectory, filename);
        
        try (FileWriter writer = new FileWriter(csvFile);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("Log ID", "User ID", "User Name", "Event Time", 
                            "Event Type", "Camera ID", "Confidence", "Activity"))) {
            
            for (AttendanceLog log : logs) {
                csvPrinter.printRecord(
                    log.getLogId(),
                    log.getUserId(),
                    log.getUserName() != null ? log.getUserName() : "Unknown",
                    log.getEventTime().format(DATETIME_FORMATTER),
                    log.getEventType(),
                    log.getCameraId() != null ? log.getCameraId() : "N/A",
                    String.format("%.2f", log.getConfidence()),
                    log.getActivity() != null ? log.getActivity() : "N/A"
                );
            }
            
            csvPrinter.flush();
            logger.info("Successfully exported {} records to: {}", logs.size(), csvFile.getAbsolutePath());
        }
        
        return csvFile;
    }
    
    /**
     * Gets the export directory path
     * 
     * @return export directory path
     */
    public String getExportDirectory() {
        return exportDirectory;
    }
    
    /**
     * Lists all CSV files in the export directory
     * 
     * @return Array of CSV files
     */
    public File[] listExports() {
        File dir = new File(exportDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            return new File[0];
        }
        
        File[] csvFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
        return csvFiles != null ? csvFiles : new File[0];
    }
    
    /**
     * Deletes an export file
     * 
     * @param filename Name of the file to delete
     * @return true if deletion was successful
     */
    public boolean deleteExport(String filename) {
        File file = new File(exportDirectory, filename);
        if (file.exists() && file.isFile()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("Deleted export file: {}", filename);
            } else {
                logger.warn("Failed to delete export file: {}", filename);
            }
            return deleted;
        }
        return false;
    }
}
