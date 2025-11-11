# Quick Start - Database & First User Setup

## 1️⃣ Create Database (MySQL/phpMyAdmin)

### Using phpMyAdmin (Easiest)

1. **Open** `http://localhost/phpmyadmin`
2. **Click** "New" or "Databases" tab
3. **Name:** `facial_attendance`
4. **Collation:** `utf8mb4_general_ci`
5. **Click** "Create"

### Import Tables

1. **Select** `facial_attendance` database
2. **Click** "Import" tab
3. **Choose file:** `database_setup.sql` from your IceFX folder
4. **Click** "Import"
5. **Wait for** "Import has been successfully finished"

---

## 2️⃣ Create First Admin User

### Option A: Use Pre-configured Admin (Easiest)

The `database_setup.sql` already includes a default admin:

- **User Code:** `ADMIN001`
- **Password:** `admin` ⚠️ **CHANGE THIS IMMEDIATELY!**

Just login and change the password in Admin Panel.

### Option B: Create Your Own Admin

Open phpMyAdmin SQL tab and run:

```sql
-- Replace 'YOUR_USERNAME' and generate BCrypt hash for your password
INSERT INTO persons (person_code, full_name, department, position, role, password, active)
VALUES (
  'ADM001',                    -- Your admin code
  'Your Name',                 -- Your full name
  'IT Department',             -- Department
  'Administrator',             -- Position
  'ADMIN',                     -- Must be ADMIN role
  '$2a$10$N9qo8uLOickgx2ZMRZoMye0EkIVXfkN5fzQDQhNLVGdOpyFZ7Hxnu',  -- BCrypt hash
  TRUE                         -- Active account
);
```

**Password Hashes (for testing - change in production!):**

- `admin123` → `$2a$10$N9qo8uLOickgx2ZMRZoMye0EkIVXfkN5fzQDQhNLVGdOpyFZ7Hxnu`
- `password` → `$2a$10$rqQkFGJbHpHvW8Cg5kCJ8.KH3YZ4lKp6d7Cg5kCJ8.KH3YZ4lKp6d`

Generate your own at: https://bcrypt-generator.com (use 10 rounds)

---

## 3️⃣ Configure Application

### First Run

```bash
cd /home/josh/IceFX
mvn javafx:run
```

This creates: `~/.icefx/config.properties`

### Edit Config

```bash
nano ~/.icefx/config.properties
```

**Update these values:**

```properties
db.type=mysql
db.host=localhost
db.port=3306
db.name=facial_attendance
db.username=root
db.password=YOUR_MYSQL_PASSWORD_HERE
```

**Save** and **Exit** (Ctrl+O, Enter, Ctrl+X)

---

## 4️⃣ Login & Add Users

### Login to Application

```bash
mvn javafx:run
```

**Credentials:**

- User Code: `ADMIN001` (or `ADM001` if you created your own)
- Password: `admin` (or whatever you set)

### Add New Users (Admin Panel)

1. Application opens → **Admin Panel** (automatic for ADMIN role)
2. **Fill in User Form:**
   - User Code: e.g., `STU001`
   - Full Name: e.g., `John Doe`
   - Email/Department: Optional
   - Role: Select `STUDENT`, `STAFF`, or `ADMIN`
   - Password: User's login password
   - Status: `Active`
3. **Click** "Add User"

### User Roles

| Role        | What They Can Do                                                                             |
| ----------- | -------------------------------------------------------------------------------------------- |
| **ADMIN**   | • Everything<br>• Manage users<br>• Train face models<br>• View all data<br>• Export reports |
| **STAFF**   | • View attendance<br>• Use face recognition<br>• Export own data                             |
| **STUDENT** | • View own attendance<br>• Check-in/check-out                                                |

---

## 5️⃣ Verify Everything Works

### Check Database

```sql
-- In phpMyAdmin SQL tab:
SELECT person_code, full_name, role, active
FROM persons
WHERE active = TRUE;
```

You should see your admin and any users you added.

### Check Application Logs

Look for these messages in terminal:

```
✅ Configuration initialized successfully
✅ Database connection established
✅ UserService initialized successfully
✅ Login successful for user: ADMIN001 (Role: Administrator)
```

---

## 🚨 Troubleshooting

### "Access denied for user 'root'@'localhost'"

**Fix:** Wrong MySQL password

```bash
# Edit config file
nano ~/.icefx/config.properties

# Update:
db.password=YOUR_CORRECT_PASSWORD
```

### "Unknown database 'facial_attendance'"

**Fix:** Database not created

1. Go to phpMyAdmin
2. Create database `facial_attendance`
3. Import `database_setup.sql`

### "Login failed"

**Fix:** Check user exists and password is correct

```sql
-- In phpMyAdmin:
SELECT person_code, full_name, role, active, password
FROM persons
WHERE person_code = 'ADMIN001';
```

Verify:

- ✅ User exists
- ✅ `active = 1`
- ✅ `password` starts with `$2a$10$` (BCrypt hash)

### "Application won't start"

**Fix:** Check Java version

```bash
java -version
# Should show: java version "23.0.1" or higher
```

If not Java 23:

```bash
# Install JDK 23.0.1
sudo apt update
sudo apt install openjdk-23-jdk
```

---

## ⚡ Quick Commands Reference

```bash
# Run application
mvn javafx:run

# Compile only
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn package

# Clean build
mvn clean

# View code coverage report
firefox target/site/jacoco/index.html
```

---

## 📋 Checklist

Before first use, ensure:

- [ ] MySQL is running (`sudo systemctl status mysql`)
- [ ] phpMyAdmin is accessible (`http://localhost/phpmyadmin`)
- [ ] Database `facial_attendance` exists
- [ ] Tables imported (users, attendance_logs, etc.)
- [ ] Admin user created and active
- [ ] Config file has correct MySQL password
- [ ] JDK 23.0.1+ is installed
- [ ] Application compiles (`mvn clean compile`)

---

## 🎯 Next Steps

After successful login:

1. **Add users** via Admin Panel
2. **Change default admin password** immediately
3. **Train face recognition model** (Admin Panel → Train Model)
4. **Test check-in/check-out** with face recognition
5. **Export attendance data** to CSV

---

## 🆘 Still Having Issues?

1. **Check logs:** Terminal output when running `mvn javafx:run`
2. **Database:** phpMyAdmin → facial_attendance → Browse tables
3. **Config:** `cat ~/.icefx/config.properties`
4. **Docs:** See `docs/DATABASE_SETUP_GUIDE.md` for detailed help

---

**Ready to go!** 🚀

Run: `mvn javafx:run` and login with your admin credentials.
