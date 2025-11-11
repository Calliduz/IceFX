package application;

import com.icefx.util.NativeLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // CRITICAL: Load native libraries FIRST before any OpenCV operations
            logger.info("IceFX Attendance System Starting...");
            logger.info(NativeLoader.getSystemInfo());
            
            if (!NativeLoader.loadOpenCV()) {
                logger.error("Failed to load OpenCV native libraries - EXITING");
                Platform.exit();
                System.exit(1);
                return;
            }
            
            logger.info("Native libraries loaded successfully, initializing UI...");
            
            // Load UI from FXML (now under /application in resources)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Sample.fxml"));
            BorderPane root = loader.load();
            SampleController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            // Create scene
            Scene scene = new Scene(root, 1350, 720);

            // Apply stylesheet from classpath
            scene.getStylesheets().add(
                getClass().getResource("/application/application.css").toExternalForm()
            );

            // Set application icon from classpath
            primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/application/logo.png"))
            );

            // Configure and show stage
            primaryStage.setTitle("IceFX - Facial Attendance System");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();
            
            logger.info("✅ Application started successfully");
        } catch (Exception e) {
            logger.error("Fatal error during application startup", e);
            e.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }
    
    /**
     * Clean shutdown handler
     */
    private void shutdown() {
        logger.info("Application shutting down...");
        // TODO: Cleanup camera, database connections, etc.
        Platform.exit();
    }

    public static void main(String[] args) {
        // Launch JavaFX application
        launch(args);
    }
}
