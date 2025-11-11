package com.icefx.util;

import com.icefx.model.User;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authorization utility for role-based access control
 * 
 * @author IceFX Team
 * @version 2.0
 * @since JDK 23.0.1
 */
public class AuthorizationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationManager.class);
    
    /**
     * Check if current user has required role
     * 
     * @param requiredRole Required role for the operation
     * @return true if user has required role
     */
    public static boolean hasRole(User.UserRole requiredRole) {
        Optional<User> currentUser = SessionManager.getCurrentUser();
        if (currentUser.isEmpty()) {
            logger.warn("Authorization check failed: No user logged in");
            return false;
        }
        
        return currentUser.get().getRole() == requiredRole;
    }
    
    /**
     * Check if current user has any of the required roles
     * 
     * @param requiredRoles Required roles for the operation
     * @return true if user has any of the required roles
     */
    public static boolean hasAnyRole(User.UserRole... requiredRoles) {
        Optional<User> currentUser = SessionManager.getCurrentUser();
        if (currentUser.isEmpty()) {
            logger.warn("Authorization check failed: No user logged in");
            return false;
        }
        
        for (User.UserRole role : requiredRoles) {
            if (currentUser.get().getRole() == role) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if current user is Admin
     * 
     * @return true if user is Admin
     */
    public static boolean isAdmin() {
        return hasRole(User.UserRole.ADMIN);
    }
    
    /**
     * Check if current user is Staff
     * 
     * @return true if user is Staff
     */
    public static boolean isStaff() {
        return hasRole(User.UserRole.STAFF);
    }
    
    /**
     * Check if current user is Student
     * 
     * @return true if user is Student
     */
    public static boolean isStudent() {
        return hasRole(User.UserRole.STUDENT);
    }
    
    /**
     * Check if current user is Admin or Staff
     * 
     * @return true if user is Admin or Staff
     */
    public static boolean isAdminOrStaff() {
        return hasAnyRole(User.UserRole.ADMIN, User.UserRole.STAFF);
    }
    
    /**
     * Enforce authorization and show error if not authorized
     * 
     * @param requiredRole Required role
     * @param operationName Name of the operation for error message
     * @return true if authorized
     */
    public static boolean requireRole(User.UserRole requiredRole, String operationName) {
        if (!hasRole(requiredRole)) {
            showUnauthorizedDialog(requiredRole, operationName);
            SessionManager.getCurrentUser().ifPresent(user ->
                logger.warn("Unauthorized access attempt to: {} by user: {}", operationName, user)
            );
            return false;
        }
        return true;
    }
    
    /**
     * Enforce authorization for multiple roles and show error if not authorized
     * 
     * @param operationName Name of the operation for error message
     * @param requiredRoles Required roles
     * @return true if authorized
     */
    public static boolean requireAnyRole(String operationName, User.UserRole... requiredRoles) {
        if (!hasAnyRole(requiredRoles)) {
            showUnauthorizedDialog(null, operationName);
            SessionManager.getCurrentUser().ifPresent(user ->
                logger.warn("Unauthorized access attempt to: {} by user: {}", operationName, user)
            );
            return false;
        }
        return true;
    }
    
    /**
     * Require Admin role with error dialog
     * 
     * @param operationName Name of the operation
     * @return true if authorized
     */
    public static boolean requireAdmin(String operationName) {
        return requireRole(User.UserRole.ADMIN, operationName);
    }
    
    /**
     * Require Admin or Staff role with error dialog
     * 
     * @param operationName Name of the operation
     * @return true if authorized
     */
    public static boolean requireAdminOrStaff(String operationName) {
        return requireAnyRole(operationName, User.UserRole.ADMIN, User.UserRole.STAFF);
    }
    
    /**
     * Show unauthorized access dialog
     * 
     * @param requiredRole Required role (null for multiple roles)
     * @param operationName Name of the operation
     */
    private static void showUnauthorizedDialog(User.UserRole requiredRole, String operationName) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Unauthorized Access");
        
        Optional<User> currentUser = SessionManager.getCurrentUser();
        String message;
        if (requiredRole != null) {
            message = String.format(
                "You do not have permission to perform this operation.\n\n" +
                "Operation: %s\n" +
                "Required Role: %s\n" +
                "Your Role: %s",
                operationName,
                requiredRole,
                currentUser.isPresent() ? currentUser.get().getRole() : "Not logged in"
            );
        } else {
            message = String.format(
                "You do not have permission to perform this operation.\n\n" +
                "Operation: %s\n" +
                "Your Role: %s",
                operationName,
                currentUser.isPresent() ? currentUser.get().getRole() : "Not logged in"
            );
        }
        
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show unauthorized access confirmation dialog
     * 
     * @param operationName Name of the operation
     * @return true if user confirms to proceed anyway
     */
    public static boolean confirmUnauthorized(String operationName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Restricted Operation");
        alert.setHeaderText("Role Verification Required");
        alert.setContentText(String.format(
            "The operation '%s' requires elevated privileges.\n\n" +
            "Do you want to proceed anyway?",
            operationName
        ));
        
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        
        return result.isPresent() && result.get() == ButtonType.YES;
    }
    
    /**
     * Get current user role
     * 
     * @return Current user role or null if not logged in
     */
    public static User.UserRole getCurrentRole() {
        return SessionManager.getCurrentUser()
            .map(User::getRole)
            .orElse(null);
    }
    
    /**
     * Check if user is logged in
     * 
     * @return true if a user is logged in
     */
    public static boolean isLoggedIn() {
        return SessionManager.isLoggedIn();
    }
}
