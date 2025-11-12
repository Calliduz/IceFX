package com.icefx.controller;

import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import com.icefx.service.UserService;
import com.icefx.util.SessionManager;
import com.icefx.util.ModernToast;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controller for the login screen.
 * Modern UI with Toast notifications and smooth transitions.
 * 
 * Features:
 * - BCrypt password verification
 * - Role-based access control
 * - Modern toast notifications
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
    @FXML private VBox roleSelectionBox;
    @FXML private VBox loginFormBox;
    @FXML private VBox demoCredentialsBox;
    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSubtitle;
    
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
            ModernToast.success("Authentication system ready");
        } catch (Exception e) {
            logger.error("Failed to initialize UserService", e);
            showError("Failed to initialize authentication system: " + e.getMessage());
            ModernToast.error("Failed to initialize authentication system");
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
     * Handle student role selection - go directly to student dashboard with face scan.
     */
    @FXML
    private void handleStudentRole() {
        logger.info("Student role selected - loading auto-scan dashboard");
        ModernToast.info("Loading student dashboard...");
        
        try {
            // Load student dashboard (auto-scan mode)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icefx/view/Dashboard.fxml"));
            Parent root = loader.load();
            
            // Get controller and set to student mode
            Object controller = loader.getController();
            if (controller instanceof DashboardController dashboardController) {
                // Student mode - no specific user, auto-recognize everyone
                dashboardController.setStudentAutoScanMode(true);
            }
            
            Stage stage = primaryStage != null ? primaryStage : (Stage) roleSelectionBox.getScene().getWindow();
            Scene scene = stage.getScene();
            
            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            
            stage.setTitle("IceFX - Student Dashboard (Auto-Scan)");
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
            
            ModernToast.success("Student dashboard loaded - Camera will start automatically");
            
        } catch (Exception e) {
            logger.error("Failed to load student dashboard", e);
            ModernToast.error("Failed to load student dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Handle admin role selection - show login form.
     */
    @FXML
    private void handleAdminRole() {
        logger.info("Admin role selected - showing login form");
        
        // Hide role selection, show login form
        roleSelectionBox.setVisible(false);
        roleSelectionBox.setManaged(false);
        
        loginFormBox.setVisible(true);
        loginFormBox.setManaged(true);
        
        demoCredentialsBox.setVisible(true);
        demoCredentialsBox.setManaged(true);
        
        // Update welcome text
        if (welcomeTitle != null) {
            welcomeTitle.setText("Admin Login");
        }
        if (welcomeSubtitle != null) {
            welcomeSubtitle.setText("Enter your credentials to continue");
        }
        
        // Focus on user code field
        Platform.runLater(() -> {
            if (userCodeField != null) {
                userCodeField.requestFocus();
            }
        });
        
        ModernToast.info("Admin login required");
    }
    
    /**
     * Handle back to role selection.
     */
    @FXML
    private void handleBackToRoles() {
        logger.info("Returning to role selection");
        
        // Show role selection, hide login form
        roleSelectionBox.setVisible(true);
        roleSelectionBox.setManaged(true);
        
        loginFormBox.setVisible(false);
        loginFormBox.setManaged(false);
        
        demoCredentialsBox.setVisible(false);
        demoCredentialsBox.setManaged(false);
        
        // Update welcome text
        if (welcomeTitle != null) {
            welcomeTitle.setText("Welcome!");
        }
        if (welcomeSubtitle != null) {
            welcomeSubtitle.setText("Select your role to continue");
        }
        
        // Clear form
        clearForm();
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
            ModernToast.warning("User code is required");
            userCodeField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter your password");
            ModernToast.warning("Password is required");
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
                        ModernToast.success("Login successful! Welcome, " + result.getUser().getFullName());
                        handleSuccessfulLogin(result.getUser());
                    } else {
                        showError(result.getMessage());
                        ModernToast.error(result.getMessage());
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
                
            } catch (Exception e) {
                logger.error("Authentication error", e);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Authentication failed: " + e.getMessage());
                    ModernToast.error("Authentication failed");
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
            ModernToast.error("Failed to load dashboard");
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
        ModernToast.info("Application closing...");
        SessionManager.clear();
        Platform.exit();
    }
}

