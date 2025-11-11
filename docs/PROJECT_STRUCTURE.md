# IceFX Project Structure

## 📁 Clean, Modern File Organization

This document describes the final, cleaned project structure after modernization.

````
IceFX/
├── 📄 README.md                           # Main project documentation
├── 📄 QUICK_START.md                      # Quick start guide for users
├── 📄 pom.xml                             # Maven build configuration
├── 📄 database_setup.sql                  # Database schema (clean)
├── 📄 facial_attendance May 18...sql      # Database backup with sample data
├── 📄 .gitignore                          # Git ignore rules (updated)
│
├── 📂 docs/                               # Documentation
│   ├── README.md                          # Developer documentation
│   ├── QUICK_START.md                     # Detailed setup instructions
│   ├── MIGRATION_GUIDE.md                 # Upgrade guide from old version
│   ├── REFACTORING_SUMMARY.md             # Technical refactoring details
│   └── archive/                           # Historical planning documents
│       ├── 00_INDEX.md
│       ├── 01_CRASH_ANALYSIS.md
│       ├── 02_SOURCE_CODE_AUDIT.md
│       ├── 03_IMPLEMENTATION_PLAN.md
│       ├── 04_QA_TEST_CHECKLIST.md
│       └── 05_PROJECT_STRUCTURE.md
│
├── 📂 src/                                # Source code
│   ├── main/
│   │   ├── java/com/icefx/               # Application code (Java 23)
│   │   │   ├── IceFXApplication.java     # Main entry point
│   │   │   ├── config/                   # Configuration management
│   │   │   │   ├── AppConfig.java        # Centralized app configuration
│   │   │   │   └── DatabaseConfig.java   # HikariCP database configuration
│   │   │   ├── controller/               # JavaFX UI controllers
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   └── AdminController.java
│   │   │   ├── dao/                      # Data Access Objects
│   │   │   │   ├── UserDAO.java
│   │   │   │   ├── AttendanceDAO.java
│   │   │   │   ├── ScheduleDAO.java
│   │   │   │   └── FaceTemplateDAO.java
│   │   │   ├── model/                    # Entity classes
│   │   │   │   ├── User.java
│   │   │   │   ├── AttendanceLog.java
│   │   │   │   ├── Schedule.java
│   │   │   │   ├── FaceTemplate.java
│   │   │   │   └── CameraStatus.java
│   │   │   ├── service/                  # Business logic layer
│   │   │   │   ├── UserService.java
│   │   │   │   ├── AttendanceService.java
│   │   │   │   ├── FaceRecognitionService.java
│   │   │   │   └── CameraService.java
│   │   │   └── util/                     # Utility classes
│   │   │       ├── NativeLoader.java     # OpenCV native library loader
│   │   │       └── SessionManager.java   # User session management
│   │   │
│   │   └── resources/                    # Application resources
│   │       ├── com/icefx/                # Package-specific resources
│   │       │   ├── view/                 # FXML UI layouts
│   │       │   │   ├── Login.fxml
│   │       │   │   ├── Dashboard.fxml
│   │       │   │   └── AdminPanel.fxml
│   │       │   ├── styles/               # CSS stylesheets
│   │       │   │   ├── light-theme.css
│   │       │   │   └── dark-theme.css
│   │       │   └── images/               # Application images
│   │       │       └── logo.png
│   │       ├── haar/                     # OpenCV Haar cascade files
│   │       │   ├── haarcascade_frontalface_default.xml
│   │       │   ├── lbpcascade_frontalface.xml
│   │       │   └── ... (21 cascade files total)
│   │       └── logback.xml               # SLF4J logging configuration
│   │
│   └── test/java/com/icefx/              # Unit tests
│       ├── config/
│       │   └── AppConfigTest.java        # (11 tests)
│       └── service/
│           └── UserServiceTest.java      # (10 tests)
│
├── 📂 logs/                               # Application logs (runtime)
│   └── icefx.log                         # Current log file
│
├── 📂 target/                             # Maven build output (ignored by git)
│   ├── classes/                          # Compiled classes
│   ├── test-classes/                     # Compiled test classes
│   └── IceFX-1.0.0.jar                   # Shaded JAR with dependencies
│
└── 📂 .vscode/                            # VS Code configuration
    ├── settings.json                     # Editor settings
    └── (launch.json - to be created)     # Debug configurations

## 🗑️ Removed During Cleanup

### Obsolete Libraries (replaced by Maven)
- ❌ `libs/` - Old MySQL & OpenCV JARs (Maven handles dependencies now)
- ❌ `native/` - Windows .dll files (JavaCV platform natives used instead)

### Redundant Documentation (archived or removed)
- ❌ `BUILD_AND_RUN.md` - Merged into README.md
- ❌ `DASHBOARD_IMPLEMENTATION.md` - Developer session notes
- ❌ `FINAL_SUMMARY.md` - Session summary
- ❌ `IMPLEMENTATION_COMPLETE.md` - Session notes
- ❌ `PROGRESS_REPORT.md` - Session notes
- ❌ `SESSION_SUMMARY.md` - Session summary
- ❌ `TESTING_GUIDE.md` - Content moved to docs/

### Obsolete Configuration Files
- ❌ `nbactions.xml` - NetBeans specific (using Maven now)
- ❌ `build.fxbuild` - e(fx)clipse specific (obsolete)
- ❌ `.classpath`, `.project`, `.settings/` - Eclipse specific (Maven build)
- ❌ `_config.yml` - Jekyll config (not needed)
- ❌ `dependency-reduced-pom.xml` - Auto-generated by maven-shade-plugin

### Old Data & Resources
- ❌ `resources/` (root level) - Old trained_faces.xml
- ❌ `faces/` - Test face images
- ❌ `src/*.png`, `src/*.jpg` - Loose image files (22 files)
- ❌ `src/styles.css` - Loose stylesheet
- ❌ `src/main/resources/database.properties` - Replaced by `~/.icefx/config.properties`

### Crash Logs & Debug Files
- ❌ `hs_err_pid*.log` - JVM crash logs (8 files)
- ❌ `replay_pid*.log` - Crash replay logs
- ❌ `app_startup.log` - Old startup log

## 🎯 Configuration Management

### Runtime Configuration
**Location:** `~/.icefx/config.properties`
**Created:** Automatically on first run
**Purpose:** User-specific settings (database, camera, theme, etc.)

**Key Settings:**
- `app.theme` - light or dark
- `db.type` - mysql or sqlite
- `db.mysql.*` - MySQL connection settings
- `db.sqlite.path` - SQLite database path
- `camera.*` - Camera device and resolution
- `recognition.*` - Face recognition thresholds
- `attendance.*` - Attendance logging rules

### Runtime Data Directory
**Location:** `~/.icefx/` or `data/` (configurable)
**Created:** Automatically at runtime
**Contents:**
- `trained_faces/` - LBPH face recognition models
- `facial_attendance.db` - SQLite database (if using SQLite)
- `exports/` - CSV export files (future feature)

## 🏗️ Build & Run

### Compile
```bash
mvn clean compile
````

### Run Application

```bash
mvn javafx:run
```

### Run Tests

```bash
mvn test
```

### Package (with dependencies)

```bash
mvn clean package
```

Creates: `target/IceFX-1.0.0.jar` (~740MB with all native libraries)

## 📊 Project Statistics

- **Java Files:** 23 classes + 2 test classes = 25 files
- **FXML Files:** 3 UI layouts
- **CSS Files:** 2 themes
- **Haar Cascades:** 24 OpenCV cascade files
- **Unit Tests:** 21 tests (all passing)
- **Lines of Code:** ~3,500 (Java only, excluding tests)
- **Dependencies:** 15 (managed by Maven)
- **Target JDK:** 23.0.1
- **JavaFX Version:** 23.0.1
- **OpenCV Version:** 4.9.0

## 🔒 Security Notes

- Passwords hashed with BCrypt (10 rounds)
- SQL injection prevention via prepared statements
- Configuration file has restricted permissions (600)
- Session tokens cleared on logout
- No hardcoded credentials in source code

## 🚀 Next Steps

See individual task list for remaining modernization items:

- [ ] Implement CSV export feature
- [ ] Add role-based navigation guards
- [ ] Polish CSS themes
- [ ] Add integration tests
- [ ] Create VS Code launch configurations

---

**Last Updated:** November 11, 2025  
**Version:** 2.0 (Modernized)  
**Maintainer:** IceFX Development Team
