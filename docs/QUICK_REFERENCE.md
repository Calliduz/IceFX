# IceFX Quick Reference Guide

## 🚀 Common Commands

### Development

```bash
# Clean and compile
mvn clean compile

# Run application
mvn javafx:run

# Run tests
mvn test

# Package JAR
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

### Database

```bash
# Import database schema
mysql -u root -p icefx_db < database_setup_simple.sql

# Backup database
mysqldump -u root -p icefx_db > backup_$(date +%Y%m%d).sql

# Connect to database
mysql -u root -p icefx_db
```

### Git Operations

```bash
# Check status
git status

# Add all changes
git add .

# Commit changes
git commit -m "Your message"

# Push to repository
git push origin main

# Create new branch
git checkout -b feature/your-feature
```

---

## 🎨 CSS Classes Reference

### Layout Utilities

```css
.m-0 to .m-5          /* Margin: 0, 4px, 8px, 12px, 16px, 24px */
.gap-1 to .gap-5      /* Gap: 4px, 8px, 12px, 16px, 24px */
.text-center          /* Center align */
.text-left            /* Left align */
.text-right; /* Right align */
```

### Components

```css
.button               /* Standard button */
/* Standard button */
.button-primary       /* Blue button */
.button-success       /* Green button */
.button-danger        /* Red button */
.button-warning       /* Orange button */
.button-info          /* Light blue button */

.card                 /* White card with shadow */
.card-elevated        /* Card with more shadow */

.stat-card-success    /* Green gradient card */
.stat-card-info       /* Blue gradient card */
.stat-card-warning    /* Orange gradient card */
.stat-card-error; /* Red gradient card */
```

### Messages

```css
.success-message      /* Green success message */
/* Green success message */
.error-message        /* Red error message */
.warning-message      /* Orange warning message */
.info-message; /* Blue info message */
```

### Shadows

```css
.shadow-sm            /* Small shadow */
/* Small shadow */
.shadow               /* Standard shadow */
.shadow-md            /* Medium shadow */
.shadow-lg; /* Large shadow */
```

---

## 📋 Configuration

### Config File Location

```
~/.icefx/config.properties
```

### Key Settings

```properties
# Database
db.url=jdbc:mysql://localhost:3306/icefx_db
db.username=root
db.password=yourpassword

# Camera (0 = default, 1 = external)
camera.index=0
camera.fps=30

# Recognition
recognition.threshold=80.0
recognition.model.path=models/trained_model.xml
```

---

## 👤 Default Credentials

```
Primary Admin:
  Username: ADMIN001
  Password: admin123

Secondary Admin:
  Username: ADM001
  Password: admin
```

---

## 🎯 Keyboard Shortcuts

### Login Screen

- `Enter` - Submit login
- `Esc` - Cancel/Exit

### Dashboard

- `F5` - Refresh attendance
- `Ctrl+C` - Stop camera
- `Ctrl+S` - Start camera

### Admin Panel

- `Ctrl+N` - New user
- `Ctrl+S` - Save/Update
- `Del` - Delete selected
- `Ctrl+F` - Focus search

---

## 🐛 Troubleshooting

### Camera Not Working

```bash
# Check available cameras (Linux)
ls -la /dev/video*

# Test camera with GStreamer
gst-launch-1.0 v4l2src ! autovideosink

# Change camera index in config
camera.index=1  # Try different numbers
```

### Database Connection Failed

```bash
# Check MySQL is running
sudo systemctl status mysql

# Start MySQL
sudo systemctl start mysql

# Check connection
mysql -u root -p -h localhost
```

### Face Recognition Low Accuracy

```
Solutions:
1. Train with more images (minimum 20 per person)
2. Ensure good lighting
3. Lower recognition threshold (but not below 60)
4. Retrain model with better quality images
```

### Application Won't Start

```bash
# Check Java version
java -version  # Should be 23.0.1

# Clean and rebuild
mvn clean compile

# Check logs
tail -f logs/icefx.log
```

---

## 📁 Important Files

```
src/main/java/com/icefx/
├── IceFXApplication.java          # Main entry point
├── controller/
│   ├── LoginController.java       # Login logic
│   ├── DashboardController.java   # Dashboard logic
│   └── AdminController.java       # Admin panel logic
├── service/
│   ├── FaceRecognitionService.java  # Face detection
│   ├── CameraService.java           # Camera handling
│   └── AttendanceService.java       # Attendance logic
└── util/
    └── ModernToast.java            # Notification system

src/main/resources/com/icefx/
├── view/
│   ├── Login.fxml                 # Login layout
│   ├── Dashboard.fxml             # Dashboard layout
│   └── AdminPanel.fxml            # Admin layout
└── styles/
    └── modern-light.css           # Modern theme
```

---

## 🔧 Common Tasks

### Add New User

1. Open Admin Panel
2. Fill in user details
3. Click "Add User"
4. Train face model with user images

### Train Face Model

1. Organize images: `faces/user_id/image1.jpg`
2. Admin Panel → "Train Model"
3. Select directory containing face folders
4. Wait for training
5. Save model

### Export Attendance

1. Dashboard → Select date range
2. Right-click table → "Export"
3. Choose format (CSV/PDF)
4. Select destination

### Change Theme

1. Edit `~/.icefx/config.properties`
2. Set `theme=light` or `theme=dark`
3. Restart application

---

## 🎓 Code Examples

### Using ModernToast

```java
// Success notification
ModernToast.success("User added successfully!");

// Error notification
ModernToast.error("Failed to connect to database");

// Warning notification
ModernToast.warning("Camera already in use");

// Info notification
ModernToast.info("Loading data...");
```

### Database Query

```java
try (Connection conn = DatabaseConfig.getConnection();
     PreparedStatement stmt = conn.prepareStatement(
         "SELECT * FROM users WHERE user_code = ?")) {

    stmt.setString(1, userCode);
    ResultSet rs = stmt.executeQuery();

    if (rs.next()) {
        // Process result
    }
} catch (SQLException e) {
    logger.error("Query failed", e);
}
```

### Camera Service Usage

```java
CameraService camera = new CameraService(0, 30);
camera.start();

// Set frame callback
camera.setFrameCallback(frame -> {
    // Process frame
});

// Stop camera
camera.stop();
```

---

## 📊 Performance Tips

### Database

- Use connection pooling (HikariCP)
- Index frequently queried columns
- Use prepared statements
- Close resources in try-with-resources

### UI

- Use CSS for styling (not inline)
- Minimize nested layouts
- Use Platform.runLater() for UI updates
- Cache images and resources

### Face Recognition

- Adjust camera FPS based on CPU
- Use appropriate image size
- Train with consistent lighting
- Regularly retrain model

---

## 🔗 Useful Links

- JavaFX Documentation: https://openjfx.io/
- OpenCV Docs: https://docs.opencv.org/
- Maven Guide: https://maven.apache.org/guides/
- Material Design: https://material.io/design

---

## 📞 Get Help

1. Check README.md
2. Review PROJECT_STRUCTURE.md
3. Check logs: `logs/icefx.log`
4. Search GitHub Issues
5. Create new issue with:
   - Error message
   - Steps to reproduce
   - System info (OS, Java version)
   - Log excerpt

---

**Last Updated:** November 11, 2025  
**Version:** 3.0
