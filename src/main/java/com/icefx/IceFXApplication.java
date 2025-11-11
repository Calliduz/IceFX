package com.icefx;

import com.icefx.config.AppConfig;
import com.icefx.controller.LoginController;
import com.icefx.util.NativeLoader;
import com.icefx.util.SessionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * IceFX Facial Attendance System - Main Application Entry Point
 * 
 * Modern JavaFX 23 application with OpenCV integration
 * 
 * @author IceFX Team
 * @version 2.0
 * @since JDK 23.0.1
 */
public class IceFXApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(IceFXApplication.class);
    private static Stage primaryStage;
    
    /**
     * JavaFX application initialization
     * Called before start() method
     */
    @Override
    public void init() throws Exception {
        super.init();
        logger.info("═══════════════════════════════════════════════════════");
        logger.info("IceFX Facial Attendance System v2.0");
        logger.info("JDK Version: {}", System.getProperty("java.version"));
        logger.info("JavaFX Version: {}", System.getProperty("javafx.version"));
        logger.info("═══════════════════════════════════════════════════════");
        
        // Initialize application configuration
        AppConfig.initialize();
        
        // Load OpenCV native libraries
        logger.info("Loading OpenCV native libraries...");
        if (!NativeLoader.loadOpenCV()) {
            logger.error("❌ Failed to load OpenCV native libraries");
            Platform.exit();
            System.exit(1);
        }
        logger.info("✅ OpenCV libraries loaded successfully");
    }
    
    /**
     * JavaFX application start method
     * Sets up the primary stage and loads the login screen
     */
    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            
            // Load Login screen
            logger.info("Loading Login screen...");
            showLoginScreen();
            
            // Configure stage
            primaryStage.setTitle("IceFX - Facial Attendance System");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Set application icon
            try {
                Image icon = new Image(
                    Objects.requireNonNull(
                        getClass().getResourceAsStream("/com/icefx/images/logo.png")
                    )
                );
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                logger.warn("Could not load application icon: {}", e.getMessage());
            }
            
            // Handle close request
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                shutdown();
            });
            
            primaryStage.show();
            logger.info("✅ Application started successfully");
            
        } catch (Exception e) {
            logger.error("❌ Fatal error during application startup", e);
            showErrorAndExit("Startup Error", 
                "Failed to start application: " + e.getMessage());
        }
    }
    
    /**
     * Load and display the login screen
     */
    private void showLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/icefx/view/Login.fxml")
        );
        Parent root = loader.load();
        
        // Get controller and set primary stage reference
        LoginController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        
        Scene scene = new Scene(root);
        
        // Apply theme stylesheet
        String theme = AppConfig.getTheme();
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/com/icefx/styles/" + theme + "-theme.css")
            ).toExternalForm()
        );
        
        primaryStage.setScene(scene);
    }
    
    /**
     * Clean shutdown of the application
     */
    @Override
    public void stop() throws Exception {
        shutdown();
        super.stop();
    }
    
    /**
     * Perform cleanup operations before exit
     */
    private void shutdown() {
        logger.info("Application shutdown initiated...");
        
        try {
            // Close database connections
            logger.info("Closing database connections...");
            // DatabaseManager.getInstance().shutdown();
            
            // Release camera resources
            logger.info("Releasing camera resources...");
            // CameraService.getInstance().release();
            
            SessionManager.clear();
            logger.info("✅ Clean shutdown completed");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        } finally {
            Platform.exit();
        }
    }
    
    /**
     * Show error dialog and exit application
     */
    private void showErrorAndExit(String title, String message) {
        logger.error("{}: {}", title, message);
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            Platform.exit();
            System.exit(1);
        });
    }
    
    /**
     * Get the primary stage (for controller access)
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Application entry point
     */
    public static void main(String[] args) {
        // Set system properties for better JavaFX performance on JDK 23
        System.setProperty("prism.lcdtext", "false"); // Better text rendering
        System.setProperty("prism.text", "t2k"); // Better text quality
        
        // Launch JavaFX application
        launch(args);
    }
}
