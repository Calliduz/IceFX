package com.icefx.service;

import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import com.icefx.model.User.UserRole;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User service for authentication and user management.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserDAO userDAO;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int BCRYPT_ROUNDS = 10;
    
    public static class AuthResult {
        private final boolean success;
        private final User user;
        private final String message;
        
        private AuthResult(boolean success, User user, String message) {
            this.success = success;
            this.user = user;
            this.message = message;
        }
        
        public static AuthResult success(User user) {
            return new AuthResult(true, user, "Authentication successful");
        }
        
        public static AuthResult failure(String message) {
            return new AuthResult(false, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public String getMessage() { return message; }
    }
    
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
        logger.info("UserService initialized");
    }
    
    public AuthResult authenticate(String userCode, String password) {
        try {
            Optional<User> userOpt = userDAO.findByUserCode(userCode);
            
            if (!userOpt.isPresent()) {
                logger.warn("Authentication failed: User code not found: {}", userCode);
                return AuthResult.failure("Invalid user code or password");
            }
            
            User user = userOpt.get();
            
            if (!user.isActive()) {
                logger.warn("Authentication failed: User account is inactive: {}", userCode);
                return AuthResult.failure("User account is inactive");
            }
            
            if (BCrypt.checkpw(password, user.getPassword())) {
                logger.info("✅ Authentication successful for user: {}", userCode);
                return AuthResult.success(user);
            } else {
                logger.warn("Authentication failed: Invalid password for user: {}", userCode);
                return AuthResult.failure("Invalid user code or password");
            }
            
        } catch (Exception e) {
            logger.error("Authentication error for user: {}", userCode, e);
            return AuthResult.failure("Authentication error: " + e.getMessage());
        }
    }
    
    public User createUser(String userCode, String fullName, String department, 
                          String position, UserRole role, String plainPassword) throws SQLException {
        if (plainPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        
        if (userCode == null || userCode.trim().isEmpty()) {
            throw new IllegalArgumentException("User code cannot be empty");
        }
        
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        
        Optional<User> existing = userDAO.findByUserCode(userCode);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User code already exists: " + userCode);
        }
        
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
        
        User user = new User(
            0, // userId (will be set by database)
            userCode,
            fullName,
            department,
            position,
            role,
            hashedPassword,
            LocalDateTime.now(),
            true // active
        );
        
        int userId = userDAO.createUser(user);
        user.setUserId(userId);
        
        logger.info("✅ User created: {} (ID: {})", userCode, userId);
        return user;
    }
    
    public User updateUser(User user) throws SQLException {
        if (user.getUserId() <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        
        userDAO.update(user);
        logger.info("✅ User updated: {}", user.getUserCode());
        return user;
    }
    
    public boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        
        Optional<User> userOpt = userDAO.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            logger.warn("Change password failed: Invalid old password for user {}", userId);
            return false;
        }
        
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
        user.setPassword(hashedPassword);
        userDAO.update(user);
        
        logger.info("✅ Password changed for user {}", userId);
        return true;
    }
    
    public boolean resetPassword(int userId, String newPassword) throws SQLException {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        
        Optional<User> userOpt = userDAO.findById(userId);
        if (!userOpt.isPresent()) {
            return false;
        }
        
        User user = userOpt.get();
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
        user.setPassword(hashedPassword);
        userDAO.update(user);
        
        logger.info("✅ Password reset for user {}", userId);
        return true;
    }
    
    public Optional<User> getUserById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }
    
    public Optional<User> getUserByUserCode(String userCode) throws SQLException {
        return userDAO.findByUserCode(userCode);
    }
    
    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }
    
    public List<User> getUsersByRole(UserRole role) throws SQLException {
        return userDAO.findByRole(role);
    }
    
    public boolean deactivateUser(int userId) throws SQLException {
        Optional<User> userOpt = userDAO.findById(userId);
        if (!userOpt.isPresent()) {
            return false;
        }
        
        User user = userOpt.get();
        user.setActive(false);
        userDAO.update(user);
        
        logger.info("✅ User deactivated: {}", userId);
        return true;
    }
    
    public boolean activateUser(int userId) throws SQLException {
        Optional<User> userOpt = userDAO.findById(userId);
        if (!userOpt.isPresent()) {
            return false;
        }
        
        User user = userOpt.get();
        user.setActive(true);
        userDAO.update(user);
        
        logger.info("✅ User activated: {}", userId);
        return true;
    }
    
    public boolean deleteUser(int userId) throws SQLException {
        boolean deleted = userDAO.delete(userId);
        if (deleted) {
            logger.info("✅ User deleted: {}", userId);
        }
        return deleted;
    }
}
