# Authorization Implementation Guide

## Overview

This document describes the role-based authorization system implemented in IceFX Attendance System v2.0.

## Components

### AuthorizationManager

**Location:** `src/main/java/com/icefx/util/AuthorizationManager.java`

A utility class providing role-based access control throughout the application.

#### Key Features

- **Role Checking:** Simple boolean checks for user roles
- **Permission Enforcement:** Methods that show error dialogs when unauthorized
- **Centralized Security:** Single point of authorization logic
- **Session Integration:** Works seamlessly with SessionManager

#### Available Methods

##### Basic Role Checks

```java
// Check if user has specific role
boolean isAdmin = AuthorizationManager.isAdmin();
boolean isStaff = AuthorizationManager.isStaff();
boolean isStudent = AuthorizationManager.isStudent();
boolean isAdminOrStaff = AuthorizationManager.isAdminOrStaff();

// Check any role
boolean hasRole = AuthorizationManager.hasRole(User.UserRole.ADMIN);
boolean hasAnyRole = AuthorizationManager.hasAnyRole(
    User.UserRole.ADMIN,
    User.UserRole.STAFF
);
```

##### Permission Enforcement (with User Dialog)

```java
// Require specific role - shows error dialog if unauthorized
if (!AuthorizationManager.requireAdmin("Delete User")) {
    return; // Operation blocked
}

if (!AuthorizationManager.requireAdminOrStaff("View Reports")) {
    return; // Operation blocked
}

// Require custom role
if (!AuthorizationManager.requireRole(User.UserRole.STAFF, "Export Data")) {
    return; // Operation blocked
}
```

##### Utility Methods

```java
// Get current user's role
User.UserRole role = AuthorizationManager.getCurrentRole();

// Check login status
boolean loggedIn = AuthorizationManager.isLoggedIn();
```

## Implementation in Controllers

### AdminController

**Protected Operations:**

- Panel access (initialize method)
- User updates (handleUpdate)
- User deletion (handleDelete)
- Model training (handleTrainModel)

**Example:**

```java
@FXML
public void initialize() {
    logger.info("Initializing AdminController");

    // AUTHORIZATION CHECK - Admin panel requires ADMIN role
    if (!AuthorizationManager.requireAdmin("Access Admin Panel")) {
        SessionManager.getCurrentUser().ifPresent(user ->
            logger.warn("Unauthorized access attempt by user: {}", user)
        );
        // Close the admin window
        Platform.runLater(() -> {
            if (userTable != null && userTable.getScene() != null) {
                userTable.getScene().getWindow().hide();
            }
        });
        return;
    }

    // ... rest of initialization
}

@FXML
private void handleUpdate() {
    // AUTHORIZATION CHECK
    if (!AuthorizationManager.requireAdmin("Update User")) {
        return;
    }

    // ... rest of update logic
}
```

### LoginController

**Role-Based Routing:**
Already implements role-based navigation after successful authentication:

```java
switch (user.getRole()) {
    case ADMIN -> {
        fxmlPath = "/com/icefx/view/AdminPanel.fxml";
        windowTitle = "IceFX - Admin Panel";
    }
    case STAFF -> {
        fxmlPath = "/com/icefx/view/Dashboard.fxml";
        windowTitle = "IceFX - Staff Dashboard";
    }
    case STUDENT -> {
        fxmlPath = "/com/icefx/view/Dashboard.fxml";
        windowTitle = "IceFX - Student Dashboard";
    }
}
```

### DashboardController

Currently accessible to all authenticated users. Future enhancements can add feature-level authorization:

```java
@FXML
private void handleExportData() {
    // Only Admin and Staff can export
    if (!AuthorizationManager.requireAdminOrStaff("Export Attendance Data")) {
        return;
    }

    // ... export logic
}
```

## User Roles

The system supports three roles defined in `User.UserRole`:

| Role        | Access Level    | Permissions                                                              |
| ----------- | --------------- | ------------------------------------------------------------------------ |
| **ADMIN**   | Full Access     | All operations including user management, training, system configuration |
| **STAFF**   | Moderate Access | View dashboards, attendance logs, export data (future)                   |
| **STUDENT** | Limited Access  | View own attendance records only                                         |

## Security Flow

1. **Login**

   - User authenticates via LoginController
   - SessionManager stores authenticated user
   - User redirected to role-appropriate view

2. **Navigation**

   - LoginController routes to correct view based on role
   - AdminPanel only accessible to ADMIN users
   - Dashboard accessible to all authenticated users

3. **Operation Authorization**

   - Controller methods check authorization before execution
   - AuthorizationManager verifies current user role
   - Unauthorized attempts logged and blocked with user-friendly dialog

4. **Logout**
   - SessionManager.clear() removes user session
   - Redirects to login screen

## Error Handling

When unauthorized access is attempted:

1. **User Notification:** Dialog shows:

   - Operation name
   - Required role
   - User's current role
   - Clear "Access Denied" message

2. **Logging:** Detailed log entry with:

   - User attempting access
   - Operation attempted
   - Timestamp

3. **Action Blocked:** Operation does not execute

## Adding Authorization to New Features

### Step 1: Identify Required Role

Determine which role(s) should access the feature:

- Admin only: `requireAdmin()`
- Admin or Staff: `requireAdminOrStaff()`
- Specific role: `requireRole(User.UserRole.STAFF, ...)`

### Step 2: Add Check to Method

```java
@FXML
private void handleSensitiveOperation() {
    // Add authorization check at method start
    if (!AuthorizationManager.requireAdmin("Sensitive Operation")) {
        return;
    }

    // ... rest of implementation
}
```

### Step 3: Test Authorization

1. Login as different roles
2. Attempt to access the feature
3. Verify:
   - Authorized users can proceed
   - Unauthorized users see error dialog
   - Attempt is logged

## Best Practices

### ✅ DO:

- Add authorization checks at the start of sensitive methods
- Use descriptive operation names in error messages
- Log unauthorized access attempts
- Check authorization in both UI and service layers (defense in depth)
- Use appropriate role check methods (requireAdmin vs requireAdminOrStaff)

### ❌ DON'T:

- Skip authorization checks assuming UI hides buttons
- Use generic error messages
- Allow silent failures
- Check authorization only in one layer
- Hard-code role names as strings

## Testing Authorization

### Manual Testing

```java
// Test file: AuthorizationTest.java (manual)
public class AuthorizationTest {

    @Test
    public void testAdminAccess() {
        // Setup: Login as Admin
        User admin = new User(1, "ADM001", "Admin User",
            null, null, User.UserRole.ADMIN, null, null, true);
        SessionManager.startSession(admin);

        // Test: Should allow admin operations
        assertTrue(AuthorizationManager.requireAdmin("Test Operation"));

        // Cleanup
        SessionManager.clear();
    }

    @Test
    public void testStudentDenied() {
        // Setup: Login as Student
        User student = new User(2, "STU001", "Student User",
            null, null, User.UserRole.STUDENT, null, null, true);
        SessionManager.startSession(student);

        // Test: Should block admin operations
        assertFalse(AuthorizationManager.requireAdmin("Test Operation"));

        // Cleanup
        SessionManager.clear();
    }
}
```

## Future Enhancements

### Planned Features

1. **Fine-grained Permissions:** Beyond role-based to permission-based
2. **Activity Logging:** Dedicated audit table for all authorization events
3. **Dynamic Role Assignment:** Change user roles without database edit
4. **Session Timeouts:** Auto-logout after inactivity period
5. **Multi-factor Authentication:** Enhanced security for admin accounts

### Potential Improvements

- Annotation-based authorization (`@RequiresRole(ADMIN)`)
- Programmatic role hierarchy (Admin > Staff > Student)
- Custom permission objects for complex access rules
- Integration with external authentication systems (LDAP, OAuth)

## Related Documentation

- [User Management Guide](USER_MANAGEMENT.md) (to be created)
- [Session Management](../src/main/java/com/icefx/util/SessionManager.java)
- [User Model](../src/main/java/com/icefx/model/User.java)

---

**Version:** 2.0  
**Last Updated:** 2025-11-11  
**Author:** IceFX Team
