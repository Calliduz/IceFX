package com.icefx.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
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
                
                // Create popup
                Popup popup = new Popup();
                
                // Create toast content
                VBox toastBox = createToastBox(message, type);
                popup.getContent().add(toastBox);
                
                // Position at top-right
                double x = ownerStage.getX() + ownerStage.getWidth() - 320;
                double y = ownerStage.getY() + 80;
                
                // Initial opacity for fade-in
                toastBox.setOpacity(0);
                
                // Show popup
                popup.show(ownerStage, x, y);
                
                // Fade-in animation
                FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, toastBox);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                
                // Fade-out animation
                FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, toastBox);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> popup.hide());
                
                // Slide-in animation
                TranslateTransition slideIn = new TranslateTransition(FADE_IN_DURATION, toastBox);
                slideIn.setFromX(50);
                slideIn.setToX(0);
                
                // Play animations
                ParallelTransition showTransition = new ParallelTransition(fadeIn, slideIn);
                SequentialTransition sequence = new SequentialTransition(
                    showTransition,
                    new PauseTransition(duration),
                    fadeOut
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
     * Create the toast UI component
     */
    private static VBox createToastBox(String message, ToastType type) {
        // Icon label
        Label iconLabel = new Label(type.icon);
        iconLabel.setStyle(
            "-fx-font-size: 24px; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-min-width: 40px; " +
            "-fx-alignment: center;"
        );
        
        // Message label
        Label messageLabel = new Label(message);
        messageLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-wrap-text: true; " +
            "-fx-max-width: 220px;"
        );
        
        // Container
        VBox container = new VBox(5, iconLabel, messageLabel);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
            "-fx-background-color: " + type.color + "; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 15px 20px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4); " +
            "-fx-min-width: 280px; " +
            "-fx-max-width: 280px;"
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
