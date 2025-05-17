package application;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load UI from FXML (now under /application in resources)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Sample.fxml"));
            BorderPane root = loader.load();

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
            primaryStage.setTitle("Attendance Management System");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Load OpenCV native library
        System.load("C:\\\\Users\\\\ljcab\\\\Downloads\\\\facial-attendance-main\\\\native\\\\opencv_java4110.dll");

        // Launch JavaFX application
        launch(args);
    }
}
