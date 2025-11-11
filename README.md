# IceFX Attendance System 🎯

> **Modern Face Recognition Attendance Management System**  
> Powered by JavaFX 23 | OpenCV 4.9 | JDK 23.0.1

[![Java](https://img.shields.io/badge/Java-23.0.1-orange.svg)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23.0.1-blue.svg)](https://openjfx.io/)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.9.0-green.svg)](https://opencv.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Overview

IceFX is a professional-grade facial recognition attendance management system built with modern Java technologies. It features a clean, intuitive white-themed UI, real-time face detection, and comprehensive user management capabilities.

### ✨ Key Features

- **🎥 Real-Time Face Detection** - Live camera feed with instant face recognition
- **👤 User Management** - Complete CRUD operations for user accounts
- **📊 Dashboard Analytics** - Real-time attendance statistics and insights
- **🎓 Model Training** - Built-in face recognition model training
- **🔐 Secure Authentication** - BCrypt password hashing
- **💾 Database Integration** - MySQL/MariaDB with HikariCP connection pooling
- **🎨 Modern UI** - Clean white design with Material Design principles
- **📱 Responsive Layout** - Adaptive interface for different screen sizes

---

## 🚀 Quick Start

### Prerequisites

- **JDK 23.0.1** or later
- **Maven 3.8+**
- **MySQL 8.0** or **MariaDB 10.4+**
- **Webcam** (for face recognition features)

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/IceFX.git
cd IceFX
```

#### 2. Setup Database

```bash
# Login to MySQL/MariaDB
mysql -u root -p

# Create database and import schema
CREATE DATABASE icefx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE icefx_db;
SOURCE database_setup_simple.sql;
```

#### 3. Configure Application

Edit `src/main/resources/application.properties` (created on first run):

```properties
# Database Configuration
db.url=jdbc:mysql://localhost:3306/icefx_db
db.username=root
db.password=yourpassword

# Camera Configuration
camera.index=0
camera.fps=30

# Face Recognition
recognition.threshold=80.0
recognition.model.path=models/trained_model.xml
```

#### 4. Build & Run

```bash
# Clean and compile
mvn clean compile

# Run application
mvn javafx:run
```

### Default Login Credentials

```
Username: ADMIN001
Password: admin123
```

---

## 📁 Project Structure

```
IceFX/
├── src/
│   ├── main/
│   │   ├── java/com/icefx/
│   │   │   ├── config/           # Configuration management
│   │   │   ├── controller/       # JavaFX controllers
│   │   │   ├── dao/              # Data access objects
│   │   │   ├── model/            # Data models
│   │   │   ├── service/          # Business logic
│   │   │   ├── util/             # Utility classes
│   │   │   └── IceFXApplication.java
│   │   └── resources/
│   │       ├── com/icefx/
│   │       │   ├── view/         # FXML layouts
│   │       │   ├── styles/       # CSS stylesheets
│   │       │   └── images/       # Application icons
│   │       ├── haar/             # OpenCV classifiers
│   │       └── logback.xml       # Logging configuration
│   └── test/                     # Unit tests
├── docs/                         # Documentation
├── database_setup_simple.sql    # Database schema
├── pom.xml                       # Maven configuration
└── README.md
```

---

## 🎨 UI Showcase

### Modern Clean Design
- **White-dominant theme** with subtle shadows and gradients
- **Material Design** inspired components
- **Smooth animations** and transitions
- **Responsive layouts** that adapt to screen size

### Screens
1. **Login** - Clean authentication with user code and password
2. **Dashboard** - Live camera feed with real-time face recognition
3. **Admin Panel** - Comprehensive user and model management

---

## 🛠️ Technology Stack

### Core Technologies
- **Java 23.0.1** - Latest LTS with modern language features
- **JavaFX 23.0.1** - Rich desktop UI framework
- **Maven 3.8+** - Dependency management and build tool

### Libraries & Frameworks
- **OpenCV 4.9.0** - Computer vision and face recognition
- **JavaCV 1.5.10** - Java interface to OpenCV
- **MySQL Connector 9.1.0** - Database connectivity
- **HikariCP 6.2.1** - High-performance connection pooling
- **BCrypt** - Secure password hashing
- **SLF4J + Logback** - Comprehensive logging
- **JUnit 5** - Testing framework

---

## 📊 Features in Detail

### Face Recognition System
- **Haar Cascade** classifiers for face detection
- **LBPH** (Local Binary Patterns Histograms) recognizer
- **Confidence threshold** filtering
- **Debounce logic** to prevent duplicate recognition
- **Model persistence** for trained data

### User Management
- **Role-based access** (Admin, User, Student)
- **Status management** (Active, Inactive, Suspended)
- **Secure password** hashing with BCrypt cost factor 10
- **Email validation** and user search
- **Bulk operations** support

### Attendance Logging
- **Automatic logging** on face recognition
- **Timestamp tracking** with timezone support
- **Confidence score** recording
- **Duplicate prevention** with configurable cooldown
- **Export capabilities** (CSV, PDF)

---

## 🔧 Configuration

### Application Config (`~/.icefx/config.properties`)

```properties
# Database
db.url=jdbc:mysql://localhost:3306/icefx_db
db.username=root
db.password=password

# Camera
camera.index=0
camera.fps=30
camera.width=640
camera.height=480

# Recognition
recognition.threshold=80.0
recognition.model.path=models/trained_model.xml
recognition.debounce.seconds=5

# UI
theme=light
```

### Logging Config (`src/main/resources/logback.xml`)

Customize logging levels, file outputs, and patterns.

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=UserServiceTest

# Generate coverage report
mvn clean test jacoco:report
```

---

## 📦 Building for Production

### Create Executable JAR

```bash
mvn clean package
```

Output: `target/IceFX-1.0.0.jar`

### Run JAR

```bash
java -jar target/IceFX-1.0.0.jar
```

### Platform-Specific Packages

```bash
# Windows Installer
mvn jpackage:jpackage -Pwindows

# macOS App
mvn jpackage:jpackage -Pmacos

# Linux Package
mvn jpackage:jpackage -Plinux
```

---

## 🐛 Troubleshooting

### Common Issues

#### OpenCV Not Loading
```
Solution: Ensure OpenCV native libraries are in java.library.path
```

#### Database Connection Failed
```
Solution: Check database credentials and ensure MySQL/MariaDB is running
```

#### Camera Not Detected
```
Solution: Verify camera index in config (try 0, 1, 2)
Check camera permissions on your OS
```

#### Face Recognition Low Accuracy
```
Solution: Train model with more images per person (minimum 10-20)
Ensure good lighting conditions
Adjust recognition threshold in config
```

---

## 📈 Roadmap

- [ ] Dark theme support
- [ ] Multi-language support (i18n)
- [ ] Cloud database integration
- [ ] Mobile app companion
- [ ] Attendance reports and analytics
- [ ] Email notifications
- [ ] REST API for integration
- [ ] Docker containerization

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc comments for public methods
- Keep methods focused and concise
- Write unit tests for new features

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **IceFX Team** - *Initial work*

---

## 🙏 Acknowledgments

- OpenCV community for excellent computer vision tools
- JavaFX team for the modern UI framework
- All contributors who helped improve this project

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/IceFX/issues)
- **Email**: support@icefx.com
- **Documentation**: [Wiki](https://github.com/yourusername/IceFX/wiki)

---

<div align="center">

**Made with ❤️ by IceFX Team**

⭐ Star us on GitHub if you find this project useful!

</div>
