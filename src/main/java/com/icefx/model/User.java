package com.icefx.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User entity representing a person in the system (Student or Staff).
 * Supports JavaFX property binding for UI components.
 */
public class User {
    private final IntegerProperty userId;
    private final StringProperty userCode;
    private final StringProperty fullName;
    private final StringProperty department;
    private final StringProperty position;
    private final ObjectProperty<UserRole> role;
    private final StringProperty password; // hashed password for login
    private final ObjectProperty<LocalDateTime> createdAt;
    private final BooleanProperty active;

    public enum UserRole {
        ADMIN("Administrator"),
        STAFF("Staff Member"),
        STUDENT("Student");

        private final String displayName;

        UserRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Full constructor
    public User(int userId, String userCode, String fullName, String department, 
                String position, UserRole role, String password, LocalDateTime createdAt, boolean active) {
        this.userId = new SimpleIntegerProperty(userId);
        this.userCode = new SimpleStringProperty(userCode);
        this.fullName = new SimpleStringProperty(fullName);
        this.department = new SimpleStringProperty(department);
        this.position = new SimpleStringProperty(position);
        this.role = new SimpleObjectProperty<>(role);
        this.password = new SimpleStringProperty(password);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
        this.active = new SimpleBooleanProperty(active);
    }

    // Constructor without password (for display purposes)
    public User(int userId, String userCode, String fullName, String department, String position) {
        this(userId, userCode, fullName, department, position, UserRole.STUDENT, null, LocalDateTime.now(), true);
    }

    // Getters
    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }
    
    public String getUserCode() { return userCode.get(); }
    public StringProperty userCodeProperty() { return userCode; }
    
    public String getFullName() { return fullName.get(); }
    public StringProperty fullNameProperty() { return fullName; }
    
    public String getDepartment() { return department.get(); }
    public StringProperty departmentProperty() { return department; }
    
    public String getPosition() { return position.get(); }
    public StringProperty positionProperty() { return position; }
    
    public UserRole getRole() { return role.get(); }
    public ObjectProperty<UserRole> roleProperty() { return role; }
    
    public String getPassword() { return password.get(); }
    public StringProperty passwordProperty() { return password; }
    
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
    
    public boolean isActive() { return active.get(); }
    public BooleanProperty activeProperty() { return active; }

    // Setters
    public void setUserId(int userId) { this.userId.set(userId); }
    public void setUserCode(String userCode) { this.userCode.set(userCode); }
    public void setFullName(String fullName) { this.fullName.set(fullName); }
    public void setDepartment(String department) { this.department.set(department); }
    public void setPosition(String position) { this.position.set(position); }
    public void setRole(UserRole role) { this.role.set(role); }
    public void setPassword(String password) { this.password.set(password); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public void setActive(boolean active) { this.active.set(active); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return getUserId() == user.getUserId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId());
    }

    @Override
    public String toString() {
        return fullName.get() + " (" + userCode.get() + ")";
    }
}
