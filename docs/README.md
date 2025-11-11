# IceFX Documentation

![Java](https://img.shields.io/badge/Java-23.0.1-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-23.0.1-blue) ![OpenCV](https://img.shields.io/badge/OpenCV-4.9.0-green) ![MySQL](https://img.shields.io/badge/MySQL-9.1.0-blue)

## 📚 Documentation Index

This directory contains all documentation for the IceFX Facial Recognition Attendance System.

### 🚀 Getting Started

- **[QUICK_START.md](QUICK_START.md)** - Quick setup guide for new users
- **[MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)** - Upgrading from older versions

### 📖 Technical Documentation

- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Complete project organization and file structure
- **[REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)** - Technical details of the modernization
- **[CLEANUP_SUMMARY.md](CLEANUP_SUMMARY.md)** - File structure cleanup report
- **[FILE_STRUCTURE_CLEANUP_COMPLETE.md](FILE_STRUCTURE_CLEANUP_COMPLETE.md)** - Comprehensive cleanup summary

### 📦 Archive

- **[archive/](archive/)** - Historical planning documents and analysis
  - 00_INDEX.md - Documentation index
  - 01_CRASH_ANALYSIS.md - Initial crash analysis
  - 02_SOURCE_CODE_AUDIT.md - Code audit report
  - 03_IMPLEMENTATION_PLAN.md - Implementation roadmap
  - 04_QA_TEST_CHECKLIST.md - Quality assurance checklist
  - 05_PROJECT_STRUCTURE.md - Original structure planning

---

## 🎯 Overview

A modern, enterprise-grade facial recognition attendance management system built with JavaFX 23 and OpenCV 4.9.

## 🎯 Features

### Core Functionality

- **Real-time Facial Recognition** - Advanced face detection and recognition using OpenCV LBPH algorithm
- **Attendance Management** - Automated Time In/Time Out tracking with schedule validation
- **User Management** - Complete CRUD operations for users (Admin, Staff, Students)
- **Schedule System** - Flexible scheduling with conflict detection
- **Role-Based Access Control** - Admin and Staff roles with different permissions
- **Export Reports** - Generate CSV reports for attendance logs

### Security & Performance

- **BCrypt Password Hashing** - Secure password storage
- **HikariCP Connection Pooling** - Optimized database performance
- **Liveness Detection** - Basic anti-spoofing protection
- **Session Management** - Secure user authentication

### UI/UX

- **Modern Responsive Design** - Clean, professional interface
- **Dark/Light Theme Support** - Toggle between themes
- **Real-time Camera Preview** - Live video feed with face detection overlay
- **Status Indicators** - Visual feedback for all operations
- **Smooth Animations** - Professional transitions and effects

## 📁 Project Structure

```
IceFX/
├── src/main/
│   ├── java/com/icefx/
│   │   ├── config/          # Configuration management
│   │   ├── controller/      # JavaFX Controllers
│   │   ├── dao/             # Data Access Objects
│   │   ├── model/           # Entity Classes
│   │   ├── service/         # Business Logic
│   │   ├── util/            # Utility Classes
│   │   └── Main.java
│   └── resources/
│       ├── fxml/            # FXML Layouts
│       ├── css/             # Stylesheets
│       ├── images/          # Icons & Images
│       └── haar/            # Haar Cascade Models
├── resources/trained_faces/ # Face templates
├── exports/                 # CSV reports
├── pom.xml
└── README.md
```

## 🏗️ Architecture

**Layered Architecture:**

1. **Presentation Layer** (Controller) - UI event handling
2. **Business Logic Layer** (Service) - Core business rules
3. **Data Access Layer** (DAO) - Database operations
4. **Model Layer** - Entity classes with JavaFX property binding

## 🚀 Setup & Installation

### Prerequisites

- **Java JDK 17+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** - [Download](https://dev.mysql.com/downloads/mysql/)
- **Webcam** - For facial recognition

### Database Setup

1. Create database:

```sql
CREATE DATABASE facial_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE facial_attendance;
```

2. Create tables:

```sql
-- Users table
CREATE TABLE persons (
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
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Face templates
CREATE TABLE face_templates (
    template_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    template_data MEDIUMBLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Attendance logs
CREATE TABLE attendance_logs (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Schedules
CREATE TABLE schedules (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    day ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    activity VARCHAR(100) NOT NULL,
    FOREIGN KEY (person_id) REFERENCES persons(person_id) ON DELETE CASCADE,
    INDEX idx_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Configuration

IceFX stores runtime settings in `~/.icefx/config.properties`. The file is created automatically on first launch.

Update the database section:

```properties
db.type=mysql
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=facial_attendance
db.mysql.username=root
db.mysql.password=YOUR_PASSWORD
```

### Build & Run

```bash
# Clone repository
git clone https://github.com/Calliduz/IceFX.git
cd IceFX

# Build project
mvn clean install

# Run application
mvn javafx:run
```

## 📖 Usage

### Default Login

- **Username**: `ADMIN001`
- **Password**: `admin` (Change immediately!)

### Admin Features

- Add/Edit/Delete users
- Manage roles and permissions
- Register face templates
- View all attendance logs
- Export reports to CSV

### Attendance Workflow

1. Start Camera
2. Select User
3. Position Face
4. Click "Recognize"
5. System logs Time In/Out automatically

## 🔧 Configuration

### Camera Settings

```properties
camera.index=0          # Default webcam
camera.width=640
camera.height=480
```

### Recognition Tuning

```properties
recognition.confidence.threshold=70.0    # Lower = stricter
recognition.liveness.minDiff=1200.0      # Anti-spoofing sensitivity
```

## 🐛 Troubleshooting

**Camera Not Working:**

- Check webcam permissions
- Verify camera index in properties
- Test with other applications

**Database Connection Failed:**

- Ensure MySQL is running
- Check credentials in `~/.icefx/config.properties`
- Verify database exists

**Face Recognition Inaccurate:**

- Improve lighting conditions
- Register multiple face angles
- Adjust confidence threshold

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/NewFeature`)
3. Commit changes (`git commit -m 'Add NewFeature'`)
4. Push to branch (`git push origin feature/NewFeature`)
5. Open Pull Request

## 📄 License

MIT License - See [LICENSE](LICENSE) file

## 👥 Authors

- Original Team: Camata, Dalupang, Cabunoc
- Refactored: 2025

## 📧 Support

- 🐛 [GitHub Issues](https://github.com/Calliduz/IceFX/issues)
- 📚 [Wiki](https://github.com/Calliduz/IceFX/wiki)

---

**Built with ❤️ using Java, JavaFX, and OpenCV**
