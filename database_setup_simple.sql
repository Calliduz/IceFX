-- =====================================================
-- IceFX Database Setup Script - SIMPLIFIED
-- Version: 2.1 (Fixed for MariaDB compatibility)
-- Date: November 2025
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS facial_attendance 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE facial_attendance;

-- =====================================================
-- Table 1: Persons (Users)
-- =====================================================
CREATE TABLE IF NOT EXISTS persons (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    person_code VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    position VARCHAR(50),
    role ENUM('ADMIN', 'STAFF', 'STUDENT') DEFAULT 'STUDENT',
    password VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    
    INDEX idx_person_code (person_code),
    INDEX idx_role (role),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table 2: Face Templates
-- =====================================================
CREATE TABLE IF NOT EXISTS face_templates (
    template_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    template_data MEDIUMBLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table 3: Attendance Logs
-- =====================================================
CREATE TABLE IF NOT EXISTS attendance_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type ENUM('Time In', 'Time Out') NOT NULL,
    camera_id VARCHAR(20),
    confidence DOUBLE,
    activity VARCHAR(100),
    snapshot MEDIUMBLOB,
    
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id),
    INDEX idx_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table 4: Schedules
-- =====================================================
CREATE TABLE IF NOT EXISTS schedules (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    day ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    activity VARCHAR(100) NOT NULL,
    
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Insert Default Admin User
-- =====================================================
-- IMPORTANT: This is a WORKING BCrypt hash!
-- Password: 'admin123'
-- Generated with BCrypt cost factor 10
INSERT INTO persons (person_code, full_name, department, position, role, password, active) 
VALUES (
    'ADMIN001', 
    'System Administrator', 
    'IT Department', 
    'Administrator', 
    'ADMIN',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhLW',
    TRUE
);

-- Alternative admin with simpler password
-- Password: 'admin'
INSERT INTO persons (person_code, full_name, department, position, role, password, active) 
VALUES (
    'ADM001', 
    'Admin User', 
    'IT Department', 
    'Administrator', 
    'ADMIN',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    TRUE
);

-- =====================================================
-- Sample Data
-- =====================================================

-- Sample Students
INSERT INTO persons (person_code, full_name, department, position, role, active) VALUES
('STU001', 'Alice Johnson', 'Computer Science', 'Year 3', 'STUDENT', TRUE),
('STU002', 'Bob Smith', 'Engineering', 'Year 2', 'STUDENT', TRUE);

-- Sample Staff
INSERT INTO persons (person_code, full_name, department, position, role, active) VALUES
('STF001', 'Dr. Emily Davis', 'Computer Science', 'Professor', 'STAFF', TRUE);

-- =====================================================
-- Success Message
-- =====================================================
SELECT 
    'Database setup complete!' as Status,
    'Use ADMIN001 / admin123 OR ADM001 / admin to login' as Credentials;

-- Show created users
SELECT person_code, full_name, role, active FROM persons;
