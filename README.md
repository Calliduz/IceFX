# IceFX Facial Attendance System

![Version](https://img.shields.io/badge/version-2.0-blue) ![Java](https://img.shields.io/badge/Java-23.0.1-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-23.0.1-green) ![OpenCV](https://img.shields.io/badge/OpenCV-4.9.0-red) ![License](https://img.shields.io/badge/license-MIT-lightgrey)

IceFX is a production-ready facial recognition attendance application built on JavaFX and OpenCV. The latest refactor targets JDK 23.0.1, cleans the architecture, and modernizes the UI for a fast and reliable experience.

---

## Contents

- [Highlights](#highlights)
- [System Requirements](#system-requirements)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Building & Running](#building--running)
- [Project Layout](#project-layout)
- [Features](#features)
- [Face Model Training](#face-model-training)
- [Troubleshooting](#troubleshooting)
- [Testing](#testing)
- [Technology Stack](#technology-stack)
- [Contributing](#contributing)
- [License](#license)

---

## Highlights

- Clean layered architecture (controller / service / dao / model / util)
- Role-aware navigation (Admin, Staff, Student)
- Real-time camera capture with OpenCV 4.9 via JavaCV 1.5.10
- Attendance logging backed by MySQL (with optional SQLite portability)
- Modern JavaFX 23 UI with light and dark themes
- Centralized configuration using `~/.icefx/config.properties`
- Hardened native loading and graceful error handling

---

## System Requirements

| Software | Version                | Notes                             |
| -------- | ---------------------- | --------------------------------- |
| JDK      | 23.0.1+                | Oracle JDK or Temurin/OpenJDK     |
| Maven    | 3.9+                   | Used for dependency management    |
| MySQL    | 8.0+                   | Optional when using MySQL backend |
| Camera   | 720p USB or integrated | Required for recognition          |

Hardware: 4 GB RAM minimum (8 GB recommended) and 500 MB free disk.

---

## Quick Start

```bash
git clone https://github.com/Calliduz/IceFX.git
cd IceFX
mvn clean compile
mvn javafx:run
```

### Database Options

**MySQL (production)**

1. Create the database and load the schema:
   ```bash
   mysql -u root -p
   ```
   ```sql
   CREATE DATABASE facial_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   SOURCE facial_attendance\ May\ 18\ 2025,\ 3.45AM.sql;
   ```
2. Update the configuration file (see [Configuration](#configuration)).

**SQLite (portable)**

1. Ensure the `data` directory exists (created automatically on first run).
2. Switch to SQLite in the configuration file and point to a writable path, for example `data/facial_attendance.db`.

---

## Configuration

IceFX loads settings from `~/.icefx/config.properties`. The file is created on first launch with safe defaults.

Important keys:

```
# Application theme
app.theme=light            # or dark

# Database selection
db.type=mysql              # mysql or sqlite
# MySQL connection
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=facial_attendance
db.mysql.username=root
db.mysql.password=secret
# SQLite location (used when db.type=sqlite)
db.sqlite.path=data/facial_attendance.db

# Camera configuration
camera.index=0
camera.width=640
camera.height=480
camera.fps=30

# Recognition tuning
recognition.confidence.threshold=80.0
recognition.debounce.millis=3000
recognition.model.path=resources/trained_faces.xml
```

Change values and restart the application. The `AppConfig` utility handles reloads safely.

---

## Building & Running

```bash
# Full build
mvn clean install

# Run with JavaFX plugin
mvn javafx:run

# Package fat JAR with bundled dependencies
mvn package
java -jar target/IceFX-1.0.0.jar
```

Set extra JVM options with `-Djavafx.vmargs`, for example:

```bash
mvn javafx:run -Djavafx.vmargs="-Xmx2g -Dprism.vsync=false"
```

---

## Project Layout

```
src/main/java/com/icefx/
├── IceFXApplication.java      # JavaFX entry point
├── config/                    # AppConfig, DatabaseConfig
├── controller/                # JavaFX controllers
├── dao/                       # Database access objects
├── model/                     # Entities and observable models
├── service/                   # Business logic modules
└── util/                      # Native loader, helpers

src/main/resources/com/icefx/
├── view/                      # FXML layouts (Login, Dashboard, Admin)
└── styles/                    # Light and dark CSS themes

faces/                         # Training images (per-user subfolders)
native/                        # Optional manual native libraries
resources/trained_faces.xml    # Default recognition model (if present)
```

---

## Features

**Authentication & Roles**

- Secure login with BCrypt hashing
- Session management with role-based dashboards

**Recognition & Attendance**

- Live preview with FPS indicator
- LBPH recognition with configurable confidence threshold
- Automatic attendance logging with duplicate protection
- CSV export and date-range queries (via services/DAO)

**Administration**

- Manage users (create, update, deactivate, delete)
- Train, load, and save face recognition models
- Monitor user statistics and system status

**UI/UX**

- Modern Material-inspired layouts
- Theme-aware styling (light/dark)
- Responsive panes sized for 1366x768 and above
- Toasts, alerts, and progress indicators for feedback

---

## Face Model Training

1. Gather face samples per user under `faces/<userId>/face_<n>.png`.
2. Sign in as an Admin and open the Admin Panel.
3. Use the training controls to point to the `faces/` directory.
4. Train the LBPH model and save it to the path configured in `recognition.model.path`.
5. Distribute the updated model with the application or store it on shared storage.

Tips:

- Capture 5–10 well-lit photos per user.
- Mix angles and expressions for higher accuracy.
- Keep backgrounds uncluttered to reduce noise.

---

## Troubleshooting

**Camera not detected**

- Verify the device index (`camera.index`).
- On Linux, check `/dev/video*` and confirm permissions.

**OpenCV native load failure**

- Run `mvn clean install` to re-fetch natives.
- Clear the JavaCV cache: `rm -rf ~/.m2/repository/org/bytedeco`.
- Ensure the architecture (x86_64 vs arm64) matches your JDK.

**Database connection errors**

- Confirm credentials in `config.properties`.
- For MySQL, run `mysql -u user -p -h host` to test connectivity.
- For SQLite, ensure the path is writable and not on a read-only volume.

**Low recognition accuracy**

- Increase lighting and ensure faces occupy most of the frame.
- Train with updated samples and re-save the LBPH model.
- Adjust `recognition.confidence.threshold` (lower values accept more matches).

---

## Testing

```bash
mvn test
```

Add unit tests for services and DAO classes under `src/test/java`. Use the Maven Surefire plugin (already configured) to run them during CI.

---

## Technology Stack

| Layer           | Technology          | Version         |
| --------------- | ------------------- | --------------- |
| Language        | Java                | 23.0.1          |
| UI              | JavaFX              | 23.0.1          |
| CV              | OpenCV (via JavaCV) | 4.9.0 / 1.5.10  |
| DB              | MySQL / SQLite      | 9.1.0 / 3.47.1  |
| Connection Pool | HikariCP            | 6.2.1           |
| Logging         | SLF4J + Logback     | 2.0.16 / 1.5.12 |
| Build           | Maven               | 3.9+            |

---

## Contributing

1. Fork the repository and create a feature branch.
2. Run `mvn fmt:format` or your formatter of choice before committing.
3. Ensure `mvn clean test` passes.
4. Open a pull request describing the changes and testing performed.

Bug reports and feature requests are welcome via GitHub Issues.

---

## License

IceFX is released under the MIT License. Refer to the [LICENSE](LICENSE) file for details.
