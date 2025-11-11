package com.icefx.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Safe native library loader for OpenCV with comprehensive error handling.
 * This class prevents JVM crashes by validating native library loading before use.
 * 
 * CRITICAL: Must be called before ANY OpenCV operations.
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class NativeLoader {
    private static final Logger logger = LoggerFactory.getLogger(NativeLoader.class);
    private static boolean loaded = false;
    private static boolean loadAttempted = false;
    
    /**
     * Loads OpenCV native libraries with comprehensive error handling.
     * This method is thread-safe and idempotent.
     * 
     * @return true if successful, false if failed
     */
    public static synchronized boolean loadOpenCV() {
        if (loaded) {
            logger.debug("OpenCV already loaded, skipping");
            return true;
        }
        
        if (loadAttempted) {
            logger.warn("Previous load attempt failed, not retrying");
            return false;
        }
        
        loadAttempted = true;
        
        try {
            logger.info("========================================");
            logger.info("Starting OpenCV Native Library Loading");
            logger.info("========================================");
            logger.info("System Information:");
            logger.info("  OS: {} {} ({})", 
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
            logger.info("  Java: {} ({})", 
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
            logger.info("  Architecture: {}-bit", 
                System.getProperty("sun.arch.data.model"));
            logger.info("  Working Dir: {}", System.getProperty("user.dir"));
            
            // JavaCV will automatically extract and load correct platform natives
            logger.info("Loading opencv_java via Loader.load()...");
            Loader.load(opencv_java.class);
            
            loaded = true;
            logger.info("✅ OpenCV loaded successfully!");
            logger.info("========================================");
            return true;
            
        } catch (UnsatisfiedLinkError e) {
            logger.error("❌ CRITICAL: Failed to load OpenCV native libraries", e);
            logDetailedError(e);
            showNativeLibraryError(e);
            return false;
            
        } catch (Exception e) {
            logger.error("❌ CRITICAL: Unexpected error loading OpenCV", e);
            showGenericError(e);
            return false;
        }
    }
    
    /**
     * Check if OpenCV is loaded and ready to use.
     */
    public static boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Logs detailed diagnostic information about the error.
     */
    private static void logDetailedError(UnsatisfiedLinkError e) {
        logger.error("Native library load failed. Diagnostics:");
        logger.error("  Error message: {}", e.getMessage());
        logger.error("  Java library path: {}", System.getProperty("java.library.path"));
        logger.error("  Classpath: {}", System.getProperty("java.class.path"));
        
        // Log stack trace
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        logger.error("Stack trace:\n{}", sw.toString());
    }
    
    /**
     * Shows user-friendly error dialog for native library loading failure.
     */
    private static void showNativeLibraryError(UnsatisfiedLinkError e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Native Library Error");
            alert.setHeaderText("Failed to Load OpenCV");
            
            String content = 
                "Could not load OpenCV native libraries.\n\n" +
                "Common causes:\n" +
                "1. Missing opencv-platform dependency in pom.xml\n" +
                "2. Incompatible Java/OpenCV versions\n" +
                "3. Corrupted Maven cache\n" +
                "4. Architecture mismatch (32-bit vs 64-bit)\n\n" +
                "Solutions:\n" +
                "• Run: mvn clean install\n" +
                "• Delete Maven cache: rm -rf ~/.m2/repository/org/bytedeco\n" +
                "• Verify Java version: java -version\n" +
                "• Check pom.xml has opencv-platform dependency\n\n" +
                "System Info:\n" +
                "  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + "\n" +
                "  Java: " + System.getProperty("java.version") + "\n\n" +
                "Technical details:\n" + e.getMessage();
            
            alert.setContentText(content);
            
            // Add expandable exception details
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();
            
            Label label = new Label("Full Stack Trace:");
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);
            
            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);
            
            alert.getDialogPane().setExpandableContent(expContent);
            alert.getDialogPane().setExpanded(false);
            
            alert.showAndWait();
        });
    }
    
    /**
     * Shows generic error dialog for unexpected exceptions.
     */
    private static void showGenericError(Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Initialization Error");
            alert.setHeaderText("Failed to Initialize OpenCV");
            alert.setContentText(
                "An unexpected error occurred while initializing OpenCV.\n\n" +
                "Error: " + e.getMessage() + "\n\n" +
                "Please check the logs for more details."
            );
            alert.showAndWait();
        });
    }
    
    /**
     * Get detailed system information for debugging.
     */
    public static String getSystemInfo() {
        return String.format(
            "System Information:\n" +
            "  OS: %s %s (%s)\n" +
            "  Java: %s (%s)\n" +
            "  Architecture: %s-bit\n" +
            "  Working Directory: %s\n" +
            "  Java Library Path: %s",
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            System.getProperty("sun.arch.data.model"),
            System.getProperty("user.dir"),
            System.getProperty("java.library.path")
        );
    }
    
    /**
     * Force reload of native libraries (use with caution).
     * Only for testing/debugging purposes.
     */
    public static synchronized void forceReload() {
        logger.warn("Force reload requested - this may cause issues");
        loaded = false;
        loadAttempted = false;
    }
}
