
# IceFX – Facial Attendance System

This project is a JavaFX-based facial recognition attendance system using OpenCV + JavaCV.  
**Compatible with Apache NetBeans 15 and Java 11+**

---

## 🚀 How to Run (NetBeans 15)

1. **Open Project**:
   - File → Open Project → Select the `facial-attendance-main` folder.

2. **Build**:
   - Right-click the project → `Clean and Build`

3. **Run**:
   - Right-click the project → `Run`

Ensure NetBeans is configured with:
- **Java 11+ JDK**
- Maven enabled

---

## 🧰 Tech Stack

- JavaFX SDK 19+
- OpenCV 4.5.3 via JavaCV
- MySQL Connector 8.0.13
- Maven (standard layout)
- JDK 11

---

## 🛠️ Maven CLI (Alternative to NetBeans)

```bash
mvn clean install
mvn javafx:run
```

---

## ✅ Notes

- Main class: `application.Main`
- Source folder: `src/main/java`
- If you encounter JVM crashes, ensure OpenCV native libs match your OS architecture.

