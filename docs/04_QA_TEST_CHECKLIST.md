# ✅ IceFX Quality Assurance Test Checklist

## 🎯 Testing Philosophy

**Test Early, Test Often, Test Everything**

This checklist ensures IceFX is production-ready with comprehensive coverage of:

- **Functional Testing** - Features work as expected
- **Edge Case Testing** - Handles unusual scenarios gracefully
- **Performance Testing** - Meets speed and resource requirements
- **Security Testing** - Protects against vulnerabilities
- **Usability Testing** - User-friendly and intuitive

---

## 📋 Pre-Test Setup

### **Environment Preparation**

- [ ] Clean Maven cache: `rm -rf ~/.m2/repository/org/bytedeco`
- [ ] Clean build: `mvn clean install`
- [ ] Database running (MySQL)
- [ ] Test data loaded (sample users, schedules)
- [ ] Camera connected and functional
- [ ] Testing tools ready (VisualVM, logs viewer)

### **Test Data**

```sql
-- Create test users
INSERT INTO users (username, full_name, email, role, password_hash) VALUES
('admin', 'Test Admin', 'admin@test.com', 'ADMIN', '$2a$10$...'),
('staff1', 'Staff One', 'staff1@test.com', 'STAFF', '$2a$10$...'),
('student1', 'Student One', 'student1@test.com', 'STUDENT', '$2a$10$...');

-- Create test schedules
INSERT INTO schedules (user_id, day_of_week, start_time, end_time) VALUES
(3, 'MONDAY', '09:00:00', '17:00:00');
```

---

## 🚨 CRITICAL: Crash Prevention Tests

**Priority:** 🔴 **BLOCKER - Must pass before any deployment**

### **CP-001: Native Library Loading**

```
✅ Test Case:
├─ Launch application
├─ Expected: OpenCV loads without error
├─ Expected: No UnsatisfiedLinkError
├─ Expected: Logs show "✅ OpenCV loaded successfully"
└─ Pass Criteria: Application starts

❌ Failure Modes:
├─ UnsatisfiedLinkError → Check pom.xml dependencies
├─ "No opencv_java in library path" → Verify opencv-platform
└─ Crash on startup → Check crash logs

Status: [ ] PASS  [ ] FAIL
Notes: ___________________________________________
```

### **CP-002: Camera Thread Safety**

```
✅ Test Case:
├─ Start camera
├─ Run for 30 minutes continuously
├─ Expected: No crashes
├─ Expected: No EXCEPTION_ACCESS_VIOLATION
├─ Expected: Camera FPS stable at ~30
└─ Pass Criteria: Runs 30+ minutes without crash

❌ Failure Modes:
├─ Crash within 5 minutes → Thread safety issue
├─ Gradual slowdown → Memory leak
└─ Intermittent freezes → Blocking UI thread

Status: [ ] PASS  [ ] FAIL
Duration Run: _______ minutes
Crash Logs: [ ] None  [ ] Generated
Notes: ___________________________________________
```

### **CP-003: Face Detection Null Safety**

```
✅ Test Case:
├─ Start camera
├─ Cover lens (no faces)
├─ Uncover lens (show face)
├─ Repeat 20 times
├─ Expected: No NullPointerException
├─ Expected: Graceful "No face detected" messages
└─ Pass Criteria: No crashes during test

❌ Failure Modes:
├─ NullPointerException → Missing null checks
├─ Crash on empty Mat → Validate frame.empty()
└─ Crash on CvSeq access → Update to modern API

Status: [ ] PASS  [ ] FAIL
Notes: ___________________________________________
```

### **CP-004: Memory Management**

```
✅ Test Case:
├─ Start camera
├─ Monitor memory with VisualVM
├─ Run for 60 minutes
├─ Expected: Memory stays under 500MB
├─ Expected: No continuous growth
├─ Expected: GC cleans up properly
└─ Pass Criteria: Memory stable for 1 hour

Memory Readings:
├─ Startup: _______ MB
├─ 15 min:  _______ MB
├─ 30 min:  _______ MB
├─ 45 min:  _______ MB
└─ 60 min:  _______ MB

Status: [ ] PASS  [ ] FAIL
Leak Detected: [ ] YES  [ ] NO
Notes: ___________________________________________
```

### **CP-005: Startup/Shutdown Cycles**

```
✅ Test Case:
├─ Start application
├─ Start camera
├─ Stop camera
├─ Close application
├─ Repeat 10 times
├─ Expected: Clean startup/shutdown every time
├─ Expected: No resource leaks
└─ Pass Criteria: All 10 cycles complete

Cycle Results:
├─ Cycle 1:  [ ] PASS  [ ] FAIL
├─ Cycle 2:  [ ] PASS  [ ] FAIL
├─ Cycle 3:  [ ] PASS  [ ] FAIL
├─ Cycle 4:  [ ] PASS  [ ] FAIL
├─ Cycle 5:  [ ] PASS  [ ] FAIL
├─ Cycle 6:  [ ] PASS  [ ] FAIL
├─ Cycle 7:  [ ] PASS  [ ] FAIL
├─ Cycle 8:  [ ] PASS  [ ] FAIL
├─ Cycle 9:  [ ] PASS  [ ] FAIL
└─ Cycle 10: [ ] PASS  [ ] FAIL

Status: [ ] PASS  [ ] FAIL
Notes: ___________________________________________
```

---

## 🔐 Authentication & Authorization Tests

### **AUTH-001: Valid Login - Admin**

```
├─ Username: admin
├─ Password: correct_password
├─ Expected: Login successful
├─ Expected: Redirect to Admin Panel
├─ Expected: Admin menu items visible
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-002: Valid Login - Staff**

```
├─ Username: staff1
├─ Password: correct_password
├─ Expected: Login successful
├─ Expected: Redirect to Dashboard
├─ Expected: Limited menu (no admin features)
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-003: Invalid Credentials**

```
├─ Username: admin
├─ Password: wrong_password
├─ Expected: Login failed
├─ Expected: Error message "Invalid credentials"
├─ Expected: Remain on login screen
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-004: Empty Username**

```
├─ Username: (empty)
├─ Password: password
├─ Expected: Validation error
├─ Expected: "Username required" message
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-005: Empty Password**

```
├─ Username: admin
├─ Password: (empty)
├─ Expected: Validation error
├─ Expected: "Password required" message
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-006: SQL Injection Attempt**

```
├─ Username: admin' OR '1'='1
├─ Password: anything
├─ Expected: Login failed
├─ Expected: Input sanitized
├─ Expected: No database error
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-007: Role-Based Access Control**

```
Test: Staff user tries to access Admin Panel
├─ Login as staff1
├─ Attempt to open Admin Panel
├─ Expected: Access denied
├─ Expected: "Insufficient permissions" message
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-008: Session Persistence**

```
├─ Login as admin
├─ Navigate to different screens
├─ Expected: Session maintained
├─ Expected: User info displayed consistently
└─ Status: [ ] PASS  [ ] FAIL
```

### **AUTH-009: Logout**

```
├─ Login as admin
├─ Click logout
├─ Expected: Redirect to login screen
├─ Expected: Session cleared
├─ Expected: Cannot navigate back to protected screens
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 📸 Camera & Face Detection Tests

### **CAM-001: Camera Initialization**

```
├─ Click "Start Camera"
├─ Expected: Camera activates within 2 seconds
├─ Expected: Video feed displays
├─ Expected: Status shows "Running"
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-002: Camera Stop**

```
├─ Start camera
├─ Click "Stop Camera"
├─ Expected: Camera stops within 1 second
├─ Expected: Video feed freezes/clears
├─ Expected: Status shows "Stopped"
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-003: No Camera Connected**

```
├─ Disconnect camera
├─ Click "Start Camera"
├─ Expected: Error dialog shown
├─ Expected: "Camera not found" message
├─ Expected: Application remains stable
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-004: Camera Disconnected During Use**

```
├─ Start camera
├─ Unplug camera while running
├─ Expected: Error notification
├─ Expected: Camera stops gracefully
├─ Expected: Can restart when reconnected
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-005: Face Detection - Single Face**

```
├─ Start camera
├─ Show one face to camera
├─ Expected: Green rectangle around face
├─ Expected: "Face detected" indicator
├─ Expected: Detection smooth (no flickering)
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-006: Face Detection - Multiple Faces**

```
├─ Start camera
├─ Show 2-3 faces to camera
├─ Expected: Rectangle around each face
├─ Expected: All faces detected
├─ Expected: Performance acceptable (>20 FPS)
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-007: No Face in Frame**

```
├─ Start camera
├─ Show no faces (empty room)
├─ Expected: No rectangles drawn
├─ Expected: "No face detected" message
├─ Expected: Continuous monitoring
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-008: Partial Face**

```
├─ Start camera
├─ Show face partially (half covered)
├─ Expected: Attempt detection
├─ OR: Show "Face not clear" warning
├─ Expected: No crash
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-009: Poor Lighting**

```
Test A: Very Dark
├─ Start camera in dark room
├─ Expected: Adjusts brightness if possible
├─ OR: Shows "Poor lighting" warning
└─ Status: [ ] PASS  [ ] FAIL

Test B: Very Bright
├─ Start camera with bright backlight
├─ Expected: Handles overexposure
├─ OR: Shows warning
└─ Status: [ ] PASS  [ ] FAIL
```

### **CAM-010: FPS Performance**

```
├─ Start camera
├─ Monitor FPS counter
├─ Expected: 25-30 FPS consistently
├─ Expected: No drops below 20 FPS
├─ Expected: Smooth video
└─ Status: [ ] PASS  [ ] FAIL
   Measured FPS: ______
```

---

## 🧑 Face Recognition Tests

### **REC-001: Recognize Registered User**

```
├─ Register face for student1
├─ Show student1 to camera
├─ Expected: Recognized as "Student One"
├─ Expected: Confidence > 70%
├─ Expected: Name displayed on screen
└─ Status: [ ] PASS  [ ] FAIL
   Confidence: ______%
```

### **REC-002: Unknown Person**

```
├─ Show unregistered person to camera
├─ Expected: "Unknown person" message
├─ Expected: No false positive match
├─ Expected: No attendance logged
└─ Status: [ ] PASS  [ ] FAIL
```

### **REC-003: Similar Looking People**

```
├─ Register two similar-looking people
├─ Show each person separately
├─ Expected: Correct identification for each
├─ Expected: No confusion between them
└─ Status: [ ] PASS  [ ] FAIL
```

### **REC-004: Same Person - Different Lighting**

```
├─ Register person in normal light
├─ Test recognition in:
│  ├─ Bright light  [ ] PASS  [ ] FAIL
│  ├─ Dim light     [ ] PASS  [ ] FAIL
│  └─ Side lighting [ ] PASS  [ ] FAIL
└─ Expected: Recognized in all conditions
```

### **REC-005: Same Person - Different Angles**

```
├─ Register person facing camera
├─ Test recognition at:
│  ├─ Slight left turn  [ ] PASS  [ ] FAIL
│  ├─ Slight right turn [ ] PASS  [ ] FAIL
│  └─ Tilted head       [ ] PASS  [ ] FAIL
└─ Expected: Recognized from different angles
```

### **REC-006: Recognition Debouncing**

```
├─ Recognize person (logs attendance)
├─ Same person shows face within 3 seconds
├─ Expected: "Recently logged" message
├─ Expected: No duplicate attendance entry
├─ Wait 4 seconds
├─ Show face again
├─ Expected: New attendance logged
└─ Status: [ ] PASS  [ ] FAIL
```

### **REC-007: Recognition Speed**

```
├─ Show face to camera
├─ Measure time to recognition
├─ Expected: Recognition within 1 second
├─ Expected: No UI lag
└─ Status: [ ] PASS  [ ] FAIL
   Time: ______ ms
```

### **REC-008: Confidence Threshold**

```
├─ Show face with glasses (if not trained)
├─ Expected: Low confidence score
├─ Expected: Rejection if < threshold
├─ Expected: "Not confident" message
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 📝 Attendance Logging Tests

### **ATT-001: Log Attendance - Valid**

```
├─ Recognize registered user
├─ User has active schedule
├─ Current time within schedule
├─ Expected: Attendance logged to database
├─ Expected: Success notification
├─ Expected: Entry appears in table
└─ Status: [ ] PASS  [ ] FAIL
   Database ID: ______
```

### **ATT-002: Prevent Duplicate Logging**

```
├─ Log attendance for student1
├─ Recognize student1 again within 1 hour
├─ Expected: "Already logged" message
├─ Expected: No duplicate database entry
└─ Status: [ ] PASS  [ ] FAIL
```

### **ATT-003: Log Outside Schedule**

```
├─ Recognize user
├─ Current time outside schedule hours
├─ Expected: Warning "Outside schedule"
├─ Expected: Allow logging with warning
└─ Status: [ ] PASS  [ ] FAIL
```

### **ATT-004: Log Without Schedule**

```
├─ Recognize user with no schedule
├─ Expected: Attendance logged
├─ Expected: Warning "No schedule assigned"
└─ Status: [ ] PASS  [ ] FAIL
```

### **ATT-005: Attendance Table Updates**

```
├─ Log attendance
├─ Expected: New row in table immediately
├─ Expected: Correct user name
├─ Expected: Correct timestamp
├─ Expected: Table scrolls to new entry
└─ Status: [ ] PASS  [ ] FAIL
```

### **ATT-006: Multiple Users in Sequence**

```
├─ Recognize student1 → log
├─ Recognize student2 → log
├─ Recognize student3 → log
├─ Expected: All three logged correctly
├─ Expected: No confusion between users
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 👥 User Management Tests (Admin)

### **USR-001: Create New User**

```
├─ Login as admin
├─ Navigate to User Management
├─ Fill user form:
│  ├─ Username: testuser
│  ├─ Full Name: Test User
│  ├─ Email: test@example.com
│  ├─ Role: STUDENT
│  └─ Password: Test@123
├─ Click Save
├─ Expected: User created
├─ Expected: Appears in user table
└─ Status: [ ] PASS  [ ] FAIL
   User ID: ______
```

### **USR-002: Validation - Duplicate Username**

```
├─ Try to create user with existing username
├─ Expected: Error "Username already exists"
├─ Expected: User not created
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-003: Validation - Invalid Email**

```
├─ Enter invalid email (e.g., "notanemail")
├─ Expected: Error "Invalid email format"
├─ Expected: Cannot save
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-004: Validation - Weak Password**

```
├─ Enter weak password (e.g., "123")
├─ Expected: Error "Password too weak"
├─ Expected: Password requirements shown
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-005: Update User Details**

```
├─ Select existing user
├─ Change full name
├─ Click Update
├─ Expected: Changes saved
├─ Expected: Updated name in table
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-006: Change User Role**

```
├─ Select STUDENT user
├─ Change role to STAFF
├─ Click Update
├─ Expected: Role updated
├─ Expected: User gets staff permissions
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-007: Delete User**

```
├─ Select user
├─ Click Delete
├─ Confirm deletion
├─ Expected: User removed from table
├─ Expected: User deactivated in database
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-008: Search Users**

```
├─ Enter search query "John"
├─ Expected: Only matching users shown
├─ Expected: Search is case-insensitive
├─ Expected: Searches name, username, email
└─ Status: [ ] PASS  [ ] FAIL
```

### **USR-009: Filter by Role**

```
├─ Select role filter "ADMIN"
├─ Expected: Only admin users shown
├─ Expected: Count accurate
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 📅 Schedule Management Tests (Admin)

### **SCH-001: Create Schedule**

```
├─ Select user
├─ Create schedule:
│  ├─ Day: Monday
│  ├─ Start: 09:00
│  └─ End: 17:00
├─ Click Save
├─ Expected: Schedule created
├─ Expected: Appears in user's schedule list
└─ Status: [ ] PASS  [ ] FAIL
```

### **SCH-002: Validation - End Before Start**

```
├─ Start time: 17:00
├─ End time: 09:00
├─ Expected: Error "End time must be after start"
├─ Expected: Cannot save
└─ Status: [ ] PASS  [ ] FAIL
```

### **SCH-003: Validation - Overlapping Schedules**

```
├─ Create schedule: Mon 09:00-17:00
├─ Try to create: Mon 15:00-18:00
├─ Expected: Error "Schedule overlaps"
├─ Expected: Cannot save
└─ Status: [ ] PASS  [ ] FAIL
```

### **SCH-004: Update Schedule**

```
├─ Select existing schedule
├─ Change end time
├─ Click Update
├─ Expected: Schedule updated
├─ Expected: No conflicts created
└─ Status: [ ] PASS  [ ] FAIL
```

### **SCH-005: Delete Schedule**

```
├─ Select schedule
├─ Click Delete
├─ Confirm
├─ Expected: Schedule removed
├─ Expected: User can log attendance without schedule
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 📊 Reporting & Export Tests

### **REP-001: View Attendance Report**

```
├─ Navigate to Reports
├─ Select date range (last 7 days)
├─ Expected: Attendance list displayed
├─ Expected: Correct entries shown
├─ Expected: Sorted by date (newest first)
└─ Status: [ ] PASS  [ ] FAIL
```

### **REP-002: Export to CSV**

```
├─ Generate report
├─ Click "Export CSV"
├─ Expected: File download dialog
├─ Expected: CSV file created
├─ Open CSV
├─ Expected: All columns present
├─ Expected: Data matches report
└─ Status: [ ] PASS  [ ] FAIL
   File: ______________________
```

### **REP-003: Filter by User**

```
├─ Select specific user
├─ Generate report
├─ Expected: Only that user's attendance shown
└─ Status: [ ] PASS  [ ] FAIL
```

### **REP-004: Filter by Date Range**

```
├─ Select date range: Jan 1 - Jan 31
├─ Generate report
├─ Expected: Only entries in range shown
├─ Expected: No entries outside range
└─ Status: [ ] PASS  [ ] FAIL
```

### **REP-005: Attendance Statistics**

```
├─ View statistics panel
├─ Expected: Total attendance count
├─ Expected: Attendance by day chart
├─ Expected: Most active users
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 💾 Database Tests

### **DB-001: Connection Pool**

```
├─ Start application
├─ Expected: HikariCP initializes
├─ Expected: 10 connections in pool
├─ Check logs for "HikariCP started"
└─ Status: [ ] PASS  [ ] FAIL
```

### **DB-002: Database Unavailable at Startup**

```
├─ Stop MySQL
├─ Start application
├─ Expected: Error dialog "Cannot connect to database"
├─ Expected: Retry option
└─ Status: [ ] PASS  [ ] FAIL
```

### **DB-003: Database Connection Lost**

```
├─ Application running
├─ Stop MySQL
├─ Perform database operation
├─ Expected: Error notification
├─ Expected: Automatic reconnect attempt
└─ Status: [ ] PASS  [ ] FAIL
```

### **DB-004: Transaction Rollback**

```
├─ Simulate failed transaction (e.g., constraint violation)
├─ Expected: Transaction rolled back
├─ Expected: No partial data saved
├─ Expected: Database consistent
└─ Status: [ ] PASS  [ ] FAIL
```

### **DB-005: Query Performance**

```
Test with 10,000+ records:
├─ Search users: _______ ms (< 500ms)
├─ Load attendance: _______ ms (< 500ms)
├─ Generate report: _______ ms (< 1000ms)
└─ Status: [ ] PASS  [ ] FAIL
```

### **DB-006: Connection Leak Detection**

```
├─ Perform 100 database operations
├─ Check connection pool stats
├─ Expected: All connections returned to pool
├─ Expected: No connection leaks
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 🎨 UI/UX Tests

### **UI-001: Window Resize**

```
├─ Resize window (larger, smaller)
├─ Expected: UI adapts responsively
├─ Expected: No clipping or overlap
├─ Expected: All controls accessible
└─ Status: [ ] PASS  [ ] FAIL
```

### **UI-002: Theme Switching**

```
├─ Switch from light to dark theme
├─ Expected: All UI elements update
├─ Expected: Readable text in both themes
├─ Expected: Consistent styling
└─ Status: [ ] PASS  [ ] FAIL
```

### **UI-003: Navigation**

```
├─ Click through all menu items
├─ Expected: All screens load
├─ Expected: Back navigation works
├─ Expected: No broken links
└─ Status: [ ] PASS  [ ] FAIL
```

### **UI-004: Form Validation**

```
├─ Submit empty forms
├─ Expected: Inline validation errors
├─ Expected: Red border on invalid fields
├─ Expected: Clear error messages
└─ Status: [ ] PASS  [ ] FAIL
```

### **UI-005: Loading Indicators**

```
├─ Perform slow operation (e.g., large report)
├─ Expected: Progress spinner shown
├─ Expected: UI remains responsive
├─ Expected: Can cancel operation
└─ Status: [ ] PASS  [ ] FAIL
```

### **UI-006: Tooltips & Help**

```
├─ Hover over buttons/icons
├─ Expected: Helpful tooltips shown
├─ Expected: Context-sensitive help
└─ Status: [ ] PASS  [ ] FAIL
```

---

## ⚡ Performance Tests

### **PERF-001: Application Startup Time**

```
├─ Close application
├─ Launch application
├─ Measure time to main screen
├─ Expected: < 5 seconds
└─ Status: [ ] PASS  [ ] FAIL
   Time: ______ seconds
```

### **PERF-002: Camera Startup Time**

```
├─ Click "Start Camera"
├─ Measure time to video feed
├─ Expected: < 2 seconds
└─ Status: [ ] PASS  [ ] FAIL
   Time: ______ seconds
```

### **PERF-003: Face Recognition Latency**

```
├─ Show face to camera
├─ Measure time to recognition
├─ Expected: < 1 second
└─ Status: [ ] PASS  [ ] FAIL
   Time: ______ ms
```

### **PERF-004: Database Query Speed**

```
With 10,000 records:
├─ SELECT user by ID: _______ ms (< 10ms)
├─ Search users: _______ ms (< 500ms)
├─ Load attendance (last 30 days): _______ ms (< 500ms)
└─ Status: [ ] PASS  [ ] FAIL
```

### **PERF-005: Memory Usage**

```
After 1 hour operation:
├─ Heap memory: _______ MB (< 500MB)
├─ Non-heap memory: _______ MB (< 200MB)
├─ GC activity: Normal / Excessive
└─ Status: [ ] PASS  [ ] FAIL
```

### **PERF-006: CPU Usage**

```
During camera operation:
├─ CPU usage: _______ % (< 50%)
├─ Threads: _______ (< 50)
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 🔒 Security Tests

### **SEC-001: Password Storage**

```
├─ Create user with password "Test@123"
├─ Check database password_hash column
├─ Expected: BCrypt hash (starts with $2a$)
├─ Expected: Not plaintext
└─ Status: [ ] PASS  [ ] FAIL
```

### **SEC-002: SQL Injection Prevention**

```
├─ Enter malicious input in search: `'; DROP TABLE users; --`
├─ Expected: Input sanitized
├─ Expected: No database error
├─ Expected: No SQL execution
└─ Status: [ ] PASS  [ ] FAIL
```

### **SEC-003: XSS Prevention**

```
├─ Enter script in username: `<script>alert('XSS')</script>`
├─ Expected: Script not executed
├─ Expected: Displayed as plain text
└─ Status: [ ] PASS  [ ] FAIL
```

### **SEC-004: Authentication Required**

```
├─ Try to access protected pages without login
├─ Expected: Redirect to login
├─ Expected: Cannot bypass authentication
└─ Status: [ ] PASS  [ ] FAIL
```

### **SEC-005: Role Enforcement**

```
├─ Login as STAFF
├─ Try to access admin-only features
├─ Expected: Access denied
├─ Expected: Clear error message
└─ Status: [ ] PASS  [ ] FAIL
```

---

## 🌐 Cross-Platform Tests

### **CP-001: Windows 10 Compatibility**

```
├─ Install on Windows 10
├─ Expected: Application runs
├─ Expected: Native libraries load
├─ Expected: All features work
└─ Status: [ ] PASS  [ ] FAIL  [ ] N/A
```

### **CP-002: Windows 11 Compatibility**

```
├─ Install on Windows 11
├─ Expected: Application runs
├─ Expected: Native libraries load
├─ Expected: All features work
└─ Status: [ ] PASS  [ ] FAIL  [ ] N/A
```

### **CP-003: Ubuntu 20.04 Compatibility**

```
├─ Install on Ubuntu 20.04
├─ Expected: Application runs
├─ Expected: Native libraries load
├─ Expected: Camera detected
└─ Status: [ ] PASS  [ ] FAIL  [ ] N/A
```

### **CP-004: Ubuntu 22.04 Compatibility**

```
├─ Install on Ubuntu 22.04
├─ Expected: Application runs
├─ Expected: Native libraries load
├─ Expected: Camera detected
└─ Status: [ ] PASS  [ ] FAIL  [ ] N/A
```

---

## 📋 Final Acceptance Checklist

### **Critical Requirements**

- [ ] ✅ No JVM crashes for 1 hour continuous operation
- [ ] ✅ Native libraries load successfully on all platforms
- [ ] ✅ Camera runs at 25-30 FPS consistently
- [ ] ✅ Face recognition accuracy > 90%
- [ ] ✅ Authentication system working
- [ ] ✅ Role-based access control enforced
- [ ] ✅ Attendance logging accurate
- [ ] ✅ Database operations < 500ms

### **High Priority**

- [ ] All core features functional
- [ ] UI responsive and modern
- [ ] No memory leaks
- [ ] CSV export working
- [ ] User management complete
- [ ] Schedule management complete

### **Medium Priority**

- [ ] Dark theme implemented
- [ ] Comprehensive validation
- [ ] Helpful error messages
- [ ] Documentation complete

### **Sign-Off**

**Tested By:** ************\_\_\_************  
**Date:** ******\_\_******  
**Environment:** [ ] Windows [ ] Linux [ ] macOS  
**JDK Version:** ******\_\_\_******  
**Build Version:** ******\_\_\_******

**Overall Result:** [ ] ✅ PASS - Ready for Production  
 [ ] ⚠️ PASS WITH ISSUES - See notes  
 [ ] ❌ FAIL - Blocking issues found

**Notes:**

---

---

---

**Blocker Issues:**

---

---

**Recommendations:**

---

---

---

**Last Updated:** November 11, 2025  
**Version:** 1.0  
**Status:** Ready for QA Team
