# Database Setup Guide - IceFX Attendance System

## Quick Setup for MySQL with phpMyAdmin

### Step 1: Open phpMyAdmin

1. Open your web browser
2. Navigate to: `http://localhost/phpmyadmin`
3. Login with your MySQL credentials (default: username `root`, password: usually blank or `root`)

### Step 2: Create the Database

1. Click on "**New**" in the left sidebar or "**Databases**" tab
2. Database name: `facial_attendance`
3. Collation: `utf8mb4_general_ci` (recommended)
4. Click "**Create**"

### Step 3: Import the Database Schema

**Option A: Using the SQL File (Recommended)**

1. Select the `facial_attendance` database from the left sidebar
2. Click the "**Import**" tab at the top
3. Click "**Choose File**"
4. Navigate to your IceFX project folder
5. Select `database_setup.sql`
6. Scroll down and click "**Import**"
7. Wait for "Import has been successfully finished"

**Option B: Manual SQL Execution**

1. Select the `facial_attendance` database
2. Click the "**SQL**" tab
3. Copy the SQL from `database_setup.sql` file
4. Paste into the SQL textarea
5. Click "**Go**"

### Step 4: Verify Tables Created

After import, you should see these tables in the left sidebar:

- ✅ `users` - User accounts (admin, staff, students)
- ✅ `persons` - Person records for face recognition
- ✅ `attendance_logs` - Attendance check-in/check-out records

### Step 5: Create Initial Admin Account

Run this SQL in the SQL tab:

```sql
-- Create an admin user (password is 'admin123' - CHANGE THIS!)
INSERT INTO users (user_code, full_name, department, position, role, password, active)
VALUES (
  'ADM001',
  'Administrator',
  'IT Department',
  'System Administrator',
  'ADMIN',
  '$2a$10$YourBCryptHashHere',  -- You'll need to hash this properly
  1
);
```

**Important:** The password field must be BCrypt hashed. To create an admin account:

**Option 1: Use the Application (Easier)**

1. Run the application: `mvn javafx:run`
2. Since no users exist, you'll need to insert one user directly via SQL first
3. Or, temporarily modify `UserService.java` to allow first-time setup

**Option 2: Generate BCrypt Hash**
Use an online BCrypt generator:

- Password: `admin123`
- Rounds: 10
- Example hash: `$2a$10$N9qo8uLOickgx2ZMRZoMye0EkIVXfkN5fzQDQhNLVGdOpyFZ7Hxnu`

Then insert:

```sql
INSERT INTO users (user_code, full_name, department, position, role, password, active)
VALUES (
  'ADM001',
  'Administrator',
  'IT Department',
  'System Administrator',
  'ADMIN',
  '$2a$10$N9qo8uLOickgx2ZMRZoMye0EkIVXfkN5fzQDQhNLVGdOpyFZ7Hxnu',
  1
);
```

### Step 6: Configure Application Database Connection

1. Run the application once: `mvn javafx:run`
2. It will create config file at: `~/.icefx/config.properties` (Linux/Mac) or `C:\Users\YourName\.icefx\config.properties` (Windows)
3. Edit the file with your database credentials:

```properties
# Database Configuration
db.type=mysql
db.host=localhost
db.port=3306
db.name=facial_attendance
db.username=root
db.password=your_mysql_password_here

# HikariCP Pool Settings
db.pool.maximumPoolSize=10
db.pool.minimumIdle=2
db.pool.connectionTimeout=30000
```

### Step 7: Test the Connection

Run the application:

```bash
mvn javafx:run
```

You should see in the console:

```
✅ Configuration initialized successfully
✅ Database connection established
```

Login with:

- **User Code:** ADM001
- **Password:** admin123 (or whatever you set)

---

## User Registration / Signup

### ❌ No Registration Page Currently

The application **does NOT have a public registration/signup page** for security reasons. This is intentional for a facial attendance system.

### How to Add Users

**Option 1: Via Admin Panel (Recommended)**

1. Login as Admin (ADM001)
2. The application will open the **Admin Panel**
3. Use the User Management interface to:
   - Add new users
   - Assign roles (ADMIN, STAFF, STUDENT)
   - Set user codes and passwords
   - Activate/deactivate accounts

**Option 2: Via phpMyAdmin SQL**

```sql
-- Create a new student user
INSERT INTO users (user_code, full_name, department, position, role, password, active)
VALUES (
  'STU001',
  'John Doe',
  'Engineering',
  'Student',
  'STUDENT',
  '$2a$10$N9qo8uLOickgx2ZMRZoMye0EkIVXfkN5fzQDQhNLVGdOpyFZ7Hxnu',  -- 'student123'
  1
);

-- Create a staff member
INSERT INTO users (user_code, full_name, department, position, role, password, active)
VALUES (
  'STF001',
  'Jane Smith',
  'Human Resources',
  'HR Manager',
  'STAFF',
  '$2a$10$N9qo8uLOickgx2ZMRZoMye0EkIVXfkN5fzQDQhNLVGdOpyFZ7Hxnu',  -- 'staff123'
  1
);
```

### User Roles Explained

| Role        | Access Level    | Can Do                                                                                                             |
| ----------- | --------------- | ------------------------------------------------------------------------------------------------------------------ |
| **ADMIN**   | Full Access     | • Manage all users<br>• Train face recognition<br>• View all attendance<br>• Export data<br>• System configuration |
| **STAFF**   | Moderate Access | • View attendance dashboard<br>• View own attendance<br>• Use face recognition                                     |
| **STUDENT** | Limited Access  | • View own attendance<br>• Use face recognition<br>• Check-in/Check-out                                            |

---

## Database Backup & Restore

### Backup (Export)

1. Open phpMyAdmin
2. Select `facial_attendance` database
3. Click "**Export**" tab
4. Method: **Quick** (or Custom for advanced options)
5. Format: **SQL**
6. Click "**Export**"
7. Save the `.sql` file

### Restore (Import)

1. Drop the existing database (if needed):
   ```sql
   DROP DATABASE facial_attendance;
   CREATE DATABASE facial_attendance;
   ```
2. Select the database
3. Click "**Import**" tab
4. Choose your backup `.sql` file
5. Click "**Import**"

---

## Troubleshooting

### Problem: "Access denied for user 'root'@'localhost'"

**Solution:**

- Check your MySQL username and password
- Update `~/.icefx/config.properties`
- Restart the application

### Problem: "Unknown database 'facial_attendance'"

**Solution:**

- Database not created yet
- Follow Step 2 to create the database
- Re-run the import

### Problem: "Table 'users' doesn't exist"

**Solution:**

- SQL import failed or incomplete
- Re-run Step 3 to import `database_setup.sql`
- Check phpMyAdmin for error messages

### Problem: "Cannot login with admin credentials"

**Solution:**

- Check the password hash is correct BCrypt format
- Verify user exists: `SELECT * FROM users WHERE user_code = 'ADM001';`
- Ensure `active = 1`
- Check console logs for authentication errors

### Problem: "How do I change my password?"

**Solution:**

- Login as Admin
- Go to Admin Panel → User Management
- Select the user
- Enter new password
- Click "Update User"

**Or via SQL:**

```sql
-- First, generate a BCrypt hash of your new password
-- Then update:
UPDATE users
SET password = '$2a$10$YourNewBCryptHash'
WHERE user_code = 'ADM001';
```

---

## MySQL Command Line Alternative

If you prefer command line instead of phpMyAdmin:

### 1. Login to MySQL

```bash
mysql -u root -p
```

### 2. Create Database

```sql
CREATE DATABASE facial_attendance;
USE facial_attendance;
```

### 3. Import Schema

```bash
mysql -u root -p facial_attendance < database_setup.sql
```

### 4. Verify

```sql
SHOW TABLES;
SELECT * FROM users;
```

---

## Production Deployment Notes

For production use:

1. **Change Default Passwords** ⚠️

   - Never use 'admin123' in production
   - Use strong passwords (12+ characters, mixed case, numbers, symbols)

2. **Secure the Database**

   - Don't use 'root' user for the application
   - Create dedicated MySQL user:
     ```sql
     CREATE USER 'icefx_app'@'localhost' IDENTIFIED BY 'strong_password_here';
     GRANT ALL PRIVILEGES ON facial_attendance.* TO 'icefx_app'@'localhost';
     FLUSH PRIVILEGES;
     ```

3. **Regular Backups**

   - Schedule daily database backups
   - Store backups off-site
   - Test restore procedure regularly

4. **Monitor Performance**
   - Check connection pool usage
   - Monitor query performance
   - Index frequently queried columns

---

## Need Help?

- Check the logs: `logs/` directory in project folder
- Console output when running `mvn javafx:run`
- Database errors show in phpMyAdmin SQL tab
- Application logs show authentication and connection issues

---

**Last Updated:** November 11, 2025  
**IceFX Version:** 2.0  
**Database Schema Version:** 1.0
