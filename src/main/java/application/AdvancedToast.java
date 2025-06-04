package application;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AdvancedToast {
    public enum ToastType { SUCCESS, INFO, WARNING, ERROR }

    public static void show(Stage owner, String message, ToastType type) {
        Platform.runLater(() -> {
            Popup popup = new Popup();
            Label label = new Label(message);
            label.setStyle("-fx-font-size: 16px; -fx-padding: 16 32 16 32; -fx-background-color: transparent; -fx-text-fill: white;");
            label.setEffect(new DropShadow(8, Color.gray(0, 0.4)));

            // Color by type
            String bg;
            switch (type) {
                case SUCCESS: bg = "#43a047"; break; // green
                case INFO:    bg = "#1976d2"; break; // blue
                case WARNING: bg = "#fbc02d"; label.setStyle(label.getStyle() + "-fx-text-fill: #222;"); break; // yellow
                case ERROR:   bg = "#e53935"; break; // red
                default:      bg = "#333";
            }
            label.setStyle(label.getStyle() + "-fx-background-color: " + bg + ";");

            StackPane pane = new StackPane(label);
            pane.setPadding(new Insets(10));
            pane.setPickOnBounds(false);
            pane.setAlignment(Pos.BOTTOM_CENTER);
            pane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

            // --- Toast visual fix ---
            // Remove all background from StackPane, and set label background only
            pane.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-color: transparent;");
            label.setStyle("-fx-font-size: 16px; -fx-padding: 16 32 16 32; -fx-background-radius: 20; -fx-background-color: " + bg + "; -fx-text-fill: " + (type == ToastType.WARNING ? "#222;" : "white;") + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 16,0,0,4); -fx-border-color: transparent; -fx-background-insets: 0;");
            label.setMinWidth(Label.USE_PREF_SIZE);
            label.setMinHeight(Label.USE_PREF_SIZE);
            // --- End Toast visual fix ---

            popup.getContent().add(pane);

            // Wait for layout to be calculated before showing
            pane.applyCss();
            pane.layout();
            Scene scene = owner.getScene();
            double x = owner.getX() + (scene.getWidth() - pane.getWidth()) / 2;
            double y = owner.getY() + scene.getHeight() - 100;
            popup.show(owner, x, y);

            // Animation: slide up & fade in
            pane.setOpacity(0);
            pane.setTranslateY(40);
            Timeline showAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pane.opacityProperty(), 0),
                    new KeyValue(pane.translateYProperty(), 40)
                ),
                new KeyFrame(Duration.millis(350),
                    new KeyValue(pane.opacityProperty(), 1),
                    new KeyValue(pane.translateYProperty(), 0)
                )
            );
            showAnim.play();

            // Auto-dismiss after 2.5s with fade out
            PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
            pause.setOnFinished(e -> {
                Timeline hideAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(pane.opacityProperty(), 1),
                        new KeyValue(pane.translateYProperty(), 0)
                    ),
                    new KeyFrame(Duration.millis(350),
                        new KeyValue(pane.opacityProperty(), 0),
                        new KeyValue(pane.translateYProperty(), 40)
                    )
                );
                hideAnim.setOnFinished(ev -> popup.hide());
                hideAnim.play();
            });
            pause.play();
        });
    }
}