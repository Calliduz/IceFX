package com.icefx.config;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppConfig.
 */
class AppConfigTest {
    
    private static Path originalConfigPath;
    private static Path tempConfigPath;
    
    @BeforeAll
    static void setupOnce() throws IOException {
        // Back up any existing config
        originalConfigPath = AppConfig.getConfigPath();
        if (Files.exists(originalConfigPath)) {
            tempConfigPath = Files.createTempFile("icefx-config-backup", ".properties");
            Files.copy(originalConfigPath, tempConfigPath, 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    @AfterAll
    static void teardownOnce() throws IOException {
        // Restore original config
        if (tempConfigPath != null && Files.exists(tempConfigPath)) {
            Files.copy(tempConfigPath, originalConfigPath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tempConfigPath);
        }
    }
    
    @BeforeEach
    void setup() throws IOException {
        // Delete config before each test to ensure clean state
        Files.deleteIfExists(AppConfig.getConfigPath());
        AppConfig.reload();
    }
    
    @Test
    @DisplayName("Should initialize with default values")
    void testInitializeDefaults() {
        AppConfig.initialize();
        
        assertEquals("light", AppConfig.getTheme());
        assertEquals("mysql", AppConfig.getDatabaseType());
        assertEquals(0, AppConfig.getCameraIndex());
        assertEquals(80.0, AppConfig.getConfidenceThreshold(), 0.01);
        assertEquals(3000, AppConfig.getDebounceMillis());
    }
    
    @Test
    @DisplayName("Should create config file on first initialization")
    void testConfigFileCreation() {
        AppConfig.initialize();
        assertTrue(Files.exists(AppConfig.getConfigPath()));
    }
    
    @Test
    @DisplayName("Should persist configuration changes")
    void testConfigPersistence() {
        AppConfig.initialize();
        
        AppConfig.setTheme("dark");
        AppConfig.set("test.key", "test.value");
        AppConfig.saveConfig();
        
        // Reload and verify
        AppConfig.reload();
        assertEquals("dark", AppConfig.getTheme());
        assertEquals("test.value", AppConfig.get("test.key", null));
    }
    
    @Test
    @DisplayName("Should handle integer properties")
    void testIntProperties() {
        AppConfig.initialize();
        
        assertEquals(0, AppConfig.getInt("camera.index", 99));
        assertEquals(640, AppConfig.getInt("camera.width", 99));
        assertEquals(99, AppConfig.getInt("nonexistent.key", 99));
    }
    
    @Test
    @DisplayName("Should handle double properties")
    void testDoubleProperties() {
        AppConfig.initialize();
        
        assertEquals(80.0, AppConfig.getDouble("recognition.confidence.threshold", 50.0), 0.01);
        assertEquals(50.0, AppConfig.getDouble("nonexistent.key", 50.0), 0.01);
    }
    
    @Test
    @DisplayName("Should handle boolean properties")
    void testBooleanProperties() {
        AppConfig.initialize();
        
        assertFalse(AppConfig.getBoolean("attendance.auto.checkout", true));
        assertTrue(AppConfig.getBoolean("ui.animation.enabled", false));
        assertFalse(AppConfig.getBoolean("nonexistent.key", false));
    }
    
    @Test
    @DisplayName("Should return default on invalid number format")
    void testInvalidNumberFormat() {
        AppConfig.initialize();
        AppConfig.set("invalid.int", "not-a-number");
        AppConfig.set("invalid.double", "not-a-double");
        
        assertEquals(999, AppConfig.getInt("invalid.int", 999));
        assertEquals(99.9, AppConfig.getDouble("invalid.double", 99.9), 0.01);
    }
    
    @Test
    @DisplayName("Should load database configuration defaults")
    void testDatabaseDefaults() {
        AppConfig.initialize();
        
        assertEquals("mysql", AppConfig.get("db.type", null));
        assertEquals("localhost", AppConfig.get("db.mysql.host", null));
        assertEquals("3306", AppConfig.get("db.mysql.port", null));
        assertEquals("facial_attendance", AppConfig.get("db.mysql.database", null));
        assertEquals("root", AppConfig.get("db.mysql.username", null));
        assertEquals("data/facial_attendance.db", AppConfig.get("db.sqlite.path", null));
    }
    
    @Test
    @DisplayName("Should load connection pool defaults")
    void testPoolDefaults() {
        AppConfig.initialize();
        
        assertEquals(10, AppConfig.getInt("db.pool.maxSize", 0));
        assertEquals(2, AppConfig.getInt("db.pool.minIdle", 0));
        assertEquals(30000, AppConfig.getInt("db.pool.connectionTimeout", 0));
    }
    
    @Test
    @DisplayName("Should load camera configuration defaults")
    void testCameraDefaults() {
        AppConfig.initialize();
        
        assertEquals(0, AppConfig.getCameraIndex());
        assertEquals(640, AppConfig.getInt("camera.width", 0));
        assertEquals(480, AppConfig.getInt("camera.height", 0));
        assertEquals(30, AppConfig.getInt("camera.fps", 0));
    }
    
    @Test
    @DisplayName("Should load recognition configuration defaults")
    void testRecognitionDefaults() {
        AppConfig.initialize();
        
        assertEquals(80.0, AppConfig.getConfidenceThreshold(), 0.01);
        assertEquals(3000, AppConfig.getDebounceMillis());
        assertEquals("resources/trained_faces.xml", AppConfig.getModelPath());
    }
}
