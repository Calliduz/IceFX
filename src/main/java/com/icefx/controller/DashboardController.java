package com.icefx.controller;

import com.icefx.dao.AttendanceDAO;
import com.icefx.dao.UserDAO;
import com.icefx.model.AttendanceLog;
import com.icefx.model.User;
import com.icefx.service.AttendanceService;
import com.icefx.service.CameraService;
import com.icefx.service.FaceRecognitionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the main dashboard with camera and face recognition.
 * 
 * Features:
 * - Live camera feed
 * - Real-time face detection and recognition
 * - Automatic attendance logging
 * - Recent attendance display
 * - Statistics overview
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    // UI Components
    @FXML private ImageView cameraView;
    @FXML private Button startCameraButton;
    @FXML private Button stopCameraButton;
    @FXML private Label statusLabel;
    @FXML private Label fpsLabel;
    @FXML private Label recognitionLabel;
    @FXML private TableView<AttendanceLog> attendanceTable;
    @FXML private TableColumn<AttendanceLog, String> timeColumn;
    @FXML private TableColumn<AttendanceLog, String> nameColumn;
    @FXML private TableColumn<AttendanceLog, String> eventColumn;
    @FXML private TableColumn<AttendanceLog, Double> confidenceColumn;
    @FXML private Label todayCountLabel;
    @FXML private Label weekCountLabel;
    @FXML private VBox recognitionPanel;
    @FXML private ProgressIndicator loadingIndicator;
    
    // Services
    private CameraService cameraService;
    private FaceRecognitionService faceRecognitionService;
    private AttendanceService attendanceService;
    
    // DAOs
    private UserDAO userDAO;
    private AttendanceDAO attendanceDAO;
    
    // Data
    private ObservableList<AttendanceLog> attendanceData;
    private User currentUser; // The logged-in user
    
    // Configuration
    private static final int CAMERA_INDEX = 0;
    private static final int CAMERA_FPS = 30;
    private static final String CASCADE_PATH = "resources/haar/haarcascade_frontalface_default.xml";
    private static final String MODEL_PATH = "resources/trained_faces.xml";
    
    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        logger.info("DashboardController initializing...");
        
        try {
            // Initialize DAOs
            userDAO = new UserDAO();
            attendanceDAO = new AttendanceDAO();
            
            // Initialize services
            attendanceService = new AttendanceService(attendanceDAO, userDAO);
            faceRecognitionService = new FaceRecognitionService(userDAO, CASCADE_PATH);
            
            // Load trained model
            loadFaceRecognitionModel();
            
            // Initialize camera service
            cameraService = new CameraService(CAMERA_INDEX, CAMERA_FPS);
            
            // Set up UI bindings
            setupUIBindings();
            
            // Set up attendance table
            setupAttendanceTable();
            
            // Load initial data
            loadTodayAttendance();
            updateStatistics();
            
            // Initial UI state
            stopCameraButton.setDisable(true);
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
            
            logger.info("✅ DashboardController initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize DashboardController", e);
            showError("Failed to initialize dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Load the face recognition model.
     */
    private void loadFaceRecognitionModel() {
        try {
            logger.info("Loading face recognition model from: {}", MODEL_PATH);
            faceRecognitionService.loadModel(MODEL_PATH);
            logger.info("✅ Face recognition model loaded successfully");
        } catch (Exception e) {
            logger.warn("Failed to load face recognition model: {}", e.getMessage());
            logger.info("Model can be trained later from the Admin panel");
        }
    }
    
    /**
     * Set up UI bindings with camera service.
     */
    private void setupUIBindings() {
        // Bind camera feed to ImageView
        if (cameraView != null) {
            cameraView.imageProperty().bind(cameraService.currentFrameProperty());
            cameraView.setPreserveRatio(true);
        }
        
        // Bind status label
        if (statusLabel != null) {
            cameraService.statusTextProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> statusLabel.setText(newVal));
            });
        }
        
        // Bind FPS label
        if (fpsLabel != null) {
            cameraService.fpsProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> 
                    fpsLabel.setText(String.format("FPS: %.1f", newVal.doubleValue()))
                );
            });
        }
        
        // Set up frame processing callback
        cameraService.setFrameCallback(this::processFrame);
    }
    
    /**
     * Set up the attendance table columns.
     */
    private void setupAttendanceTable() {
        if (attendanceTable == null) return;
        
        attendanceData = FXCollections.observableArrayList();
        attendanceTable.setItems(attendanceData);
        
        // Time column
        if (timeColumn != null) {
            timeColumn.setCellValueFactory(cellData -> {
                String time = cellData.getValue().getEventTime()
                    .format(DateTimeFormatter.ofPattern("hh:mm a"));
                return new javafx.beans.property.SimpleStringProperty(time);
            });
        }
        
        // Name column
        if (nameColumn != null) {
            nameColumn.setCellValueFactory(cellData -> 
                cellData.getValue().userNameProperty()
            );
        }
        
        // Event type column
        if (eventColumn != null) {
            eventColumn.setCellValueFactory(cellData -> 
                cellData.getValue().eventTypeProperty()
            );
        }
        
        // Confidence column
        if (confidenceColumn != null) {
            confidenceColumn.setCellValueFactory(cellData -> 
                cellData.getValue().confidenceProperty().asObject()
            );
            
            // Format confidence as percentage
            confidenceColumn.setCellFactory(col -> new TableCell<AttendanceLog, Double>() {
                @Override
                protected void updateItem(Double confidence, boolean empty) {
                    super.updateItem(confidence, empty);
                    if (empty || confidence == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.1f%%", confidence));
                    }
                }
            });
        }
    }
    
    /**
     * Process each camera frame for face recognition.
     */
    private void processFrame(Mat frame) {
        try {
            // Perform face detection and recognition
            FaceRecognitionService.RecognitionResult result = 
                faceRecognitionService.detectAndRecognize(frame);
            
            // Update UI with recognition result
            Platform.runLater(() -> updateRecognitionDisplay(result));
            
            // Log attendance if user is recognized
            if (result.shouldLogAttendance()) {
                logAttendance(result);
            }
            
        } catch (Exception e) {
            logger.error("Error processing frame", e);
        }
    }
    
    /**
     * Update the recognition display panel.
     */
    private void updateRecognitionDisplay(FaceRecognitionService.RecognitionResult result) {
        if (recognitionLabel == null) return;
        
        String message;
        String style;
        
        switch (result.getStatus()) {
            case RECOGNIZED:
                message = String.format("✅ Recognized: %s\nConfidence: %.1f%%", 
                    result.getUserName(), result.getConfidence());
                style = "-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 16px;";
                break;
                
            case UNKNOWN:
                message = "👤 Unknown Face Detected";
                style = "-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 16px;";
                break;
                
            case LOW_CONFIDENCE:
                message = String.format("⚠️ Low Confidence: %.1f%%", result.getConfidence());
                style = "-fx-text-fill: #FFC107; -fx-font-weight: bold; -fx-font-size: 16px;";
                break;
                
            case DEBOUNCED:
                message = "⏳ Recently Recognized";
                style = "-fx-text-fill: #2196F3; -fx-font-weight: bold; -fx-font-size: 16px;";
                break;
                
            case NO_FACE:
                message = "🔍 Scanning...";
                style = "-fx-text-fill: #757575; -fx-font-size: 14px;";
                break;
                
            default:
                message = "Ready";
                style = "-fx-text-fill: #757575;";
                break;
        }
        
        recognitionLabel.setText(message);
        recognitionLabel.setStyle(style);
    }
    
    /**
     * Log attendance for recognized user.
     */
    private void logAttendance(FaceRecognitionService.RecognitionResult recognition) {
        new Thread(() -> {
            try {
                logger.info("Logging attendance for user: {} (confidence: {})", 
                    recognition.getUserName(), recognition.getConfidence());
                
                AttendanceService.AttendanceResult result = 
                    attendanceService.logAttendance(
                        recognition.getUserId(), 
                        recognition.getConfidence()
                    );
                
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        showSuccess(String.format(
                            "✅ Welcome, %s!\nAttendance logged successfully.",
                            recognition.getUserName()
                        ));
                        
                        // Refresh attendance table
                        loadTodayAttendance();
                        updateStatistics();
                        
                    } else if (result.getStatus() == AttendanceService.AttendanceResult.Status.DUPLICATE) {
                        showInfo(String.format(
                            "👋 Welcome back, %s!\nYou already checked in recently.",
                            recognition.getUserName()
                        ));
                    } else {
                        showWarning("Failed to log attendance: " + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error logging attendance", e);
                Platform.runLater(() -> 
                    showError("Failed to log attendance: " + e.getMessage())
                );
            }
        }).start();
    }
    
    /**
     * Load today's attendance records.
     */
    private void loadTodayAttendance() {
        new Thread(() -> {
            try {
                List<AttendanceLog> logs = attendanceService.getTodayAttendance();
                
                Platform.runLater(() -> {
                    attendanceData.clear();
                    attendanceData.addAll(logs);
                    logger.info("Loaded {} attendance records for today", logs.size());
                });
                
            } catch (Exception e) {
                logger.error("Error loading attendance records", e);
                Platform.runLater(() -> 
                    showError("Failed to load attendance: " + e.getMessage())
                );
            }
        }).start();
    }
    
    /**
     * Update statistics labels.
     */
    private void updateStatistics() {
        new Thread(() -> {
            try {
                // Today's count
                List<AttendanceLog> todayLogs = attendanceService.getTodayAttendance();
                int todayCount = todayLogs.size();
                
                // Week's count
                LocalDate weekStart = LocalDate.now().minusDays(7);
                LocalDate weekEnd = LocalDate.now();
                List<AttendanceLog> weekLogs = 
                    attendanceService.getAttendanceByDateRange(weekStart, weekEnd);
                int weekCount = weekLogs.size();
                
                Platform.runLater(() -> {
                    if (todayCountLabel != null) {
                        todayCountLabel.setText(String.valueOf(todayCount));
                    }
                    if (weekCountLabel != null) {
                        weekCountLabel.setText(String.valueOf(weekCount));
                    }
                });
                
            } catch (SQLException e) {
                logger.error("Error updating statistics", e);
            }
        }).start();
    }
    
    /**
     * Handle start camera button.
     */
    @FXML
    private void handleStartCamera() {
        logger.info("Starting camera...");
        
        try {
            cameraService.start();
            
            startCameraButton.setDisable(true);
            stopCameraButton.setDisable(false);
            
            if (statusLabel != null) {
                statusLabel.setText("Camera started - Face recognition active");
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            }
            
        } catch (Exception e) {
            logger.error("Failed to start camera", e);
            showError("Failed to start camera: " + e.getMessage());
        }
    }
    
    /**
     * Handle stop camera button.
     */
    @FXML
    private void handleStopCamera() {
        logger.info("Stopping camera...");
        
        try {
            cameraService.stop();
            
            startCameraButton.setDisable(false);
            stopCameraButton.setDisable(true);
            
            if (statusLabel != null) {
                statusLabel.setText("Camera stopped");
                statusLabel.setStyle("-fx-text-fill: #757575;");
            }
            
            if (recognitionLabel != null) {
                recognitionLabel.setText("Camera is off");
            }
            
        } catch (Exception e) {
            logger.error("Failed to stop camera", e);
            showError("Failed to stop camera: " + e.getMessage());
        }
    }
    
    /**
     * Handle refresh button.
     */
    @FXML
    private void handleRefresh() {
        logger.info("Refreshing attendance data...");
        loadTodayAttendance();
        updateStatistics();
    }
    
    /**
     * Set the current logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.info("Current user set: {}", user.getFullName());
    }
    
    /**
     * Cleanup when controller is destroyed.
     */
    public void cleanup() {
        logger.info("Cleaning up DashboardController...");
        
        if (cameraService != null) {
            cameraService.stop();
        }
        
        logger.info("DashboardController cleanup complete");
    }
    
    // UI Helper Methods
    
    private void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message);
    }
    
    private void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Information", message);
    }
    
    private void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, "Warning", message);
    }
    
    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Error", message);
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
