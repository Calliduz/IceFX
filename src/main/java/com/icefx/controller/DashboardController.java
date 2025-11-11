package com.icefx.controller;

import com.icefx.config.AppConfig;
import com.icefx.dao.AttendanceDAO;
import com.icefx.dao.UserDAO;
import com.icefx.model.AttendanceLog;
import com.icefx.model.User;
import com.icefx.service.AttendanceService;
import com.icefx.service.CameraService;
import com.icefx.service.FaceRecognitionService;
import com.icefx.util.ModernToast;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
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
    @FXML private Label recognitionDetails;
    @FXML private Label recognitionIcon;
    @FXML private TableView<AttendanceLog> attendanceTable;
    @FXML private TableColumn<AttendanceLog, String> timeColumn;
    @FXML private TableColumn<AttendanceLog, String> nameColumn;
    @FXML private TableColumn<AttendanceLog, String> eventColumn;
    @FXML private TableColumn<AttendanceLog, Double> confidenceColumn;
    @FXML private Label todayCountLabel;
    @FXML private Label weekCountLabel;
    @FXML private Label recordCountLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label statusIndicator;
    @FXML private Label systemStatusIndicator;
    @FXML private VBox recognitionPanel;
    @FXML private VBox cameraOffOverlay;
    @FXML private StackPane loadingOverlay;
    
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
    
    // Configuration (loaded from AppConfig)
    private final int cameraIndex = AppConfig.getInt("camera.index", 0);
    private final int cameraFps = AppConfig.getInt("camera.fps", 30);
    private final String cascadePath = AppConfig.get("recognition.haar.cascade",
        "resources/haar/haarcascade_frontalface_default.xml");
    private final String modelPath = AppConfig.getModelPath();
    
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
            faceRecognitionService = new FaceRecognitionService(userDAO, cascadePath);
            
            // Load trained model
            loadFaceRecognitionModel();
            
            // Initialize camera service
            cameraService = new CameraService(cameraIndex, cameraFps);
            
            // Set up UI bindings
            setupUIBindings();
            
            // Set up attendance table
            setupAttendanceTable();
            
            // Load initial data
            loadTodayAttendance();
            updateStatistics();
            
            // Initial UI state
            stopCameraButton.setDisable(true);
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(false);
            }
            if (cameraOffOverlay != null) {
                cameraOffOverlay.setVisible(true);
            }
            
            // Initialize user info if available
            if (currentUser != null) {
                updateUserDisplay();
            }
            
            logger.info("✅ DashboardController initialized successfully");
            ModernToast.success("Dashboard loaded successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to initialize DashboardController", e);
            ModernToast.error("Failed to initialize dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Load the face recognition model.
     */
    private void loadFaceRecognitionModel() {
        try {
            logger.info("Loading face recognition model from: {}", modelPath);
            faceRecognitionService.loadModel(modelPath);
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
        
        String mainText;
        String detailText;
        String icon;
        
        switch (result.getStatus()) {
            case RECOGNIZED:
                mainText = String.format("Recognized: %s", result.getUserName());
                detailText = String.format("Confidence: %.1f%% - Attendance logged", result.getConfidence());
                icon = "✅";
                break;
                
            case UNKNOWN:
                mainText = "Unknown Face Detected";
                detailText = "No matching profile found in database";
                icon = "❓";
                break;
                
            case LOW_CONFIDENCE:
                mainText = "Low Confidence Match";
                detailText = String.format("Only %.1f%% confident - Verification needed", result.getConfidence());
                icon = "⚠️";
                break;
                
            case DEBOUNCED:
                mainText = "Recently Recognized";
                detailText = "Already logged - Cooldown period active";
                icon = "⏳";
                break;
                
            case NO_FACE:
                mainText = "Scanning for faces...";
                detailText = "Position yourself in front of the camera";
                icon = "🔍";
                break;
                
            default:
                mainText = "Ready for face detection";
                detailText = "System initialized and waiting";
                icon = "😊";
                break;
        }
        
        recognitionLabel.setText(mainText);
        if (recognitionDetails != null) {
            recognitionDetails.setText(detailText);
        }
        if (recognitionIcon != null) {
            recognitionIcon.setText(icon);
        }
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
                        ModernToast.success(String.format(
                            "Welcome, %s! Attendance logged successfully.",
                            recognition.getUserName()
                        ));
                        
                        // Refresh attendance table
                        loadTodayAttendance();
                        updateStatistics();
                        
                    } else if (result.getStatus() == AttendanceService.AttendanceResult.Status.DUPLICATE) {
                        ModernToast.info(String.format(
                            "Welcome back, %s! You already checked in recently.",
                            recognition.getUserName()
                        ));
                    } else {
                        ModernToast.warning("Failed to log attendance: " + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error logging attendance", e);
                Platform.runLater(() -> 
                    ModernToast.error("Failed to log attendance: " + e.getMessage())
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
                    
                    // Update record count
                    if (recordCountLabel != null) {
                        recordCountLabel.setText(String.valueOf(logs.size()));
                    }
                    
                    // Update last update time
                    if (lastUpdateLabel != null) {
                        lastUpdateLabel.setText(java.time.LocalTime.now()
                            .format(DateTimeFormatter.ofPattern("h:mm a")));
                    }
                    
                    logger.info("Loaded {} attendance records for today", logs.size());
                });
                
            } catch (Exception e) {
                logger.error("Error loading attendance records", e);
                Platform.runLater(() -> 
                    ModernToast.error("Failed to load attendance: " + e.getMessage())
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
            // Show loading
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(true);
            }
            if (cameraOffOverlay != null) {
                cameraOffOverlay.setVisible(false);
            }
            
            cameraService.start();
            
            // Hide loading after short delay
            Platform.runLater(() -> {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisible(false);
                }
                
                startCameraButton.setDisable(true);
                stopCameraButton.setDisable(false);
                
                if (statusLabel != null) {
                    statusLabel.setText("Camera active - Face recognition running");
                }
                if (statusIndicator != null) {
                    statusIndicator.setStyle("-fx-text-fill: #4CAF50;");
                }
                
                ModernToast.success("Camera started successfully");
            });
            
        } catch (Exception e) {
            logger.error("Failed to start camera", e);
            Platform.runLater(() -> {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisible(false);
                }
                if (cameraOffOverlay != null) {
                    cameraOffOverlay.setVisible(true);
                }
                ModernToast.error("Failed to start camera: " + e.getMessage());
            });
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
            
            if (cameraOffOverlay != null) {
                cameraOffOverlay.setVisible(true);
            }
            
            if (statusLabel != null) {
                statusLabel.setText("Camera offline - Click 'Start Camera' to begin");
            }
            if (statusIndicator != null) {
                statusIndicator.setStyle("-fx-text-fill: #BDBDBD;");
            }
            
            if (recognitionLabel != null) {
                recognitionLabel.setText("Ready for face detection");
            }
            if (recognitionDetails != null) {
                recognitionDetails.setText("System initialized and waiting");
            }
            if (recognitionIcon != null) {
                recognitionIcon.setText("😊");
            }
            
            ModernToast.info("Camera stopped");
            
        } catch (Exception e) {
            logger.error("Failed to stop camera", e);
            ModernToast.error("Failed to stop camera: " + e.getMessage());
        }
    }
    
    /**
     * Handle refresh button.
     */
    @FXML
    private void handleRefresh() {
        logger.info("Refreshing attendance data...");
        ModernToast.info("Refreshing attendance data...");
        loadTodayAttendance();
        updateStatistics();
    }
    
    /**
     * Set the current logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.info("Current user set: {}", user.getFullName());
        updateUserDisplay();
    }
    
    /**
     * Update user display in header.
     */
    private void updateUserDisplay() {
        if (currentUser == null) return;
        
        Platform.runLater(() -> {
            if (userNameLabel != null) {
                userNameLabel.setText(currentUser.getFullName());
            }
            if (userRoleLabel != null) {
                String role = currentUser.getRole() == User.UserRole.ADMIN ? 
                    "Administrator" : currentUser.getRole().getDisplayName();
                userRoleLabel.setText(role);
            }
        });
    }
    
    /**
     * Handle logout button.
     */
    @FXML
    private void handleLogout() {
        logger.info("Logout requested by user: {}", 
            currentUser != null ? currentUser.getFullName() : "Unknown");
        
        // Stop camera if running
        if (cameraService != null && !startCameraButton.isDisabled()) {
            handleStopCamera();
        }
        
        // Show confirmation toast
        ModernToast.info("Logging out...");
        
        // Small delay to show toast
        Platform.runLater(() -> {
            try {
                // Cleanup
                cleanup();
                
                // Navigate back to login
                javafx.stage.Stage stage = (javafx.stage.Stage) startCameraButton.getScene().getWindow();
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/icefx/view/Login.fxml")
                );
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                stage.setScene(scene);
                stage.setTitle("IceFX - Login");
                
                ModernToast.success("Logged out successfully");
                logger.info("User logged out successfully");
                
            } catch (Exception e) {
                logger.error("Error during logout", e);
                ModernToast.error("Error during logout: " + e.getMessage());
            }
        });
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
}
