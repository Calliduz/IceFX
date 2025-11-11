package com.icefx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application Configuration Manager
 * Handles loading and saving of application settings
 * 
 * @author IceFX Team
 * @version 2.0
 * @since JDK 23.0.1
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "config.properties";
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".icefx", CONFIG_FILE);
    
    private static Properties properties;
    private static boolean initialized = false;
    
    // Default configuration values
    private static final String DEFAULT_THEME = "light";
    private static final String DEFAULT_DB_TYPE = "mysql";
    private static final String DEFAULT_CAMERA_INDEX = "0";
    private static final String DEFAULT_CONFIDENCE_THRESHOLD = "80.0";
    private static final String DEFAULT_DEBOUNCE_TIME = "3000";
    private static final String DEFAULT_MODEL_PATH = "resources/trained_faces.xml";
    
    /**
     * Initialize configuration - load from file or create defaults
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        properties = new Properties();
        
        try {
            // Create config directory if it doesn't exist
            Files.createDirectories(CONFIG_PATH.getParent());
            
            // Load existing config or create default
            if (Files.exists(CONFIG_PATH)) {
                loadConfig();
            } else {
                createDefaultConfig();
            }
            
            initialized = true;
            logger.info("✅ Configuration initialized successfully");
            
        } catch (IOException e) {
            logger.error("Failed to initialize configuration", e);
            // Use in-memory defaults
            setDefaults();
            initialized = true;
        }
    }
    
    /**
     * Load configuration from file
     */
    private static void loadConfig() throws IOException {
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
            logger.info("Configuration loaded from: {}", CONFIG_PATH);
        }
    }
    
    /**
     * Create default configuration file
     */
    private static void createDefaultConfig() throws IOException {
        setDefaults();
        saveConfig();
        logger.info("Default configuration created at: {}", CONFIG_PATH);
    }
    
    /**
     * Set default configuration values
     */
    private static void setDefaults() {
        properties.setProperty("app.theme", DEFAULT_THEME);
        properties.setProperty("app.language", "en");
        
        // Database configuration
        properties.setProperty("db.type", DEFAULT_DB_TYPE);
        properties.setProperty("db.mysql.host", "localhost");
        properties.setProperty("db.mysql.port", "3306");
        properties.setProperty("db.mysql.database", "facial_attendance");
        properties.setProperty("db.mysql.username", "root");
        properties.setProperty("db.mysql.password", "");
        properties.setProperty("db.sqlite.path", "data/facial_attendance.db");
        
        // Camera configuration
        properties.setProperty("camera.index", DEFAULT_CAMERA_INDEX);
        properties.setProperty("camera.width", "640");
        properties.setProperty("camera.height", "480");
        properties.setProperty("camera.fps", "30");
        
        // Recognition configuration
    properties.setProperty("recognition.confidence.threshold", DEFAULT_CONFIDENCE_THRESHOLD);
    properties.setProperty("recognition.debounce.millis", DEFAULT_DEBOUNCE_TIME);
    properties.setProperty("recognition.model.path", DEFAULT_MODEL_PATH);
        properties.setProperty("recognition.haar.cascade", "resources/haar/haarcascade_frontalface_default.xml");
        
        // Attendance configuration
        properties.setProperty("attendance.duplicate.prevention.minutes", "60");
        properties.setProperty("attendance.auto.checkout", "false");
        
        // UI configuration
        properties.setProperty("ui.window.width", "1350");
        properties.setProperty("ui.window.height", "720");
        properties.setProperty("ui.animation.enabled", "true");
    }
    
    /**
     * Save current configuration to file
     */
    public static synchronized void saveConfig() {
        if (!initialized) {
            initialize();
        }
        
        try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(output, "IceFX Application Configuration");
            logger.info("Configuration saved to: {}", CONFIG_PATH);
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }
    
    /**
     * Get configuration property
     */
    public static String get(String key, String defaultValue) {
        if (!initialized) {
            initialize();
        }
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Set configuration property
     */
    public static void set(String key, String value) {
        if (!initialized) {
            initialize();
        }
        properties.setProperty(key, value);
    }
    
    /**
     * Get integer property
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get double property
     */
    public static double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get boolean property
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }
    
    // Convenience methods for common properties
    
    public static String getTheme() {
        return get("app.theme", DEFAULT_THEME);
    }
    
    public static void setTheme(String theme) {
        set("app.theme", theme);
        saveConfig();
    }
    
    public static String getDatabaseType() {
        return get("db.type", DEFAULT_DB_TYPE);
    }
    
    public static int getCameraIndex() {
        return getInt("camera.index", Integer.parseInt(DEFAULT_CAMERA_INDEX));
    }
    
    public static double getConfidenceThreshold() {
        return getDouble("recognition.confidence.threshold", 
            Double.parseDouble(DEFAULT_CONFIDENCE_THRESHOLD));
    }
    
    public static long getDebounceMillis() {
        return Long.parseLong(get("recognition.debounce.millis", DEFAULT_DEBOUNCE_TIME));
    }

    public static String getModelPath() {
        return get("recognition.model.path", DEFAULT_MODEL_PATH);
    }
    
    /**
     * Get configuration file path
     */
    public static Path getConfigPath() {
        return CONFIG_PATH;
    }
    
    /**
     * Reload configuration from file
     */
    public static synchronized void reload() {
        initialized = false;
        initialize();
    }
}
