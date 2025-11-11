package com.icefx.dao;

import com.icefx.config.DatabaseConfig;
import com.icefx.model.FaceTemplate;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for FaceTemplate entities.
 */
public class FaceTemplateDAO {
    
    /**
     * Add a face template for a user
     */
    public int addTemplate(FaceTemplate template) throws SQLException {
        String sql = "INSERT INTO face_templates (person_id, template_data, created_at, is_primary) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, template.getUserId());
            ps.setBytes(2, template.getTemplateData());
            ps.setTimestamp(3, Timestamp.valueOf(template.getCreatedAt()));
            ps.setBoolean(4, template.isPrimary());
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int templateId = rs.getInt(1);
                        template.setTemplateId(templateId);
                        return templateId;
                    }
                }
            }
            
            throw new SQLException("Failed to add template, no ID obtained");
        }
    }
    
    /**
     * Get all templates for a user
     */
    public List<FaceTemplate> findByUserId(int userId) throws SQLException {
        String sql = "SELECT template_id, person_id, template_data, created_at, is_primary " +
                    "FROM face_templates WHERE person_id = ? ORDER BY is_primary DESC, created_at DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            return mapResultSet(ps.executeQuery());
        }
    }
    
    /**
     * Get the primary template for a user
     */
    public Optional<FaceTemplate> findPrimaryByUserId(int userId) throws SQLException {
        String sql = "SELECT template_id, person_id, template_data, created_at, is_primary " +
                    "FROM face_templates WHERE person_id = ? AND is_primary = TRUE LIMIT 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            List<FaceTemplate> results = mapResultSet(ps.executeQuery());
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }
    }
    
    /**
     * Replace all templates for a user with a new one
     */
    public void replaceAllTemplates(int userId, byte[] newTemplateData) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Delete all existing templates
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM face_templates WHERE person_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            // Add new template
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO face_templates (person_id, template_data, created_at, is_primary) VALUES (?, ?, ?, TRUE)")) {
                ps.setInt(1, userId);
                ps.setBytes(2, newTemplateData);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
            
            conn.commit();
            
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
     * Delete a specific template
     */
    public boolean delete(int templateId) throws SQLException {
        String sql = "DELETE FROM face_templates WHERE template_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, templateId);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete template by data (for backward compatibility)
     */
    public boolean deleteByData(int userId, byte[] templateData) throws SQLException {
        String sql = "DELETE FROM face_templates WHERE person_id = ? AND template_data = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setBytes(2, templateData);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete all templates for a user
     */
    public boolean deleteByUserId(int userId) throws SQLException {
        String sql = "DELETE FROM face_templates WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Set a template as primary (and unset others)
     */
    public void setPrimaryTemplate(int templateId, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Unset all primary flags for this user
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE face_templates SET is_primary = FALSE WHERE person_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            // Set this template as primary
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE face_templates SET is_primary = TRUE WHERE template_id = ?")) {
                ps.setInt(1, templateId);
                ps.executeUpdate();
            }
            
            conn.commit();
            
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
     * Get count of templates for a user
     */
    public int countByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM face_templates WHERE person_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Map ResultSet to list of FaceTemplate objects
     */
    private List<FaceTemplate> mapResultSet(ResultSet rs) throws SQLException {
        List<FaceTemplate> templates = new ArrayList<>();
        
        while (rs.next()) {
            int templateId = rs.getInt("template_id");
            int userId = rs.getInt("person_id");
            byte[] data = rs.getBytes("template_data");
            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            boolean isPrimary = rs.getBoolean("is_primary");
            
            templates.add(new FaceTemplate(templateId, userId, data, createdAt, isPrimary));
        }
        
        return templates;
    }
}
