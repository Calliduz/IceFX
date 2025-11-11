package com.icefx.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern Toast Notification System
 * Displays non-intrusive temporary messages with smooth animations
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class ModernToast {
    
    private static final Logger logger = LoggerFactory.getLogger(ModernToast.class);
    private static final Duration FADE_IN_DURATION = Duration.millis(300);
    private static final Duration FADE_OUT_DURATION = Duration.millis(300);
    private static final Duration DISPLAY_DURATION = Duration.seconds(3);
    
    public enum ToastType {
        SUCCESS("#4CAF50", "✓"),
        ERROR("#F44336", "✗"),
        WARNING("#FF9800", "⚠"),
        INFO("#2196F3", "ℹ");
        
        final String color;
        final String icon;
        
        ToastType(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }
    
    /**
     * Show a success toast
     */
    public static void success(String message) {
        show(message, ToastType.SUCCESS);
    }
    
    /**
     * Show an error toast
     */
    public static void error(String message) {
        show(message, ToastType.ERROR);
    }
    
    /**
     * Show a warning toast
     */
    public static void warning(String message) {
        show(message, ToastType.WARNING);
    }
    
    /**
     * Show an info toast
     */
    public static void info(String message) {
        show(message, ToastType.INFO);
    }
    
    /**
     * Show a toast with custom duration
     */
    public static void show(String message, ToastType type, Duration duration) {
        Platform.runLater(() -> {
            try {
                // Get the primary stage
                Stage ownerStage = getActiveStage();
                if (ownerStage == null) {
                    logger.warn("No active stage found for toast notification");
                    return;
                }
                
                // Create a transparent stage for the toast
                Stage toastStage = new Stage();
                toastStage.initOwner(ownerStage);
                toastStage.setResizable(false);
                toastStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                toastStage.setAlwaysOnTop(true);
                
                // Create toast content
                VBox toastBox = createToastBox(message, type);
                
                // Create transparent scene
                StackPane root = new StackPane(toastBox);
                root.setStyle("-fx-background-color: transparent;");
                Scene scene = new Scene(root);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                toastStage.setScene(scene);
                
                // Position consistently at top-right corner
                double targetX = ownerStage.getX() + ownerStage.getWidth() - 340;
                double y = ownerStage.getY() + 20; // Consistent top position
                toastStage.setX(targetX);
                toastStage.setY(y);
                
                // Start with toast off-screen to the right
                toastBox.setTranslateX(400);
                
                // Show stage
                toastStage.show();
                
                // Slide-in animation (from right)
                TranslateTransition slideIn = new TranslateTransition(FADE_IN_DURATION, toastBox);
                slideIn.setFromX(400);
                slideIn.setToX(0);
                slideIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                
                // Slide-out animation (to right)
                TranslateTransition slideOut = new TranslateTransition(FADE_OUT_DURATION, toastBox);
                slideOut.setFromX(0);
                slideOut.setToX(400);
                slideOut.setInterpolator(javafx.animation.Interpolator.EASE_IN);
                slideOut.setOnFinished(e -> toastStage.close());
                
                // Play animations in sequence
                SequentialTransition sequence = new SequentialTransition(
                    slideIn,
                    new PauseTransition(duration),
                    slideOut
                );
                sequence.play();
                
            } catch (Exception e) {
                logger.error("Failed to show toast notification", e);
            }
        });
    }
    
    /**
     * Show a toast with default duration
     */
    public static void show(String message, ToastType type) {
        show(message, type, DISPLAY_DURATION);
    }
    
    /**
     * Create the toast UI component - Modern white design with colored accent
     */
    private static VBox createToastBox(String message, ToastType type) {
        // Icon label with colored background
        Label iconLabel = new Label(type.icon);
        iconLabel.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-min-width: 36px; " +
            "-fx-min-height: 36px; " +
            "-fx-max-width: 36px; " +
            "-fx-max-height: 36px; " +
            "-fx-alignment: center; " +
            "-fx-background-color: " + type.color + "; " +
            "-fx-background-radius: 18px;"
        );
        
        // Message label - dark text on white
        Label messageLabel = new Label(message);
        messageLabel.setStyle(
            "-fx-text-fill: #212121; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 500; " +
            "-fx-wrap-text: true; " +
            "-fx-max-width: 200px;"
        );
        
        // Horizontal layout with icon and message
        HBox content = new HBox(12, iconLabel, messageLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 4 8 4 4;");
        
        // White container with subtle shadow and colored left border
        VBox container = new VBox(content);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 12px 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); " +
            "-fx-border-color: " + type.color + " transparent transparent transparent; " +
            "-fx-border-width: 3px 0 0 0; " +
            "-fx-border-radius: 8px 8px 0 0; " +
            "-fx-min-width: 300px; " +
            "-fx-max-width: 300px;"
        );
        
        return container;
    }
    
    /**
     * Get the currently active stage
     */
    private static Stage getActiveStage() {
        // Try to get from IceFXApplication
        Stage stage = com.icefx.IceFXApplication.getPrimaryStage();
        if (stage != null) {
            return stage;
        }
        
        // Fallback: find any visible stage
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        
        return null;
    }
}
