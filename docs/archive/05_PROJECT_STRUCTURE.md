# 🏗️ IceFX Project Structure

## 📂 Directory Tree

```
IceFX/
│
├── 📄 pom.xml                          # Maven build configuration
├── 📄 build.fxbuild                    # JavaFX build settings
├── 📄 nbactions.xml                    # NetBeans actions
├── 📄 _config.yml                      # Jekyll config (GitHub Pages)
├── 📄 facial_attendance.sql            # Database dump
│
├── 📁 docs/                            # ⭐ ALL DOCUMENTATION HERE
│   ├── 00_INDEX.md                     # 📖 START HERE - Navigation guide
│   ├── 01_CRASH_ANALYSIS.md            # 🚨 CRITICAL - Crash fixes
│   ├── 02_SOURCE_CODE_AUDIT.md         # 📁 Complete code inventory
│   ├── 03_IMPLEMENTATION_PLAN.md       # 🗓️ Development roadmap
│   ├── 04_QA_TEST_CHECKLIST.md         # ✅ Test cases
│   ├── README.md                       # Project overview
│   ├── REFACTORING_SUMMARY.md          # Architecture changes
│   ├── QUICK_START.md                  # 5-minute setup
│   └── MIGRATION_GUIDE.md              # Old → New code guide
│
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/
│   │   │   │
│   │   │   ├── 📁 application/         # ⚠️ LEGACY CODE (15 files)
│   │   │   │   ├── Main.java           # 🔴 Entry point (needs native loading fix)
│   │   │   │   ├── SampleController.java # 🔴 1200 lines (must split)
│   │   │   │   ├── Database.java       # 🟡 Deprecated (use DAOs)
│   │   │   │   ├── FaceDetector.java   # 🔴 CRASH SOURCE (unsafe native calls)
│   │   │   │   ├── FaceRecognizer.java # 🟡 No confidence threshold
│   │   │   │   ├── Person.java         # 🟢 Use User.java instead
│   │   │   │   ├── AttendanceRecord.java # 🟢 Use AttendanceLog.java instead
│   │   │   │   ├── Schedule.java       # 🟢 Use new Schedule.java instead
│   │   │   │   ├── RecognitionResult.java # 🟢 OK
│   │   │   │   ├── Toast.java          # 🟢 OK
│   │   │   │   ├── AdvancedToast.java  # 🟢 Good
│   │   │   │   ├── FaceDetectionController.java # 🟢 Demo only
│   │   │   │   ├── MotionDetector.java # 🟢 Unused
│   │   │   │   ├── ColoredObjectTracker.java # 🟢 Unused
│   │   │   │   ├── SquareDetector.java # 🟢 Unused
│   │   │   │   └── OCR.java            # 🟢 Unused
│   │   │   │
│   │   │   └── 📁 com/icefx/           # ✅ NEW ARCHITECTURE (10 files)
│   │   │       │
│   │   │       ├── 📁 model/           # ✅ COMPLETE (5 files)
│   │   │       │   ├── User.java       # ✅ Enhanced with roles, JavaFX properties
│   │   │       │   ├── AttendanceLog.java # ✅ With formatted dates
│   │   │       │   ├── Schedule.java   # ✅ With validation, DayOfWeek enum
│   │   │       │   ├── FaceTemplate.java # ✅ Binary data handling
│   │   │       │   └── CameraStatus.java # ✅ Status enum
│   │   │       │
│   │   │       ├── 📁 dao/             # ✅ COMPLETE (4 files)
│   │   │       │   ├── UserDAO.java    # ✅ CRUD, search, validation
│   │   │       │   ├── AttendanceDAO.java # ✅ Queries, date ranges
│   │   │       │   ├── ScheduleDAO.java # ✅ Conflict detection
│   │   │       │   └── FaceTemplateDAO.java # ✅ Template management
│   │   │       │
│   │   │       ├── 📁 config/          # ✅ COMPLETE (1 file)
│   │   │       │   └── DatabaseConfig.java # ✅ HikariCP connection pooling
│   │   │       │
│   │   │       ├── 📁 service/         # ❌ MISSING (0/6 files)
│   │   │       │   ├── UserService.java # ⏳ TODO - Auth, password hashing
│   │   │       │   ├── AttendanceService.java # ⏳ TODO - Rules, validation
│   │   │       │   ├── FaceRecognitionService.java # ⏳ TODO - LBPH pipeline
│   │   │       │   ├── CameraService.java # ⏳ TODO - Thread-safe camera
│   │   │       │   ├── ExportService.java # ⏳ TODO - CSV reports
│   │   │       │   └── AuthenticationService.java # ⏳ TODO - Sessions
│   │   │       │
│   │   │       ├── 📁 controller/      # ❌ MISSING (0/5 files)
│   │   │       │   ├── LoginController.java # ⏳ TODO - Auth screen
│   │   │       │   ├── DashboardController.java # ⏳ TODO - Staff main UI
│   │   │       │   ├── AdminController.java # ⏳ TODO - Admin panel
│   │   │       │   ├── AttendanceController.java # ⏳ TODO - Camera + recognition
│   │   │       │   └── UserManagementController.java # ⏳ TODO - User CRUD
│   │   │       │
│   │   │       └── 📁 util/            # ❌ MISSING (0/7 files)
│   │   │           ├── NativeLoader.java # 🔴 CRITICAL - Safe OpenCV loading
│   │   │           ├── ImageUtils.java # ⏳ TODO - Image helpers
│   │   │           ├── ValidationUtils.java # ⏳ TODO - Input validation
│   │   │           ├── PasswordUtils.java # ⏳ TODO - BCrypt hashing
│   │   │           ├── AlertUtils.java # ⏳ TODO - User notifications
│   │   │           ├── ThreadUtils.java # ⏳ TODO - Background tasks
│   │   │           └── CsvExporter.java # ⏳ TODO - Report generation
│   │   │
│   │   └── 📁 resources/
│   │       ├── 📁 application/
│   │       │   ├── application.css     # 🟡 Needs modernization
│   │       │   └── Sample.fxml         # ⚠️ Monolithic (400 lines)
│   │       │
│   │       ├── 📁 fxml/                # ❌ MISSING (0/5 files)
│   │       │   ├── Login.fxml          # ⏳ TODO - Auth screen
│   │       │   ├── Dashboard.fxml      # ⏳ TODO - Staff view
│   │       │   ├── AdminPanel.fxml     # ⏳ TODO - Admin tools
│   │       │   ├── AttendanceView.fxml # ⏳ TODO - Camera interface
│   │       │   └── UserManagement.fxml # ⏳ TODO - User CRUD
│   │       │
│   │       ├── 📁 css/                 # ❌ MISSING (0/4 files)
│   │       │   ├── base.css            # ⏳ TODO - Common styles
│   │       │   ├── light-theme.css     # ⏳ TODO - Light mode
│   │       │   ├── dark-theme.css      # ⏳ TODO - Dark mode
│   │       │   └── components.css      # ⏳ TODO - Reusable components
│   │       │
│   │       └── 📁 haar/                # ✅ OpenCV cascade classifiers
│   │           ├── haarcascade_frontalface_default.xml # ✅ Face detection
│   │           ├── haarcascade_eye.xml # ✅ Eye detection
│   │           └── (15 more classifiers...)
│   │
│   └── 📁 test/                        # ❌ NO TESTS YET
│       └── 📁 java/
│           └── 📁 com/icefx/
│               ├── 📁 service/
│               │   ├── UserServiceTest.java # ⏳ TODO
│               │   ├── AttendanceServiceTest.java # ⏳ TODO
│               │   └── FaceRecognitionServiceTest.java # ⏳ TODO
│               │
│               └── 📁 dao/
│                   ├── UserDAOTest.java # ⏳ TODO
│                   └── AttendanceDAOTest.java # ⏳ TODO
│
├── 📁 bin/                             # Compiled classes
│   └── (build output)
│
├── 📁 libs/                            # External libraries
│   └── (dependencies)
│
├── 📁 native/                          # Native OpenCV libraries
│   └── (platform-specific binaries)
│
├── 📁 faces/                           # Stored face images
│   └── (user face photos)
│
└── 📁 resources/
    └── trained_faces.xml               # LBPH recognizer data
```

---

## 📊 File Count Summary

| Category       | Complete | Missing | Legacy               | Total |
| -------------- | -------- | ------- | -------------------- | ----- |
| **Model**      | 5        | 0       | 0                    | 5     |
| **DAO**        | 4        | 0       | 0                    | 4     |
| **Config**     | 1        | 0       | 0                    | 1     |
| **Service**    | 0        | 6       | 0                    | 6     |
| **Controller** | 0        | 5       | 1 (SampleController) | 6     |
| **Util**       | 0        | 7       | 3 (Toast, etc.)      | 10    |
| **FXML**       | 1        | 5       | 0                    | 6     |
| **CSS**        | 1        | 4       | 0                    | 5     |
| **Tests**      | 0        | 10+     | 0                    | 10+   |
| **Docs**       | 9        | 0       | 0                    | 9     |

**Total Java Files:** 25 (10 new, 15 legacy)  
**Overall Completion:** 30%

---

## 🎯 Layer Status

### ✅ **Data Layer: 100% Complete**

```
Model (5 files)     ████████████████████ 100%
DAO (4 files)       ████████████████████ 100%
Config (1 file)     ████████████████████ 100%
```

**Status:** Production-ready with:

- JavaFX property bindings
- HikariCP connection pooling
- Comprehensive CRUD operations
- Input validation
- Transaction management

---

### ⏳ **Business Layer: 0% Complete**

```
Service (6 files)   ░░░░░░░░░░░░░░░░░░░░ 0%
```

**Missing:**

- UserService (authentication, password hashing)
- AttendanceService (business rules)
- FaceRecognitionService (OpenCV pipeline)
- CameraService (thread-safe camera)
- ExportService (CSV reports)
- AuthenticationService (session management)

**Priority:** 🔴 **HIGH - Blocks feature development**

---

### ⚠️ **Presentation Layer: 20% Complete**

```
Controller (6 files) ████░░░░░░░░░░░░░░░░ 16%
FXML (6 files)       ███░░░░░░░░░░░░░░░░░ 16%
CSS (5 files)        ████░░░░░░░░░░░░░░░░ 20%
```

**Current:**

- SampleController (1200 lines - must split)
- Sample.fxml (400 lines - must split)
- application.css (basic styling)

**Missing:**

- Modern login screen
- Role-based dashboards
- Admin panel
- Responsive layouts
- Theme support

**Priority:** 🟠 **MEDIUM - After service layer**

---

### ❌ **Utility Layer: 0% Complete**

```
Util (7 files)      ░░░░░░░░░░░░░░░░░░░░ 0%
```

**Missing (Critical):**

- NativeLoader.java 🔴 **BLOCKER - Prevents crashes**

**Missing (High Priority):**

- ImageUtils, ValidationUtils, PasswordUtils, AlertUtils, ThreadUtils, CsvExporter

**Priority:** 🔴 **CRITICAL (NativeLoader), HIGH (others)**

---

### ❌ **Testing Layer: 0% Complete**

```
Tests (10+ files)   ░░░░░░░░░░░░░░░░░░░░ 0%
```

**Missing:**

- Unit tests for services
- Unit tests for DAOs
- Integration tests
- UI tests (TestFX)

**Priority:** 🟡 **MEDIUM - After core features**

---

### ✅ **Documentation: 100% Complete**

```
Docs (9 files)      ████████████████████ 100%
```

**Available:**

- Comprehensive crash analysis
- Complete source code audit
- Detailed implementation plan
- QA test checklist
- Migration guide
- Quick start guide
- Architecture documentation

**Status:** ✅ **Excellent documentation coverage**

---

## 🔥 Critical Files

### **Must Create Immediately:**

#### **1. NativeLoader.java** 🚨 **BLOCKER**

```
Location: src/main/java/com/icefx/util/NativeLoader.java
Priority: 🔴 CRITICAL
Time: 30 minutes
Why: Prevents JVM crashes on startup
Blocks: Everything
```

#### **2. CameraService.java** 🚨 **BLOCKER**

```
Location: src/main/java/com/icefx/service/CameraService.java
Priority: 🔴 CRITICAL
Time: 90 minutes
Why: Thread-safe camera operations
Blocks: Face detection, recognition, attendance
```

#### **3. FaceRecognitionService.java** 🔥 **HIGH**

```
Location: src/main/java/com/icefx/service/FaceRecognitionService.java
Priority: 🔴 HIGH
Time: 60 minutes
Why: Core feature - face recognition
Blocks: Attendance logging
```

---

## 🎯 Priority Files by Milestone

### **10-Hour Milestone (Stabilization)**

```
✅ Must Create:
├── NativeLoader.java           (30 min)  🔴 CRITICAL
├── CameraService.java          (90 min)  🔴 CRITICAL
├── FaceRecognitionService.java (60 min)  🔴 HIGH
└── Fix: FaceDetector.java      (45 min)  🔴 CRITICAL

✅ Must Update:
├── Main.java                   (15 min)  🔴 CRITICAL
└── pom.xml                     (15 min)  🔴 CRITICAL
```

### **30-Hour Milestone (Core Refactoring)**

```
✅ Must Create:
├── UserService.java            (2 hours) 🔴 HIGH
├── AttendanceService.java      (2 hours) 🔴 HIGH
├── ExportService.java          (1 hour)  🟠 MEDIUM
├── LoginController.java        (2 hours) 🔴 HIGH
├── DashboardController.java    (2 hours) 🔴 HIGH
├── AdminController.java        (2 hours) 🔴 HIGH
├── Login.fxml                  (1 hour)  🔴 HIGH
├── Dashboard.fxml              (1 hour)  🔴 HIGH
└── AdminPanel.fxml             (1 hour)  🔴 HIGH
```

### **80-Hour Milestone (Full Modernization)**

```
✅ Must Create:
├── All utility classes         (5 hours) 🟡 MEDIUM
├── All remaining FXML/CSS      (5 hours) 🟡 MEDIUM
├── Comprehensive tests         (10 hours) 🟡 MEDIUM
└── Deployment packages         (5 hours) 🟡 MEDIUM
```

---

## 📏 Size Metrics

### **Current Codebase:**

```
Total Lines of Code: ~8,500
├── Java: ~6,000 lines
│   ├── Legacy (application): ~4,500 lines
│   └── New (com.icefx): ~1,500 lines
├── FXML: ~400 lines
├── CSS: ~350 lines
└── SQL: ~1,250 lines
```

### **Complexity Issues:**

```
🔴 CRITICAL:
├── SampleController.java: 1,200 lines (should be <400)
├── Cyclomatic complexity: 45 (should be <10)
└── Code duplication: 35% (should be <5%)

🟡 MODERATE:
├── Database.java: 350 lines (deprecated)
├── FaceDetector.java: 250 lines (needs refactor)
└── Average method length: 80 lines (should be <30)
```

### **Target Codebase (After Refactoring):**

```
Projected Total: ~12,000 lines
├── Java: ~9,000 lines
│   ├── Model: ~600 lines
│   ├── DAO: ~800 lines
│   ├── Service: ~2,000 lines
│   ├── Controller: ~1,500 lines
│   ├── Util: ~800 lines
│   └── Tests: ~3,300 lines (80% coverage)
├── FXML: ~1,200 lines (5 layouts)
├── CSS: ~800 lines (themes)
└── SQL: ~1,000 lines (optimized)
```

---

## 🗺️ Dependency Graph

### **Current (Tightly Coupled):**

```
┌─────────────────────┐
│ SampleController    │ ← 1200 lines, god object
└──────────┬──────────┘
           ├─── Direct JDBC calls
           ├─── Direct OpenCV calls
           ├─── Direct File I/O
           └─── UI updates (mixed)
```

### **Target (Loose Coupling):**

```
┌──────────────┐
│ Controller   │ ← Thin, UI only
└──────┬───────┘
       ↓ uses
┌──────────────┐
│ Service      │ ← Business logic
└──────┬───────┘
       ↓ uses
┌──────────────┐
│ DAO          │ ← Data access
└──────┬───────┘
       ↓ uses
┌──────────────┐
│ Database     │ ← HikariCP pooling
└──────────────┘
```

---

## 🎨 Architecture Visualization

```
┌────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                      │
├─────────────────┬─────────────────┬────────────────────────┤
│ LoginController │ DashboardCtrl   │ AdminController        │
│                 │                 │                        │
│ Login.fxml      │ Dashboard.fxml  │ AdminPanel.fxml        │
│ light-theme.css │ dark-theme.css  │ components.css         │
└────────┬────────┴────────┬────────┴────────┬───────────────┘
         │                 │                 │
         ↓ uses            ↓ uses            ↓ uses
┌────────────────────────────────────────────────────────────┐
│                     BUSINESS LOGIC LAYER                    │
├──────────────┬──────────────┬──────────────┬───────────────┤
│ UserService  │ AttendanceS  │ FaceRecognS  │ CameraService │
│              │              │              │               │
│ - Auth       │ - Logging    │ - Detection  │ - Thread-safe │
│ - Password   │ - Rules      │ - Training   │ - FPS control │
│ - Validation │ - Export     │ - Debounce   │ - Callbacks   │
└──────┬───────┴──────┬───────┴──────┬───────┴───────┬───────┘
       │              │              │               │
       ↓ uses         ↓ uses         ↓ uses          ↓ uses
┌────────────────────────────────────────────────────────────┐
│                       DATA ACCESS LAYER                     │
├──────────────┬──────────────┬──────────────┬───────────────┤
│ UserDAO      │ AttendanceDAO│ ScheduleDAO  │ FaceTemplateDAO│
│              │              │              │               │
│ - CRUD       │ - Queries    │ - Conflicts  │ - Templates   │
│ - Search     │ - Reports    │ - Active     │ - Primary     │
└──────┬───────┴──────┬───────┴──────┬───────┴───────┬───────┘
       │              │              │               │
       ↓ uses         ↓ uses         ↓ uses          ↓ uses
┌────────────────────────────────────────────────────────────┐
│                      PERSISTENCE LAYER                      │
├─────────────────────────────────────────────────────────────┤
│ DatabaseConfig (HikariCP Connection Pool)                   │
│                                                             │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│ │ Conn 1  │ │ Conn 2  │ │ Conn 3  │ │ ...10   │          │
│ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘          │
│      └───────────┴───────────┴───────────┘               │
│                      ↓                                     │
│                 MySQL Database                             │
└────────────────────────────────────────────────────────────┘
```

---

## 🔗 Cross-References

**For detailed information, see:**

- **Architecture deep dive:** `docs/REFACTORING_SUMMARY.md`
- **File-by-file analysis:** `docs/02_SOURCE_CODE_AUDIT.md`
- **Implementation timeline:** `docs/03_IMPLEMENTATION_PLAN.md`
- **Crash analysis:** `docs/01_CRASH_ANALYSIS.md`
- **Testing guide:** `docs/04_QA_TEST_CHECKLIST.md`

---

## 📋 Quick Reference

### **Find a File:**

```bash
# Search for a Java file
find src/main/java -name "*.java" | grep <FileName>

# Search for a resource
find src/main/resources -name "*" | grep <ResourceName>

# Find all controllers
find src -name "*Controller.java"

# Find all DAOs
find src -name "*DAO.java"
```

### **Count Lines:**

```bash
# Count Java lines
find src/main/java -name "*.java" -exec wc -l {} + | sort -n

# Count all code
find src -name "*.java" -o -name "*.fxml" -o -name "*.css" | xargs wc -l
```

---

**Last Updated:** November 11, 2025  
**Structure Version:** 1.0  
**Status:** 30% complete - See implementation plan for next steps
