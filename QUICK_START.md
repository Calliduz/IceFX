# IceFX Quick Start Guide

This guide walks you through running the modernized IceFX attendance system with JDK 23.

---

## 1. Clone and Build
```bash
git clone https://github.com/Calliduz/IceFX.git
cd IceFX
mvn clean compile
```

Run the application with:
```bash
mvn javafx:run
```

Package an executable JAR with:
```bash
mvn package
java -jar target/IceFX-1.0.0.jar
```

---

## 2. Configure the Database
IceFX supports MySQL or SQLite. The application creates `~/.icefx/config.properties` on the first launch.

### MySQL
1. Create the schema:
   ```bash
   mysql -u root -p
   ```
   ```sql
   CREATE DATABASE facial_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   SOURCE facial_attendance\ May\ 18\ 2025,\ 3.45AM.sql;
   ```
2. Update these keys in `~/.icefx/config.properties`:
   ```properties
   db.type=mysql
   db.mysql.host=localhost
   db.mysql.port=3306
   db.mysql.database=facial_attendance
   db.mysql.username=root
   db.mysql.password=secret
   ```

### SQLite (portable)
1. Opt-in via configuration:
   ```properties
   db.type=sqlite
   db.sqlite.path=data/facial_attendance.db
   ```
2. Ensure the `data/` directory is writable. The file is created automatically on first run.

---

## 3. Prepare Face Data
1. Capture 5–10 clear photos per user.
2. Store them under `faces/<userId>/face_<timestamp>.png`.
3. Launch the app, log in as an Admin, and use the **Train Model** action to build the LBPH model.
4. Save the model to the path configured by `recognition.model.path` (default `resources/trained_faces.xml`).

---

## 4. Login Roles
| Role | Default Capabilities |
|------|----------------------|
| Admin | User management, face training, reports |
| Staff | Attendance dashboard, manual overrides |
| Student | Self check-in and history view |

Use the Admin panel to manage accounts, reset passwords, and assign roles.

---

## 5. Test Checklist
- [ ] Application launches without errors (`mvn javafx:run`).
- [ ] Login succeeds with known credentials.
- [ ] Camera starts and shows live preview.
- [ ] Recognized users are logged in attendance (`attendance_logs`).
- [ ] Admin panel CRUD actions work (add/update/deactivate/delete).
- [ ] CSV export and reports run without exceptions.

Run the automated tests with:
```bash
mvn test
```

---

## 6. Troubleshooting
- **Camera missing**: adjust `camera.index` in the config, verify `/dev/video*` on Linux, or test with another webcam.
- **OpenCV load failure**: run `mvn clean install`, clear the BYTEDeco cache (`rm -rf ~/.m2/repository/org/bytedeco`), ensure the JDK architecture matches the OS.
- **DB connection errors**: test credentials manually (`mysql -u user -p`), confirm network access, or switch to SQLite for offline use.
- **Slow recognition**: reduce resolution (e.g., 320x240), ensure good lighting, retrain the model, or tune `recognition.confidence.threshold`.

For deeper context, see `README.md` and the inline JavaDocs across controllers, services, and DAO classes.
