package com.icefx.dao;

import com.icefx.config.DatabaseConfig;
import com.icefx.model.User;
import com.icefx.model.User.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User entities.
 * Handles all database operations related to users.
 */
public class UserDAO {
    
    /**
     * Create a new user in the database
     */
    public int createUser(User user) throws SQLException {
        String sql = "INSERT INTO persons (person_code, full_name, department, position, role, password, created_at, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, user.getUserCode());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getDepartment());
            ps.setString(4, user.getPosition());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getPassword());
            ps.setTimestamp(7, Timestamp.valueOf(user.getCreatedAt()));
            ps.setBoolean(8, user.isActive());
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        user.setUserId(userId);
                        return userId;
                    }
                }
            }
            
            throw new SQLException("Failed to create user, no ID obtained");
        }
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT person_id, person_code, full_name, department, position, role, password, created_at, active " +
                    "FROM persons WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Find user by user code
     */
    public Optional<User> findByUserCode(String userCode) throws SQLException {
        String sql = "SELECT person_id, person_code, full_name, department, position, role, password, created_at, active " +
                    "FROM persons WHERE person_code = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, userCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all users
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT person_id, person_code, full_name, department, position, role, password, created_at, active " +
                    "FROM persons ORDER BY full_name";
        
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        
        return users;
    }
    
    /**
     * Get all active users
     */
    public List<User> findAllActive() throws SQLException {
        String sql = "SELECT person_id, person_code, full_name, department, position, role, password, created_at, active " +
                    "FROM persons WHERE active = TRUE ORDER BY full_name";
        
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        
        return users;
    }
    
    /**
     * Get users by role
     */
    public List<User> findByRole(UserRole role) throws SQLException {
        String sql = "SELECT person_id, person_code, full_name, department, position, role, password, created_at, active " +
                    "FROM persons WHERE role = ? AND active = TRUE ORDER BY full_name";
        
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, role.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        
        return users;
    }
    
    /**
     * Update user information
     */
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE persons SET person_code = ?, full_name = ?, department = ?, " +
                    "position = ?, role = ?, password = ?, active = ? WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, user.getUserCode());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getDepartment());
            ps.setString(4, user.getPosition());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getPassword());
            ps.setBoolean(7, user.isActive());
            ps.setInt(8, user.getUserId());
            
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Soft delete user (set active = false)
     */
    public boolean deactivate(int userId) throws SQLException {
        String sql = "UPDATE persons SET active = FALSE WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Hard delete user and all related data
     */
    public boolean delete(int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Delete face templates
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM face_templates WHERE person_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            // Delete attendance logs (optional - consider keeping for audit)
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attendance_logs WHERE person_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            // Delete schedules
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM schedules WHERE person_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            // Delete user
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM persons WHERE person_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
    
    /**
     * Search users by name or code
     */
    public List<User> search(String query) throws SQLException {
        String sql = "SELECT person_id, person_code, full_name, department, position, role, password, created_at, active " +
                    "FROM persons WHERE (full_name LIKE ? OR person_code LIKE ?) AND active = TRUE " +
                    "ORDER BY full_name";
        
        List<User> users = new ArrayList<>();
        String searchPattern = "%" + query + "%";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        
        return users;
    }
    
    /**
     * Check if user code already exists
     */
    public boolean existsByUserCode(String userCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM persons WHERE person_code = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, userCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("person_id");
        String userCode = rs.getString("person_code");
        String fullName = rs.getString("full_name");
        String department = rs.getString("department");
        String position = rs.getString("position");
        
        String roleStr = rs.getString("role");
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.STUDENT;
        
        String password = rs.getString("password");
        
        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();
        
        boolean active = rs.getBoolean("active");
        
        return new User(userId, userCode, fullName, department, position, role, password, createdAt, active);
    }
}
