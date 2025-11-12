# IceFX Quick Start Guide

## 🚀 Getting Started

### Compile & Run

```bash
# Compile
mvn clean compile

# Run application
mvn javafx:run
```

---

## 👥 User Flows

### For Students (Face Scan Only)

```
1. Launch IceFX
2. Click "👨‍🎓 I'm a Student"
3. Dashboard loads automatically
4. Camera starts scanning
5. Look at camera
6. ✅ Attendance logged + Schedule displayed
```

**No password needed!**

### For Admins (Credentials Required)

```
1. Launch IceFX
2. Click "👨‍💼 I'm an Admin"
3. Enter credentials:
   - Code: ADMIN001
   - Password: admin123
4. Access admin panel
```

---

## 📸 Face Registration

### Manual Mode (Click to Capture)

```
1. Admin Panel → Select user from table
2. Click "Register Faces" button
3. Camera starts automatically
4. Position for each angle
5. Click "📸 Capture Photo" (10 times)
6. Auto-trains and closes
```

### Auto Mode (Hands-Free)

```
1. Admin Panel → Select user
2. Click "Register Faces"
3. ✅ Enable "🤖 Auto-Capture Mode"
4. Position for angle 1
5. Wait 2 seconds → Auto-captures
6. Move to angle 2
7. Wait 2 seconds → Auto-captures
8. Repeat for all 10 angles
9. Auto-trains and closes
```

**Recommended Angles:**

1. Looking straight at camera
2. Head tilted slightly left (2/10)
3. Head tilted slightly right (2/10)
4. Head tilted slightly up (2/10)
5. Head tilted slightly down (2/10)
   6-10. Various expressions (smile, neutral, etc.)

---

## 🎯 Key Features

### Camera Mirroring

- ✅ Camera acts like a mirror
- Move left → See left on screen
- Intuitive, natural interaction

### Spam Prevention

- ✅ 1-second cooldown between manual captures
- Prevents skipping angles
- Ensures proper coverage

### Auto-Train & Close

- ✅ Automatically trains model at 10 photos
- Camera stops automatically
- Window closes after training
- Zero manual steps needed

### Auto-Capture Mode

- ✅ Toggle checkbox to enable
- Automatically captures every 2 seconds
- Hands-free operation
- Perfect for accessibility

### Duplicate Prevention

- ✅ 60-minute cooldown between logs
- Prevents spam attendance
- One entry per hour per student

---

## 🔧 Configuration

### Camera Settings

```properties
camera.index=0           # 0 = default camera
camera.fps=30            # Frames per second
camera.width=640         # Resolution width
camera.height=480        # Resolution height
```

### Face Recognition

```properties
recognition.confidence.threshold=80.0    # Minimum confidence
faces.directory=faces                    # Storage folder
```

### Registration

```properties
registration.minimum.photos=5           # Minimum required
registration.recommended.photos=10      # Recommended count
```

### Attendance

```properties
attendance.duplicate.cooldown=60        # Minutes between logs
```

---

## 📁 File Structure

```
IceFX/
├── faces/                  # Face images by user
│   ├── STU001/
│   ├── STU002/
│   └── ADMIN001/
├── trained_faces.xml       # Trained model
├── database/
│   └── icefx.db           # SQLite database
└── logs/
    └── application.log    # System logs
```

---

## 🐛 Troubleshooting

### Camera Not Starting

```
Problem: "Failed to open camera"
Solution:
  1. Check if another app is using camera
  2. Verify camera permissions
  3. Try different camera.index (0, 1, 2...)
  4. Restart application
```

### Face Not Recognized

```
Problem: "Unknown face"
Solution:
  1. Check if user is registered (faces/USER_ID/ has files)
  2. Verify model is trained (trained_faces.xml exists)
  3. Re-register with better lighting
  4. Ensure face is centered and well-lit
  5. Try manual training in admin panel
```

### Duplicate Attendance

```
Problem: "Already logged today"
Solution:
  This is normal! Cooldown prevents spam.
  Wait 60 minutes before next log.
```

### Registration Window Won't Close

```
Problem: Stuck on training
Solution:
  1. Check logs for errors
  2. Verify faces folder has images
  3. Close manually and try "Train Model" again
  4. Restart application if stuck
```

---

## 🎓 Demo Credentials

### Admin Access

```
Code: ADMIN001
Password: admin123
```

### Test Students (For Face Registration)

```
STU001 - Juztyne Clever
STU002 - Joshua Baguio
STU003 - Test Student
```

---

## 📊 System Requirements

### Minimum

- Java 17+
- OpenCV 4.5+
- 4GB RAM
- Webcam (720p)
- Linux/Windows/Mac

### Recommended

- Java 21
- OpenCV 4.9
- 8GB RAM
- HD Webcam (1080p)
- Good lighting

---

## 🔐 Security Notes

### Students

- ✅ No passwords stored
- ✅ Face-only authentication
- ✅ Cannot access admin functions

### Admins

- ✅ BCrypt password hashing
- ✅ Full system access
- ✅ Can register/modify users

### Privacy

- ✅ All data stored locally
- ✅ No cloud uploads
- ✅ GDPR-friendly on-premises
- ✅ Can delete user data

---

## 📞 Support

### Logs Location

```
logs/application.log
```

### Check Errors

```bash
# View last 50 lines
tail -n 50 logs/application.log

# Follow real-time
tail -f logs/application.log
```

### Reset System

```bash
# Delete all face data
rm -rf faces/*

# Delete trained model
rm trained_faces.xml

# Reset database (CAUTION!)
rm database/icefx.db
```

---

## ✅ Quick Checklist

### First Time Setup

- [ ] Compile project (`mvn clean compile`)
- [ ] Run application (`mvn javafx:run`)
- [ ] Test admin login (ADMIN001/admin123)
- [ ] Register at least one student face
- [ ] Test student face scan login
- [ ] Verify attendance logging
- [ ] Check schedule display

### Before Production

- [ ] Register all student faces
- [ ] Train model with all users
- [ ] Test in actual lighting conditions
- [ ] Configure camera settings
- [ ] Set up backup schedule
- [ ] Test duplicate prevention
- [ ] Verify attendance reports
- [ ] Document admin procedures

---

**Version**: IceFX 3.0  
**Last Updated**: November 12, 2025  
**Status**: ✅ Production Ready
