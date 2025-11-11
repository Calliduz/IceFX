package com.icefx.controller;

import com.icefx.config.AppConfig;
import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import com.icefx.service.UserService;
import com.icefx.service.FaceRecognitionService;
import com.icefx.util.AuthorizationManager;
import com.icefx.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Admin Panel
 * Handles user management, face training, and system administration
 */
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    // Services
    private final UserService userService;
    private final FaceRecognitionService faceRecognitionService;
    private final UserDAO userDAO;
    
    // User Table
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> userCodeColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    
    // User Form Fields
    @FXML private TextField userCodeField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> statusComboBox;
    
    // Search and Filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterComboBox;
    
    // Buttons
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button trainButton;
    @FXML private Button loadModelButton;
    @FXML private Button saveModelButton;
    
    // Face Training
    @FXML private Label modelStatusLabel;
    @FXML private Label trainingStatusLabel;
    @FXML private ProgressBar trainingProgressBar;
    
    // Statistics
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label adminCountLabel;
    
    // Data
    private ObservableList<User> allUsers;
    private User selectedUser;
    private User currentUser;
    
    /**
     * Constructor with dependency injection
     */
    public AdminController() {
        this.userDAO = new UserDAO();
        this.userService = new UserService(userDAO);
        
        String cascadePath = AppConfig.get("recognition.haar.cascade", 
            "resources/haar/haarcascade_frontalface_default.xml");
        this.faceRecognitionService = new FaceRecognitionService(userDAO, cascadePath);
    }
    
    /**
     * FXML initialization
     */
    @FXML
    public void initialize() {
        logger.info("Initializing AdminController");
        
        // AUTHORIZATION CHECK - Admin panel requires ADMIN role
        if (!AuthorizationManager.requireAdmin("Access Admin Panel")) {
            SessionManager.getCurrentUser().ifPresent(user ->
                logger.warn("Unauthorized access attempt to Admin Panel by user: {}", user)
            );
            // Close the admin window
            Platform.runLater(() -> {
                if (userTable != null && userTable.getScene() != null) {
                    userTable.getScene().getWindow().hide();
                }
            });
            return;
        }
        
        setupUserTable();
        setupFormFields();
        setupSearchAndFilter();
        loadAllUsers();
        updateStatistics();
        updateModelStatus();
        
        // Initially disable update/delete buttons
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    /**
     * Setup user table columns and selection handler
     */
    private void setupUserTable() {
        // Configure columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        userCodeColumn.setCellValueFactory(new PropertyValueFactory<>("userCode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDepartment() != null ? cellData.getValue().getDepartment() : "")
        );
        roleColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRole() != null ? cellData.getValue().getRole().name() : "")
        );
        
        // Status column with custom cell factory for better display
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Active" : "Inactive")
        );
        
        // Selection handler
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedUser = newSelection;
                populateFormWithUser(newSelection);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                selectedUser = null;
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });
        
        // Double-click to edit
        userTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && selectedUser != null) {
                handleUpdate();
            }
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.info("AdminController session bound to {}", user.getUserCode());
    }
    
    /**
     * Setup form fields with options
     */
    private void setupFormFields() {
        // Role options
        roleComboBox.setItems(FXCollections.observableArrayList(
            "ADMIN", "STAFF", "STUDENT"
        ));
        roleComboBox.setValue("STUDENT");
        
        // Status options
        statusComboBox.setItems(FXCollections.observableArrayList(
            "Active", "Inactive"
        ));
        statusComboBox.setValue("Active");
    }
    
    /**
     * Setup search and filter functionality
     */
    private void setupSearchAndFilter() {
        // Role filter options
        roleFilterComboBox.setItems(FXCollections.observableArrayList(
            "All Roles", "ADMIN", "STAFF", "STUDENT"
        ));
        roleFilterComboBox.setValue("All Roles");
        
        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });
        
        // Role filter listener
        roleFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });
    }
    
    /**
     * Load all users from database
     */
    private void loadAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            allUsers = FXCollections.observableArrayList(users);
            userTable.setItems(allUsers);
            logger.info("Loaded {} users", users.size());
        } catch (Exception e) {
            logger.error("Error loading users", e);
            showError("Error", "Failed to load users: " + e.getMessage());
        }
    }
    
    /**
     * Apply search and filter to user table
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String roleFilter = roleFilterComboBox.getValue();
        
        ObservableList<User> filtered = allUsers.filtered(user -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                user.getUserCode().toLowerCase().contains(searchText) ||
                user.getFullName().toLowerCase().contains(searchText) ||
                (user.getDepartment() != null && user.getDepartment().toLowerCase().contains(searchText));
            
            // Role filter
            boolean matchesRole = "All Roles".equals(roleFilter) ||
                user.getRole().name().equals(roleFilter);
            
            return matchesSearch && matchesRole;
        });
        
        userTable.setItems(filtered);
    }
    
    /**
     * Populate form fields with user data
     */
    private void populateFormWithUser(User user) {
        userCodeField.setText(user.getUserCode());
        nameField.setText(user.getFullName());
        emailField.setText(user.getDepartment() != null ? user.getDepartment() : "");
        passwordField.clear(); // Don't show password
        roleComboBox.setValue(user.getRole().name());
        statusComboBox.setValue(user.isActive() ? "Active" : "Inactive");
    }
    
    /**
     * Clear form fields
     */
    @FXML
    private void handleClear() {
        userCodeField.clear();
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        roleComboBox.setValue("STUDENT");
        statusComboBox.setValue("Active");
        
        userTable.getSelectionModel().clearSelection();
        selectedUser = null;
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    /**
     * Add new user
     */
    @FXML
    private void handleAdd() {
        // Validate input
        if (!validateInput()) {
            return;
        }
        
        // Check if password is provided for new user
        if (passwordField.getText().trim().isEmpty()) {
            showError("Validation Error", "Password is required for new users");
            return;
        }
        
        // Create new user
        String userCode = userCodeField.getText().trim();
        String fullName = nameField.getText().trim();
        String department = emailField.getText().trim().isEmpty() ? null : emailField.getText().trim();
        String position = null; // Can be extended to have a position field
        User.UserRole role = User.UserRole.valueOf(roleComboBox.getValue());
        boolean isActive = "Active".equals(statusComboBox.getValue());
        
        // Run in background thread
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                User created = userService.createUser(userCode, fullName, department, position, role, passwordField.getText());
                created.setActive(isActive);
                if (!isActive) {
                    userService.updateUser(created);
                }
                return created;
            }
            
            @Override
            protected void succeeded() {
                User created = getValue();
                allUsers.add(created);
                userTable.refresh();
                updateStatistics();
                handleClear();
                showInfo("Success", "User created successfully: " + created.getUserCode());
                logger.info("Created user: {}", created.getUserCode());
            }
            
            @Override
            protected void failed() {
                Throwable e = getException();
                logger.error("Error creating user", e);
                showError("Error", "Failed to create user: " + e.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Update selected user
     */
    @FXML
    private void handleUpdate() {
        // AUTHORIZATION CHECK
        if (!AuthorizationManager.requireAdmin("Update User")) {
            return;
        }
        
        if (selectedUser == null) {
            showError("Selection Error", "Please select a user to update");
            return;
        }
        
        // Validate input
        if (!validateInput()) {
            return;
        }
        
        // Update user object
        selectedUser.setUserCode(userCodeField.getText().trim());
        selectedUser.setFullName(nameField.getText().trim());
        selectedUser.setDepartment(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
        selectedUser.setRole(User.UserRole.valueOf(roleComboBox.getValue()));
        selectedUser.setActive("Active".equals(statusComboBox.getValue()));
        
        // Run in background thread
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                User updated = userService.updateUser(selectedUser);
                
                // Update password if provided
                String newPassword = passwordField.getText().trim();
                if (!newPassword.isEmpty()) {
                    userService.resetPassword(selectedUser.getUserId(), newPassword);
                }
                
                return updated;
            }
            
            @Override
            protected void succeeded() {
                userTable.refresh();
                updateStatistics();
                showInfo("Success", "User updated successfully");
                logger.info("Updated user: {}", selectedUser.getUserCode());
            }
            
            @Override
            protected void failed() {
                Throwable e = getException();
                logger.error("Error updating user", e);
                showError("Error", "Failed to update user: " + e.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Delete selected user
     */
    @FXML
    private void handleDelete() {
        // AUTHORIZATION CHECK
        if (!AuthorizationManager.requireAdmin("Delete User")) {
            return;
        }
        
        if (selectedUser == null) {
            showError("Selection Error", "Please select a user to delete");
            return;
        }
        
        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete User: " + selectedUser.getFullName());
        alert.setContentText("Are you sure you want to delete this user? This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int userId = selectedUser.getUserId();
            
            // Run in background thread
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    userService.deleteUser(userId);
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    allUsers.remove(selectedUser);
                    userTable.refresh();
                    updateStatistics();
                    handleClear();
                    showInfo("Success", "User deleted successfully");
                    logger.info("Deleted user ID: {}", userId);
                }
                
                @Override
                protected void failed() {
                    Throwable e = getException();
                    logger.error("Error deleting user", e);
                    showError("Error", "Failed to delete user: " + e.getMessage());
                }
            };
            
            new Thread(task).start();
        }
    }
    
    /**
     * Refresh user list
     */
    @FXML
    private void handleRefresh() {
        loadAllUsers();
        updateStatistics();
        handleClear();
        showInfo("Refreshed", "User list refreshed successfully");
    }
    
    /**
     * Train face recognition model
     */
    @FXML
    private void handleTrainModel() {
        // AUTHORIZATION CHECK
        if (!AuthorizationManager.requireAdmin("Train Face Recognition Model")) {
            return;
        }
        
        // Choose directory with face images
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Faces Directory");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        File facesDir = dirChooser.showDialog(trainButton.getScene().getWindow());
        if (facesDir == null) {
            return;
        }
        
        // Disable button during training
        trainButton.setDisable(true);
        trainingProgressBar.setVisible(true);
        trainingProgressBar.setProgress(-1); // Indeterminate
        trainingStatusLabel.setText("Training in progress...");
        
        // Run training in background
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                faceRecognitionService.trainFromDirectory(facesDir.getAbsolutePath());
                return null;
            }
            
            @Override
            protected void succeeded() {
                trainingProgressBar.setVisible(false);
                trainingStatusLabel.setText("Training completed successfully");
                trainButton.setDisable(false);
                updateModelStatus();
                showInfo("Success", "Face recognition model trained successfully");
                logger.info("Model trained from: {}", facesDir.getAbsolutePath());
            }
            
            @Override
            protected void failed() {
                Throwable e = getException();
                trainingProgressBar.setVisible(false);
                trainingStatusLabel.setText("Training failed");
                trainButton.setDisable(false);
                logger.error("Error training model", e);
                showError("Error", "Failed to train model: " + e.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Load face recognition model
     */
    @FXML
    private void handleLoadModel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Face Recognition Model");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        
        File modelFile = fileChooser.showOpenDialog(loadModelButton.getScene().getWindow());
        if (modelFile == null) {
            return;
        }
        
        try {
            faceRecognitionService.loadModel(modelFile.getAbsolutePath());
            updateModelStatus();
            showInfo("Success", "Model loaded successfully");
            logger.info("Model loaded from: {}", modelFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error loading model", e);
            showError("Error", "Failed to load model: " + e.getMessage());
        }
    }
    
    /**
     * Save face recognition model
     */
    @FXML
    private void handleSaveModel() {
        if (!faceRecognitionService.isTrained()) {
            showError("Error", "No trained model to save. Please train a model first.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Face Recognition Model");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.setInitialFileName("trained_faces.xml");
        
        File modelFile = fileChooser.showSaveDialog(saveModelButton.getScene().getWindow());
        if (modelFile == null) {
            return;
        }
        
        try {
            faceRecognitionService.saveModel(modelFile.getAbsolutePath());
            showInfo("Success", "Model saved successfully");
            logger.info("Model saved to: {}", modelFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error saving model", e);
            showError("Error", "Failed to save model: " + e.getMessage());
        }
    }
    
    /**
     * Update model status label
     */
    private void updateModelStatus() {
        if (faceRecognitionService.isTrained()) {
            modelStatusLabel.setText("✓ Model Trained");
            modelStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            modelStatusLabel.setText("✗ No Model");
            modelStatusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
        }
    }
    
    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        try {
            List<User> users = userService.getAllUsers();
            totalUsersLabel.setText(String.valueOf(users.size()));
            
            long activeCount = users.stream().filter(User::isActive).count();
            activeUsersLabel.setText(String.valueOf(activeCount));
            
            long adminCount = users.stream().filter(u -> u.getRole() == User.UserRole.ADMIN).count();
            adminCountLabel.setText(String.valueOf(adminCount));
        } catch (Exception e) {
            logger.error("Error updating statistics", e);
        }
    }
    
    /**
     * Validate form input
     */
    private boolean validateInput() {
        if (userCodeField.getText().trim().isEmpty()) {
            showError("Validation Error", "User Code is required");
            return false;
        }
        
        if (nameField.getText().trim().isEmpty()) {
            showError("Validation Error", "Name is required");
            return false;
        }
        
        if (roleComboBox.getValue() == null) {
            showError("Validation Error", "Role is required");
            return false;
        }
        
        return true;
    }
    
    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
