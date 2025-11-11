-- =====================================================
-- IceFX Database Setup Script
-- Version: 2.0 (Refactored)
-- Date: November 2025
-- =====================================================

-- Drop database if exists (CAUTION: This deletes all data!)
-- DROP DATABASE IF EXISTS facial_attendance;

-- Create database
CREATE DATABASE IF NOT EXISTS facial_attendance 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE facial_attendance;

-- =====================================================
-- Table 1: Users/Persons
-- =====================================================
CREATE TABLE IF NOT EXISTS persons (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    person_code VARCHAR(50) UNIQUE NOT NULL COMMENT 'Unique identifier (student ID, employee ID)',
    full_name VARCHAR(100) NOT NULL COMMENT 'Full name of the person',
    department VARCHAR(100) COMMENT 'Department or class',
    position VARCHAR(50) COMMENT 'Position or year level',
    role ENUM('ADMIN', 'STAFF', 'STUDENT') DEFAULT 'STUDENT' COMMENT 'User role for access control',
    password VARCHAR(255) COMMENT 'BCrypt hashed password for login',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Account creation date',
    active BOOLEAN DEFAULT TRUE COMMENT 'Account status (active/deactivated)',
    
    INDEX idx_person_code (person_code),
    INDEX idx_role (role),
    INDEX idx_active (active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User accounts with role-based access control';

-- =====================================================
-- Table 2: Face Templates
-- =====================================================
CREATE TABLE IF NOT EXISTS face_templates (
    template_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL COMMENT 'Reference to person',
    template_data MEDIUMBLOB NOT NULL COMMENT 'Face image data (PNG/JPEG)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Template creation date',
    is_primary BOOLEAN DEFAULT FALSE COMMENT 'Primary template for recognition',
    
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id),
    INDEX idx_is_primary (is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Facial recognition templates for each user';

-- =====================================================
-- Table 3: Attendance Logs
-- =====================================================
CREATE TABLE IF NOT EXISTS attendance_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL COMMENT 'Reference to person',
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Time of attendance event',
    event_type ENUM('Time In', 'Time Out') NOT NULL COMMENT 'Type of event',
    camera_id VARCHAR(20) COMMENT 'Camera identifier',
    confidence DOUBLE COMMENT 'Recognition confidence score (0-100)',
    activity VARCHAR(100) COMMENT 'Activity/Subject during attendance',
    snapshot MEDIUMBLOB COMMENT 'Face snapshot at time of recognition',
    
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id),
    INDEX idx_event_time (event_time),
    INDEX idx_event_type (event_type),
    INDEX idx_activity (activity),
    INDEX idx_person_event_time (person_id, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Attendance records with timestamps';

-- =====================================================
-- Table 4: Schedules
-- =====================================================
CREATE TABLE IF NOT EXISTS schedules (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL COMMENT 'Reference to person',
    day ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') 
        NOT NULL COMMENT 'Day of the week',
    start_time TIME NOT NULL COMMENT 'Schedule start time',
    end_time TIME NOT NULL COMMENT 'Schedule end time',
    activity VARCHAR(100) NOT NULL COMMENT 'Activity name (e.g., subject, class)',
    
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id),
    INDEX idx_day (day),
    INDEX idx_person_day (person_id, day),
    
    CONSTRAINT chk_time_range CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User schedules for attendance validation';

-- =====================================================
-- Insert Default Admin User
-- =====================================================
-- Password: 'admin123' (Please change after first login!)
-- BCrypt hash generated with cost factor 10
INSERT INTO persons (person_code, full_name, department, position, role, password, active) 
VALUES (
    'ADMIN001', 
    'System Administrator', 
    'IT Department', 
    'Administrator', 
    'ADMIN',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhLW',
    TRUE
) ON DUPLICATE KEY UPDATE person_id = person_id;

-- =====================================================
-- Sample Data (Optional - for testing)
-- =====================================================

-- Sample Students
INSERT INTO persons (person_code, full_name, department, position, role, password, active) VALUES
('STU2025001', 'Alice Johnson', 'Computer Science', 'Year 3', 'STUDENT', NULL, TRUE),
('STU2025002', 'Bob Smith', 'Computer Science', 'Year 2', 'STUDENT', NULL, TRUE),
('STU2025003', 'Charlie Brown', 'Information Technology', 'Year 4', 'STUDENT', NULL, TRUE)
ON DUPLICATE KEY UPDATE person_id = person_id;

-- Sample Staff
INSERT INTO persons (person_code, full_name, department, position, role, password, active) VALUES
('STAFF001', 'Dr. Emily Davis', 'Computer Science', 'Professor', 'STAFF', NULL, TRUE),
('STAFF002', 'Prof. Michael Chen', 'Information Technology', 'Associate Professor', 'STAFF', NULL, TRUE)
ON DUPLICATE KEY UPDATE person_id = person_id;

-- Sample Schedules for Alice Johnson (assuming person_id = 2)
INSERT INTO schedules (person_id, day, start_time, end_time, activity) VALUES
(2, 'MONDAY', '08:00:00', '10:00:00', 'Data Structures'),
(2, 'MONDAY', '13:00:00', '15:00:00', 'Algorithms'),
(2, 'WEDNESDAY', '08:00:00', '10:00:00', 'Database Systems'),
(2, 'FRIDAY', '10:00:00', '12:00:00', 'Software Engineering')
ON DUPLICATE KEY UPDATE schedule_id = schedule_id;

-- =====================================================
-- Views for Reporting
-- =====================================================

-- View: Daily Attendance Summary
CREATE OR REPLACE VIEW view_daily_attendance AS
SELECT 
    DATE(al.event_time) as attendance_date,
    p.person_code,
    p.full_name,
    al.activity,
    MAX(CASE WHEN al.event_type = 'Time In' THEN al.event_time END) as time_in,
    MAX(CASE WHEN al.event_type = 'Time Out' THEN al.event_time END) as time_out,
    CASE 
        WHEN MAX(CASE WHEN al.event_type = 'Time Out' THEN al.event_time END) IS NOT NULL
        THEN TIMESTAMPDIFF(MINUTE, 
            MAX(CASE WHEN al.event_type = 'Time In' THEN al.event_time END),
            MAX(CASE WHEN al.event_type = 'Time Out' THEN al.event_time END)
        )
        ELSE NULL
    END as duration_minutes
FROM attendance_logs al
JOIN persons p ON al.person_id = p.person_id
GROUP BY DATE(al.event_time), p.person_id, al.activity
ORDER BY attendance_date DESC, p.full_name;

-- View: User Statistics
CREATE OR REPLACE VIEW view_user_statistics AS
SELECT 
    p.person_id,
    p.person_code,
    p.full_name,
    p.department,
    p.position,
    p.role,
    COUNT(DISTINCT ft.template_id) as template_count,
    COUNT(DISTINCT DATE(al.event_time)) as attendance_days,
    COUNT(al.log_id) as total_logs,
    MAX(al.event_time) as last_attendance
FROM persons p
LEFT JOIN face_templates ft ON p.person_id = ft.person_id
LEFT JOIN attendance_logs al ON p.person_id = al.person_id
WHERE p.active = TRUE
GROUP BY p.person_id
ORDER BY p.full_name;

-- View: Schedule Overview
CREATE OR REPLACE VIEW view_schedule_overview AS
SELECT 
    p.person_code,
    p.full_name,
    s.day,
    s.start_time,
    s.end_time,
    s.activity,
    CONCAT(
        DATE_FORMAT(s.start_time, '%h:%i %p'),
        ' - ',
        DATE_FORMAT(s.end_time, '%h:%i %p')
    ) as time_range
FROM schedules s
JOIN persons p ON s.person_id = p.person_id
WHERE p.active = TRUE
ORDER BY p.full_name, 
    FIELD(s.day, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'),
    s.start_time;

-- =====================================================
-- Stored Procedures
-- =====================================================

DELIMITER //

-- Procedure: Get Attendance Summary for Date Range
CREATE PROCEDURE sp_get_attendance_summary(
    IN p_person_id INT,
    IN p_start_date DATE,
    IN p_end_date DATE
)
BEGIN
    SELECT 
        DATE(event_time) as date,
        activity,
        GROUP_CONCAT(
            CONCAT(event_type, ': ', TIME_FORMAT(event_time, '%h:%i %p'))
            ORDER BY event_time
            SEPARATOR ' | '
        ) as events
    FROM attendance_logs
    WHERE person_id = p_person_id
        AND DATE(event_time) BETWEEN p_start_date AND p_end_date
    GROUP BY DATE(event_time), activity
    ORDER BY date DESC;
END//

-- Procedure: Check Schedule Conflict
CREATE PROCEDURE sp_check_schedule_conflict(
    IN p_person_id INT,
    IN p_day ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'),
    IN p_start_time TIME,
    IN p_end_time TIME,
    IN p_exclude_schedule_id INT,
    OUT p_has_conflict BOOLEAN
)
BEGIN
    DECLARE conflict_count INT;
    
    SELECT COUNT(*) INTO conflict_count
    FROM schedules
    WHERE person_id = p_person_id
        AND day = p_day
        AND schedule_id != IFNULL(p_exclude_schedule_id, 0)
        AND (
            (start_time <= p_start_time AND end_time >= p_start_time) OR
            (start_time <= p_end_time AND end_time >= p_end_time) OR
            (start_time >= p_start_time AND end_time <= p_end_time)
        );
    
    SET p_has_conflict = (conflict_count > 0);
END//

DELIMITER ;

-- =====================================================
-- Triggers
-- =====================================================

DELIMITER //

-- Trigger: Auto-set is_primary for first face template
CREATE TRIGGER trg_first_template_primary
BEFORE INSERT ON face_templates
FOR EACH ROW
BEGIN
    DECLARE template_count INT;
    
    SELECT COUNT(*) INTO template_count
    FROM face_templates
    WHERE person_id = NEW.person_id;
    
    IF template_count = 0 THEN
        SET NEW.is_primary = TRUE;
    END IF;
END//

-- Trigger: Prevent overlapping attendance in same minute
CREATE TRIGGER trg_prevent_duplicate_attendance
BEFORE INSERT ON attendance_logs
FOR EACH ROW
BEGIN
    DECLARE recent_count INT;
    
    SELECT COUNT(*) INTO recent_count
    FROM attendance_logs
    WHERE person_id = NEW.person_id
        AND event_type = NEW.event_type
        AND activity = NEW.activity
        AND TIMESTAMPDIFF(MINUTE, event_time, NEW.event_time) < 1;
    
    IF recent_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Duplicate attendance within 1 minute not allowed';
    END IF;
END//

DELIMITER ;

-- =====================================================
-- Performance Optimization
-- =====================================================

-- Analyze tables for query optimization
ANALYZE TABLE persons, face_templates, attendance_logs, schedules;

-- =====================================================
-- Database Information
-- =====================================================
SELECT 
    'Database Setup Complete!' as status,
    DATABASE() as database_name,
    @@character_set_database as charset,
    @@collation_database as collation,
    NOW() as setup_time;

-- Show created tables
SHOW TABLES;

-- Show table statistics
SELECT 
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) as size_mb
FROM information_schema.TABLES
WHERE table_schema = 'facial_attendance'
ORDER BY table_name;

-- =====================================================
-- Security Recommendations
-- =====================================================
/*
1. Create a dedicated database user (don't use root in production):
   CREATE USER 'icefx_user'@'localhost' IDENTIFIED BY 'strong_password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON facial_attendance.* TO 'icefx_user'@'localhost';
   FLUSH PRIVILEGES;

2. Change the default admin password immediately after first login!

3. Regularly backup the database:
   mysqldump -u root -p facial_attendance > backup_$(date +%Y%m%d).sql

4. Enable MySQL binary logging for point-in-time recovery:
   SET GLOBAL binlog_format = 'ROW';

5. Monitor database performance:
   SHOW PROCESSLIST;
   SHOW ENGINE INNODB STATUS;
*/

-- =====================================================
-- End of Setup Script
-- =====================================================
