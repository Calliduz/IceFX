package application;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Toast {
    public static void show(Stage owner, String message) {
        Platform.runLater(() -> {
            Popup popup = new Popup();
            Label label = new Label(message);
            label.setStyle("-fx-background-color: #4a6741; -fx-text-fill: white; -fx-padding: 12 24 12 24; -fx-background-radius: 8; -fx-background-insets: 0; -fx-font-size: 15px;");
            StackPane pane = new StackPane(label);
            pane.setStyle("-fx-background-color: #4a6741; -fx-background-radius: 8; -fx-padding: 0;");
            popup.getContent().add(pane);
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            // Wait for layout to be calculated before showing
            pane.applyCss();
            pane.layout();
            double x = owner.getX() + (owner.getWidth() - pane.getWidth()) / 2;
            double y = owner.getY() + owner.getHeight() - 80;
            popup.show(owner, x, y);

            FadeTransition fade = new FadeTransition(Duration.seconds(0.5), pane);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setDelay(Duration.seconds(2));
            fade.setOnFinished(e -> popup.hide());
            fade.play();
        });
    }
}