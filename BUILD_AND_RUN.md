# 🚀 IceFX Build and Run Guide

## Prerequisites

### Required Software

- **JDK 17 or later** (tested with JDK 17, 21, 23)
- **Maven 3.8+**
- **MySQL 8.0+** (or compatible database)
- **Webcam** (for face detection)

### Verify Installation

```bash
# Check Java version
java -version  # Should show 17 or higher

# Check Maven version
mvn -version

# Check MySQL is running
mysql --version
```

---

## 🔧 Initial Setup

### 1. Clone Repository

```bash
git clone https://github.com/Calliduz/IceFX.git
cd IceFX
```

### 2. Setup Database

```bash
# Login to MySQL
mysql -u root -p

# Create database
CREATE DATABASE facial_attendance;

# Import schema
mysql -u root -p facial_attendance < facial_attendance.sql

# Or if SQL file is named differently:
mysql -u root -p facial_attendance < "facial_attendance May 18 2025, 3.45AM.sql"
```

### 3. Configure Database Connection

IceFX stores runtime settings in `~/.icefx/config.properties`. Launch the app (or run `mvn -q -DskipTests compile`) once to generate the file, then edit it:

```properties
# Select backend
db.type=mysql

# MySQL settings
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=facial_attendance
db.mysql.username=root
db.mysql.password=your_password_here

# Optional SQLite fallback (when db.type=sqlite)
db.sqlite.path=data/facial_attendance.db

# Connection pool tuning
db.pool.maxSize=10
db.pool.minIdle=2
db.pool.connectionTimeout=30000
db.pool.idleTimeout=600000
db.pool.maxLifetime=1800000
```

---

## 🏗️ Build Project

### Option 1: Clean Build (Recommended First Time)

```bash
# Clean and install all dependencies
mvn clean install

# This will:
# - Download all Maven dependencies
# - Download platform-specific OpenCV natives
# - Compile Java sources
# - Run tests (if any)
# - Create target/IceFX-1.0.0.jar
```

### Option 2: Quick Build (Skip Tests)

```bash
mvn clean install -DskipTests
```

### Option 3: Clean Maven Cache (If Issues)

```bash
# If you encounter native library issues:
rm -rf ~/.m2/repository/org/bytedeco

# Then rebuild
mvn clean install
```

---

## ▶️ Run Application

### Method 1: Maven JavaFX Plugin (Recommended)

```bash
# Run directly with Maven
mvn javafx:run

# This automatically handles:
# - JavaFX module path
# - Native library extraction
# - Logging configuration
```

### Method 2: NetBeans IDE

1. Open project in NetBeans
2. Right-click project → **Clean and Build**
3. Right-click project → **Run**

### Method 3: IntelliJ IDEA

1. Open project in IntelliJ
2. Maven → Reload All Maven Projects
3. Run → Edit Configurations
   - Main class: `application.Main`
   - VM options: `--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED`
4. Run application

### Method 4: Eclipse IDE

1. Import as Maven project
2. Right-click → Run As → Maven Build
   - Goals: `clean javafx:run`

### Method 5: Command Line JAR

```bash
# Build fat JAR
mvn clean package

# Run JAR (requires JavaFX runtime)
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.swing \
     --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED \
     -jar target/IceFX-1.0.0.jar
```

---

## 🐛 Troubleshooting

### Issue: "UnsatisfiedLinkError: no opencv_java in java.library.path"

**Cause:** OpenCV native libraries not found

**Solution:**

```bash
# Clean bytedeco cache
rm -rf ~/.m2/repository/org/bytedeco

# Rebuild with clean install
mvn clean install

# Verify opencv-platform dependency in pom.xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>opencv-platform</artifactId>
    <version>4.9.0-1.5.10</version>
</dependency>
```

---

### Issue: "Failed to load OpenCV" Dialog

**What it shows:**

- OS and Java version
- Error message
- Remediation steps

**Common fixes:**

1. **Wrong Java version:** Use JDK 17+
2. **Architecture mismatch:** Ensure 64-bit Java on 64-bit OS
3. **Corrupted cache:** Delete `~/.m2/repository/org/bytedeco`
4. **Missing dependency:** Verify `opencv-platform` in pom.xml

---

### Issue: JVM Crash (hs_err_pid\*.log files)

**If you see crash logs:**

1. **Check OpenCV version mismatch:**

   ```bash
   # Look for this in crash log:
   # Problematic frame: C [opencv_core320.dll+0x...]

   # If you see opencv_core320.dll (version 3.2.0), this is OLD!
   # Solution: Clean install to get 4.9.0
   rm -rf ~/.m2/repository/org/bytedeco
   mvn clean install
   ```

2. **Verify NativeLoader is called:**

   ```java
   // In Main.java, this MUST be first:
   if (!NativeLoader.loadOpenCV()) {
       Platform.exit();
       return;
   }
   ```

3. **Check logs:**

   ```bash
   # Application logs
   cat logs/icefx.log

   # Look for:
   # "✅ OpenCV loaded successfully" - Good!
   # "❌ CRITICAL: Failed to load" - Problem!
   ```

---

### Issue: "Camera not found" Error

**Solutions:**

1. **Check camera connection:** Ensure webcam is plugged in
2. **Close other apps:** Zoom, Skype, etc. may be using camera
3. **Camera permissions:** Grant camera access to Java
4. **Try different camera index:**
   ```java
   // In code, try camera 0, 1, 2...
   CameraService camera = new CameraService(0);  // Try 1, 2 if 0 fails
   ```

---

### Issue: Database Connection Failed

**Error:** "DB connection failed: Access denied"

**Solutions:**

```bash
# 1. Verify MySQL is running
sudo systemctl status mysql  # Linux
# or
brew services list  # macOS

# 2. Test connection
mysql -u root -p

# 3. Grant permissions
mysql -u root -p
GRANT ALL PRIVILEGES ON facial_attendance.* TO 'root'@'localhost';
FLUSH PRIVILEGES;

# 4. Check config.properties credentials
cat ~/.icefx/config.properties
```

---

### Issue: Maven Build Fails

**Error:** "Failed to execute goal..."

**Solutions:**

```bash
# 1. Update Maven
mvn -version
# If < 3.8, upgrade Maven

# 2. Clear Maven cache
rm -rf ~/.m2/repository

# 3. Force update
mvn clean install -U

# 4. Check Java version
java -version  # Must be 17+

# 5. Set JAVA_HOME
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
```

---

### Issue: JavaFX Runtime Components Missing

**Error:** "Error: JavaFX runtime components are missing"

**Solution:**

```bash
# Use Maven JavaFX plugin (handles JavaFX automatically)
mvn javafx:run

# Or set module path manually:
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/IceFX-1.0.0.jar
```

---

## 📊 Verify Installation

### Quick Health Check

```bash
# 1. Build succeeds
mvn clean install
# Look for: "BUILD SUCCESS"

# 2. Run application
mvn javafx:run
# Look for in console:
#   "✅ OpenCV loaded successfully!"
#   "✅ Application started successfully"

# 3. Check logs
cat logs/icefx.log | grep "✅"
# Should show successful initializations

# 4. Test camera
# - Click "Start Camera" in UI
# - Webcam should activate
# - Video feed should display
```

---

## 🌐 Platform-Specific Notes

### Windows

```batch
REM Use Command Prompt or PowerShell
mvn clean install
mvn javafx:run

REM Native libs location:
REM %USERPROFILE%\.m2\repository\org\bytedeco\opencv\
```

### Linux (Ubuntu/Debian)

```bash
# Install prerequisites
sudo apt-get update
sudo apt-get install openjdk-17-jdk maven mysql-server

# Install v4l2 (camera support)
sudo apt-get install v4l-utils

# Run application
mvn javafx:run
```

### macOS

```bash
# Install prerequisites (Homebrew)
brew install openjdk@17 maven mysql

# Set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home

# Run application
mvn javafx:run
```

---

## 📦 Creating Distributable Package

### Option 1: Fat JAR with Dependencies

```bash
# Build fat JAR (includes all dependencies except JavaFX)
mvn clean package

# JAR location:
ls -lh target/IceFX-1.0.0.jar

# Run fat JAR:
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.swing \
     -jar target/IceFX-1.0.0.jar
```

### Option 2: jpackage Installer (Java 14+)

```bash
# Create native installer
jpackage --input target \
  --name IceFX \
  --main-jar IceFX-1.0.0.jar \
  --main-class application.Main \
  --type exe  # Windows
  # --type dmg  # macOS
  # --type deb  # Linux

# Creates:
# Windows: IceFX-1.0.0.exe
# macOS: IceFX-1.0.0.dmg
# Linux: IceFX-1.0.0.deb
```

---

## 🔍 Logging and Debugging

### Enable Debug Logging

Edit `src/main/resources/logback.xml`:

```xml
<!-- Change level to DEBUG for detailed logs -->
<logger name="application" level="DEBUG" />
<logger name="application.service" level="DEBUG" />
```

### View Logs

```bash
# Tail logs in real-time
tail -f logs/icefx.log

# Search for errors
grep "ERROR" logs/icefx.log

# Search for OpenCV loading
grep "OpenCV" logs/icefx.log
```

### Common Log Messages

**✅ Success Messages:**

```
✅ OpenCV loaded successfully!
✅ Camera opened successfully
✅ Recognized: John Doe (confidence: 85.3)
✅ Attendance logged successfully
```

**❌ Error Messages:**

```
❌ CRITICAL: Failed to load OpenCV native libraries
❌ Failed to open camera 0
❌ Recognition failed: No face detected
```

---

## 🧪 Testing

### Run Unit Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Manual Testing Checklist

- [ ] Application starts without crashes
- [ ] Camera activates and shows video
- [ ] Face detection works (green rectangle around face)
- [ ] Face recognition identifies users
- [ ] Attendance logging to database works
- [ ] Application closes cleanly

---

## 📞 Getting Help

### Check Documentation

```bash
# Read documentation
ls docs/
# - 00_INDEX.md - Start here!
# - 01_CRASH_ANALYSIS.md - Fix crashes
# - 02_SOURCE_CODE_AUDIT.md - Code overview
# - 03_IMPLEMENTATION_PLAN.md - Development roadmap
# - 04_QA_TEST_CHECKLIST.md - Testing guide
```

### Debug Output

```bash
# Run with debug output
mvn javafx:run -X

# Check system info
java -version
mvn -version
echo $JAVA_HOME
```

### Report Issues

Include this information:

1. OS and version
2. Java version (`java -version`)
3. Maven version (`mvn -version`)
4. Error message or stack trace
5. Relevant log snippet from `logs/icefx.log`
6. Steps to reproduce

---

## ✅ Success Criteria

**You know it's working when:**

1. ✅ `mvn clean install` completes with "BUILD SUCCESS"
2. ✅ `mvn javafx:run` starts application
3. ✅ Console shows "✅ OpenCV loaded successfully!"
4. ✅ Camera starts and shows video feed
5. ✅ No `hs_err_pid*.log` crash files generated
6. ✅ Application runs for 5+ minutes without issues

**Ready for production when:**

1. ✅ All tests pass
2. ✅ No JVM crashes for 60+ minutes continuous operation
3. ✅ Face recognition accuracy > 90%
4. ✅ Database queries < 500ms
5. ✅ Memory usage < 500MB
6. ✅ Works on Windows and Linux

---

**Last Updated:** November 11, 2025  
**Version:** 2.0  
**Status:** Stable with critical fixes implemented
