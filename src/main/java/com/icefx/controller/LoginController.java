package com.icefx.controller;

import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import com.icefx.service.UserService;
import com.icefx.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controller for the login screen.
 * Demonstrates integration with the new UserService for authentication.
 * 
 * Features:
 * - BCrypt password verification
 * - Role-based access control
 * - User-friendly error messages
 * - Session management
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    @FXML private TextField userCodeField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator progressIndicator;
    
    private UserService userService;
    private UserDAO userDAO;
    private Stage primaryStage;
    
    /**
     * Set the primary stage reference
     * @param stage The primary application stage
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
    
    /**
     * Initialize the controller.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("LoginController initializing...");
        
        // Initialize services
        try {
            userDAO = new UserDAO();
            userService = new UserService(userDAO);
            logger.info("✅ UserService initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize UserService", e);
            showError("Failed to initialize authentication system: " + e.getMessage());
            return;
        }
        
        // Hide progress indicator initially
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
        
        // Hide error label initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        
        // Set up Enter key to trigger login
        if (passwordField != null) {
            passwordField.setOnAction(event -> handleLogin());
        }
        
        logger.info("LoginController initialized successfully");
    }
    
    /**
     * Handle login button click.
     */
    @FXML
    private void handleLogin() {
        String userCode = userCodeField.getText().trim();
        String password = passwordField.getText();
        
        // Validation
        if (userCode.isEmpty()) {
            showError("Please enter your user code");
            userCodeField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter your password");
            passwordField.requestFocus();
            return;
        }
        
        // Show progress
        setLoading(true);
        hideError();
        
        // Perform authentication in background thread
        new Thread(() -> {
            try {
                logger.info("Attempting authentication for user: {}", userCode);
                
                UserService.AuthResult result = userService.authenticate(userCode, password);
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    setLoading(false);
                    
                    if (result.isSuccess()) {
                        handleSuccessfulLogin(result.getUser());
                    } else {
                        showError(result.getMessage());
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
                
            } catch (Exception e) {
                logger.error("Authentication error", e);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Authentication failed: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Handle successful login - navigate to appropriate dashboard.
     */
    private void handleSuccessfulLogin(User user) {
        logger.info("✅ Login successful for user: {} (Role: {})",
            user.getUserCode(), user.getRole().getDisplayName());

        SessionManager.startSession(user);

        try {
            String fxmlPath;
            String windowTitle;
            switch (user.getRole()) {
                case ADMIN -> {
                    fxmlPath = "/com/icefx/view/AdminPanel.fxml";
                    windowTitle = "IceFX - Admin Panel";
                }
                case STAFF -> {
                    fxmlPath = "/com/icefx/view/Dashboard.fxml";
                    windowTitle = "IceFX - Staff Dashboard";
                }
                case STUDENT -> {
                    fxmlPath = "/com/icefx/view/Dashboard.fxml";
                    windowTitle = "IceFX - Student Dashboard";
                }
                default -> throw new IllegalStateException("Unsupported role: " + user.getRole());
            }

            loadDashboard(fxmlPath, user, windowTitle);
            clearForm();

        } catch (Exception e) {
            logger.error("Error navigating to dashboard", e);
            showError("Failed to load dashboard: " + e.getMessage());
            SessionManager.clear();
        }
    }

    /**
     * Load dashboard scene based on the authenticated user.
     */
    private void loadDashboard(String fxmlPath, User user, String windowTitle) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller instanceof DashboardController dashboardController) {
            dashboardController.setCurrentUser(user);
        } else if (controller instanceof AdminController adminController) {
            adminController.setCurrentUser(user);
        }

        Stage stage = primaryStage != null ? primaryStage : (Stage) loginButton.getScene().getWindow();
        if (stage == null) {
            throw new IllegalStateException("Primary stage is not available");
        }

        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

    stage.setTitle(windowTitle);
    stage.sizeToScene();
    stage.centerOnScreen();
    stage.show();
    }
    
    /**
     * Show error message to user.
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
        logger.warn("Login error shown to user: {}", message);
    }
    
    /**
     * Hide error message.
     */
    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }
    
    /**
     * Set loading state (disable form during authentication).
     */
    private void setLoading(boolean loading) {
        if (loginButton != null) {
            loginButton.setDisable(loading);
        }
        if (userCodeField != null) {
            userCodeField.setDisable(loading);
        }
        if (passwordField != null) {
            passwordField.setDisable(loading);
        }
        if (progressIndicator != null) {
            progressIndicator.setVisible(loading);
        }
    }
    
    /**
     * Clear the login form.
     */
    private void clearForm() {
        if (userCodeField != null) {
            userCodeField.clear();
        }
        if (passwordField != null) {
            passwordField.clear();
        }
        hideError();
        userCodeField.requestFocus();
    }
    
    /**
     * Handle cancel button (if present).
     */
    @FXML
    private void handleCancel() {
        logger.info("Login cancelled by user");
        SessionManager.clear();
        Platform.exit();
    }
}
