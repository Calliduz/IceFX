package com.icefx.controller;

import com.icefx.config.AppConfig;
import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import com.icefx.service.CameraService;
import com.icefx.service.FaceRecognitionService;
import com.icefx.service.FaceRegistrationService;
import com.icefx.service.FaceRegistrationService.CaptureAngle;
import com.icefx.service.FaceRegistrationService.QualityResult;
import com.icefx.util.ModernToast;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Face Registration window.
 * 
 * Features:
 * - Select user to register
 * - Live camera preview with face quality feedback
 * - Capture multiple photos with angle guidance
 * - Show captured photos in grid
 * - Delete individual photos
 * - Train model with captured faces
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class FaceRegistrationController {
    private static final Logger logger = LoggerFactory.getLogger(FaceRegistrationController.class);
    
    // Services
    private FaceRegistrationService registrationService;
    private FaceRecognitionService recognitionService;
    private CameraService cameraService;
    private UserDAO userDAO;
    
    // UI Components
    @FXML private Label selectedUserLabel;
    @FXML private ImageView cameraView;
    @FXML private Label qualityLabel;
    @FXML private Label qualityIcon;
    @FXML private Label captureCountLabel;
    @FXML private Label angleGuideLabel;
    @FXML private Button startCameraButton;
    @FXML private Button stopCameraButton;
    @FXML private Button captureButton;
    @FXML private Button trainButton;
    @FXML private Button closeButton;
    @FXML private FlowPane photosGrid;
    @FXML private ProgressBar trainingProgress;
    @FXML private Label trainingStatusLabel;
    @FXML private VBox trainingBox;
    @FXML private StackPane cameraPane;
    @FXML private CheckBox autoCaptureCheckbox;
    @FXML private VBox cameraOffOverlay;
    
    // State
    private User selectedUser;
    private List<String> capturedPhotos = new ArrayList<>();
    private int currentAngleIndex = 0;
    private List<CaptureAngle> recommendedAngles;
    private FrameGrabber grabber;
    private JavaFXFrameConverter converter = new JavaFXFrameConverter();
    private volatile boolean cameraRunning = false;
    private Thread cameraThread;
    private Mat currentFrame;
    private long lastCaptureTime = 0;
    private static final long CAPTURE_COOLDOWN_MS = 1000; // 1 second cooldown
    private boolean autoCaptureEnabled = false;
    private long lastAutoCaptureTime = 0;
    private static final long AUTO_CAPTURE_COOLDOWN_MS = 1500; // 1.5 seconds for auto-capture
    private int stableFrameCount = 0; // Count frames with stable pose
    private static final int STABLE_FRAMES_REQUIRED = 5; // ~0.17 seconds at 30 FPS - faster response!
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing FaceRegistrationController");
        
        try {
            // Initialize services
            userDAO = new UserDAO();
            
            String cascadePath = AppConfig.get("recognition.haar.cascade",
                "/haar/haarcascade_frontalface_default.xml");
            String facesDir = AppConfig.get("faces.directory", "faces");
            
            registrationService = new FaceRegistrationService(facesDir, cascadePath);
            recognitionService = new FaceRecognitionService(userDAO, cascadePath);
            
            // Setup UI
            setupAngleGuidance();
            captureButton.setDisable(true);
            stopCameraButton.setDisable(true);
            trainButton.setDisable(true);
            trainingBox.setVisible(false);
            
            logger.info("FaceRegistrationController initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize FaceRegistrationController", e);
            ModernToast.error("Initialization failed: " + e.getMessage());
        }
    }
    
    /**
     * Set the user to register (called from AdminController)
     */
    public void setUser(User user) {
        this.selectedUser = user;
        if (user != null) {
            logger.info("User set for registration: {}", user.getUserCode());
            
            // Update label
            if (selectedUserLabel != null) {
                selectedUserLabel.setText(String.format("%s - %s", user.getUserCode(), user.getFullName()));
            }
            
            // Load existing photos
            loadExistingPhotos();
            updateCaptureCount();
            updateTrainButtonState();
            
            // Enable camera button
            startCameraButton.setDisable(false);
        }
    }
    
    /**
     * Setup angle guidance system
     */
    private void setupAngleGuidance() {
        recommendedAngles = FaceRegistrationService.getRecommendedAngles();
        updateAngleGuide();
    }
    
    /**
     * Update angle guidance label
     */
    private void updateAngleGuide() {
        if (currentAngleIndex < recommendedAngles.size()) {
            CaptureAngle angle = recommendedAngles.get(currentAngleIndex);
            angleGuideLabel.setText(String.format("📸 %s (%d/%d)", 
                angle.getInstruction(), 
                currentAngleIndex + 1, 
                recommendedAngles.size()));
        } else {
            angleGuideLabel.setText("✅ All recommended angles captured!");
        }
    }
    
    /**
     * Load existing photos for selected user
     */
    private void loadExistingPhotos() {
        if (selectedUser == null) return;
        
        capturedPhotos = registrationService.getUserFaces(selectedUser.getUserId());
        updatePhotosGrid();
        updateTrainButtonState();
        
        logger.info("Loaded {} existing photos for user {}", 
            capturedPhotos.size(), selectedUser.getUserCode());
    }
    
    /**
     * Update photos grid display
     */
    private void updatePhotosGrid() {
        photosGrid.getChildren().clear();
        
        for (String photoPath : capturedPhotos) {
            VBox photoCard = createPhotoCard(photoPath);
            photosGrid.getChildren().add(photoCard);
        }
    }
    
    /**
     * Create a photo card with image and delete button
     */
    private VBox createPhotoCard(String photoPath) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #2C2C2C; -fx-padding: 8; -fx-background-radius: 8;");
        
        // Image
        ImageView imageView = new ImageView();
        try {
            Image image = new Image("file:" + photoPath);
            imageView.setImage(image);
            imageView.setFitWidth(100);
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            logger.error("Failed to load image: {}", photoPath, e);
        }
        
        // Delete button
        Button deleteBtn = new Button("❌");
        deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 10;");
        deleteBtn.setOnAction(e -> handleDeletePhoto(photoPath));
        
        card.getChildren().addAll(imageView, deleteBtn);
        return card;
    }
    
    /**
     * Handle delete photo
     */
    private void handleDeletePhoto(String photoPath) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Photo");
        confirm.setHeaderText("Delete this photo?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (registrationService.deleteFace(photoPath)) {
                    capturedPhotos.remove(photoPath);
                    updatePhotosGrid();
                    updateCaptureCount();
                    updateTrainButtonState();
                    ModernToast.success("Photo deleted");
                } else {
                    ModernToast.error("Failed to delete photo");
                }
            }
        });
    }
    
    /**
     * Update capture count label
     */
    private void updateCaptureCount() {
        int count = capturedPhotos.size();
        String status;
        
        if (count < FaceRegistrationService.MINIMUM_PHOTOS) {
            status = String.format("⚠️ %d/%d photos (Need %d more)", 
                count, 
                FaceRegistrationService.RECOMMENDED_PHOTOS,
                FaceRegistrationService.MINIMUM_PHOTOS - count);
        } else if (count < FaceRegistrationService.RECOMMENDED_PHOTOS) {
            status = String.format("✓ %d/%d photos (Ready to train, but %d more recommended)", 
                count,
                FaceRegistrationService.RECOMMENDED_PHOTOS,
                FaceRegistrationService.RECOMMENDED_PHOTOS - count);
        } else {
            status = String.format("✅ %d photos captured (Excellent!)", count);
        }
        
        captureCountLabel.setText(status);
    }
    
    /**
     * Update train button enabled state
     */
    private void updateTrainButtonState() {
        if (selectedUser != null) {
            boolean hasEnough = registrationService.hasEnoughFaces(selectedUser.getUserId());
            trainButton.setDisable(!hasEnough);
        }
    }
    
    /**
     * Start camera
     */
    @FXML
    private void handleStartCamera() {
        if (selectedUser == null) {
            ModernToast.warning("Please select a user first");
            return;
        }
        
        try {
            logger.info("Starting camera for face registration");
            
            grabber = new OpenCVFrameGrabber(AppConfig.getCameraIndex());
            grabber.setImageWidth(AppConfig.getInt("camera.width", 640));
            grabber.setImageHeight(AppConfig.getInt("camera.height", 480));
            grabber.setFrameRate(AppConfig.getInt("camera.fps", 30));
            grabber.start();
            
            cameraRunning = true;
            startCameraButton.setDisable(true);
            stopCameraButton.setDisable(false);
            captureButton.setDisable(false);
            
            // Start camera loop
            cameraThread = new Thread(this::cameraLoop, "FaceRegistration-Camera");
            cameraThread.setDaemon(true);
            cameraThread.start();
            
            ModernToast.success("Camera started");
            
        } catch (Exception e) {
            logger.error("Failed to start camera", e);
            ModernToast.error("Failed to start camera: " + e.getMessage());
            cameraRunning = false;
        }
    }
    
    /**
     * Camera capture loop
     */
    private void cameraLoop() {
        logger.info("Camera loop started");
        
        while (cameraRunning) {
            try {
                Frame frame = grabber.grab();
                if (frame == null || frame.imageWidth == 0) {
                    Thread.sleep(50);
                    continue;
                }
                
                // Convert to Mat for quality check
                currentFrame = convertFrameToMat(frame);
                
                // Mirror the frame horizontally for intuitive display
                if (currentFrame != null) {
                    Mat flippedFrame = new Mat();
                    opencv_core.flip(currentFrame, flippedFrame, 1); // 1 = horizontal flip
                    
                    // Update currentFrame to flipped version
                    currentFrame.release();
                    currentFrame = flippedFrame;
                }
                
                // Convert flipped Mat back to Frame for JavaFX display
                org.bytedeco.javacv.OpenCVFrameConverter.ToMat matConverter = 
                    new org.bytedeco.javacv.OpenCVFrameConverter.ToMat();
                Frame flippedFrameForDisplay = matConverter.convert(currentFrame);
                
                // Convert to JavaFX Image for display
                Image image = converter.convert(flippedFrameForDisplay);
                
                // Update UI
                Platform.runLater(() -> {
                    cameraView.setImage(image);
                    
                    // Hide camera overlay when camera starts
                    if (cameraOffOverlay != null) {
                        cameraOffOverlay.setVisible(false);
                    }
                    
                    // Check face quality
                    if (currentFrame != null) {
                        QualityResult quality = registrationService.validateFaceQuality(currentFrame);
                        updateQualityDisplay(quality);
                        
                        // Auto-capture logic if enabled
                        if (autoCaptureEnabled && quality.isPassed()) {
                            tryAutoCapture(quality);
                        }
                    }
                });
                
                Thread.sleep(33); // ~30 FPS
                
            } catch (InterruptedException e) {
                logger.info("Camera loop interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error in camera loop", e);
            }
        }
        
        logger.info("Camera loop stopped");
    }
    
    /**
     * Convert Frame to Mat (simplified version)
     */
    private Mat convertFrameToMat(Frame frame) {
        try {
            org.bytedeco.javacv.OpenCVFrameConverter.ToMat matConverter = 
                new org.bytedeco.javacv.OpenCVFrameConverter.ToMat();
            return matConverter.convert(frame);
        } catch (Exception e) {
            logger.error("Failed to convert frame to Mat", e);
            return null;
        }
    }
    
    /**
     * Update quality display
     */
    private void updateQualityDisplay(QualityResult quality) {
        if (quality.isPassed()) {
            qualityLabel.setText(quality.getMessage());
            qualityLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            qualityIcon.setText("✅");
            
            // Only enable capture button if auto-capture is off
            if (!autoCaptureEnabled) {
                captureButton.setDisable(false);
            }
        } else {
            qualityLabel.setText(quality.getMessage());
            qualityLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            qualityIcon.setText("❌");
            captureButton.setDisable(true);
        }
    }
    
    /**
     * Try to auto-capture photo based on current angle and face position.
     * Uses face position detection to determine head pose.
     */
    private void tryAutoCapture(QualityResult quality) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoCaptureTime < AUTO_CAPTURE_COOLDOWN_MS) {
            stableFrameCount = 0; // Reset stability counter during cooldown
            return;
        }
        
        // Check if we still need more photos
        if (currentAngleIndex >= recommendedAngles.size()) {
            return; // All angles captured
        }
        
        // Get current recommended angle
        CaptureAngle angle = recommendedAngles.get(currentAngleIndex);
        
        // Detect if current pose matches recommended angle
        boolean poseMatches = detectPoseMatch(angle, quality);
        
        if (poseMatches) {
            stableFrameCount++;
            
            // Update UI to show pose is matching
            Platform.runLater(() -> {
                angleGuideLabel.setText(String.format("✅ %s - Hold still! (%d/%d)", 
                    angle.getInstruction(), 
                    currentAngleIndex + 1,
                    recommendedAngles.size()));
            });
            
            // Capture only after pose is stable
            if (stableFrameCount >= STABLE_FRAMES_REQUIRED) {
                logger.info("Auto-capturing for angle: {} (stable for {} frames)", 
                    angle.getInstruction(), stableFrameCount);
                
                // Capture face
                String savedPath = registrationService.captureFace(currentFrame, selectedUser, angle);
                
                if (savedPath != null) {
                    lastAutoCaptureTime = currentTime;
                    stableFrameCount = 0; // Reset for next angle
                    
                    Platform.runLater(() -> {
                        capturedPhotos.add(savedPath);
                        updatePhotosGrid();
                        updateCaptureCount();
                        updateTrainButtonState();
                        
                        // Move to next angle
                        currentAngleIndex++;
                        updateAngleGuide();
                        
                        ModernToast.success("🤖 Auto-captured! " + capturedPhotos.size() + "/" + FaceRegistrationService.RECOMMENDED_PHOTOS);
                        logger.info("Auto-captured photo {}: {}", capturedPhotos.size(), savedPath);
                        
                        // Auto-train and close if recommended number reached
                        if (capturedPhotos.size() >= FaceRegistrationService.RECOMMENDED_PHOTOS) {
                            logger.info("Reached recommended photo count via auto-capture. Auto-training model...");
                            ModernToast.info("✅ All photos captured! Auto-training model...");
                            
                            // Stop camera first
                            handleStopCamera();
                            
                            // Auto-train after short delay
                            new Thread(() -> {
                                try {
                                    Thread.sleep(500);
                                    Platform.runLater(this::performAutoTraining);
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }
                    });
                }
            }
        } else {
            // Reset stability if pose doesn't match
            stableFrameCount = 0;
        }
    }
    
    /**
     * Detect if current face pose matches the recommended angle.
     * Uses face bounding box position to estimate head orientation.
     */
    private boolean detectPoseMatch(CaptureAngle angle, QualityResult quality) {
        if (currentFrame == null || !quality.isPassed()) {
            return false;
        }
        
        try {
            // Detect face using the cascade classifier
            org.bytedeco.opencv.opencv_objdetect.CascadeClassifier detector = 
                new org.bytedeco.opencv.opencv_objdetect.CascadeClassifier();
            
            // Use the same cascade file as FaceRegistrationService
            String cascadePath = "/haar/haarcascade_frontalface_default.xml";
            java.io.InputStream cascadeStream = getClass().getResourceAsStream(cascadePath);
            
            if (cascadeStream != null) {
                // Extract to temp file
                java.io.File tempCascade = java.io.File.createTempFile("haarcascade", ".xml");
                tempCascade.deleteOnExit();
                java.nio.file.Files.copy(cascadeStream, tempCascade.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                cascadeStream.close();
                
                detector.load(tempCascade.getAbsolutePath());
            }
            
            // Detect faces
            org.bytedeco.opencv.opencv_core.RectVector faces = 
                new org.bytedeco.opencv.opencv_core.RectVector();
            detector.detectMultiScale(currentFrame, faces);
            
            if (faces.size() == 0) {
                return false; // No face detected
            }
            
            // Get the first (largest) face
            org.bytedeco.opencv.opencv_core.Rect faceRect = faces.get(0);
            
            // Calculate face position relative to frame
            int frameWidth = currentFrame.cols();
            int frameHeight = currentFrame.rows();
            
            int faceCenterX = faceRect.x() + faceRect.width() / 2;
            int faceCenterY = faceRect.y() + faceRect.height() / 2;
            
            // Calculate relative position (0.0 to 1.0)
            double relativeX = (double) faceCenterX / frameWidth;
            double relativeY = (double) faceCenterY / frameHeight;
            
            // Determine pose based on face position and angle requirement
            // Made thresholds more lenient for easier detection
            switch (angle) {
                case FRONT:
                    // Face should be centered - wider range for easier detection
                    return relativeX > 0.35 && relativeX < 0.65 && 
                           relativeY > 0.35 && relativeY < 0.65;
                    
                case LEFT:
                    // Face should be on the right side of frame (user turned left)
                    // Lowered threshold for easier detection
                    return relativeX > 0.52;
                    
                case RIGHT:
                    // Face should be on the left side of frame (user turned right)
                    // Raised threshold for easier detection
                    return relativeX < 0.48;
                    
                case UP:
                    // Face should be in lower part of frame
                    // Lowered threshold for easier detection
                    return relativeY > 0.52;
                    
                case DOWN:
                    // Face should be in upper part of frame
                    // Raised threshold for easier detection
                    return relativeY < 0.48;
                    
                default:
                    // For other angles (smiling, neutral, distance, lighting), just check if somewhat centered
                    return relativeX > 0.30 && relativeX < 0.70 && 
                           relativeY > 0.30 && relativeY < 0.70;
            }
            
        } catch (Exception e) {
            logger.error("Error detecting pose: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop camera
     */
    @FXML
    private void handleStopCamera() {
        logger.info("Stopping camera");
        
        cameraRunning = false;
        
        if (cameraThread != null) {
            cameraThread.interrupt();
        }
        
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                logger.error("Error stopping camera", e);
            }
        }
        
        startCameraButton.setDisable(false);
        stopCameraButton.setDisable(true);
        captureButton.setDisable(true);
        
        Platform.runLater(() -> {
            cameraView.setImage(null);
            qualityLabel.setText("Camera stopped");
            qualityIcon.setText("📷");
            
            // Show camera overlay again
            if (cameraOffOverlay != null) {
                cameraOffOverlay.setVisible(true);
            }
        });
        
        ModernToast.info("Camera stopped");
    }
    
    /**
     * Handle auto-capture toggle
     */
    @FXML
    private void handleToggleAutoCapture() {
        autoCaptureEnabled = autoCaptureCheckbox != null && autoCaptureCheckbox.isSelected();
        
        if (autoCaptureEnabled) {
            logger.info("Auto-capture enabled");
            ModernToast.success("🤖 Auto-capture ON - Move to each angle and wait");
            
            // Disable manual capture button when auto-capture is on
            if (captureButton != null) {
                captureButton.setDisable(true);
            }
        } else {
            logger.info("Auto-capture disabled");
            ModernToast.info("Auto-capture OFF - Use manual capture");
            
            // Enable manual capture button
            if (captureButton != null && cameraRunning) {
                captureButton.setDisable(false);
            }
        }
    }
    
    /**
     * Capture photo
     */
    @FXML
    private void handleCapture() {
        if (selectedUser == null || currentFrame == null) {
            ModernToast.warning("No frame to capture");
            return;
        }
        
        // Check cooldown to prevent spam clicking
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCaptureTime < CAPTURE_COOLDOWN_MS) {
            ModernToast.warning("Please wait before capturing next photo");
            return;
        }
        lastCaptureTime = currentTime;
        
        // Get current angle suggestion
        CaptureAngle angle = currentAngleIndex < recommendedAngles.size() ? 
            recommendedAngles.get(currentAngleIndex) : CaptureAngle.FRONT;
        
        // Capture face
        String savedPath = registrationService.captureFace(currentFrame, selectedUser, angle);
        
        if (savedPath != null) {
            capturedPhotos.add(savedPath);
            updatePhotosGrid();
            updateCaptureCount();
            updateTrainButtonState();
            
            // Move to next angle
            currentAngleIndex++;
            updateAngleGuide();
            
            ModernToast.success("Photo captured! " + capturedPhotos.size() + "/" + FaceRegistrationService.RECOMMENDED_PHOTOS);
            logger.info("Captured photo {}: {}", capturedPhotos.size(), savedPath);
            
            // Auto-train and close if recommended number reached
            if (capturedPhotos.size() >= FaceRegistrationService.RECOMMENDED_PHOTOS) {
                logger.info("Reached recommended photo count. Auto-training model...");
                ModernToast.info("✅ All photos captured! Auto-training model...");
                
                // Stop camera first
                handleStopCamera();
                
                // Auto-train after short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        Platform.runLater(this::performAutoTraining);
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        } else {
            ModernToast.error("Failed to capture photo - Check face quality");
        }
    }
    
    /**
     * Train model with captured faces
     */
    @FXML
    private void handleTrain() {
        if (selectedUser == null) {
            ModernToast.warning("Please select a user");
            return;
        }
        
        if (!registrationService.hasEnoughFaces(selectedUser.getUserId())) {
            ModernToast.warning("Need at least " + FaceRegistrationService.MINIMUM_PHOTOS + " photos");
            return;
        }
        
        // Show confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Train Model");
        confirm.setHeaderText("Train face recognition model?");
        confirm.setContentText(String.format(
            "This will train the model with %d photos for user %s.\nThis may take a few moments.",
            capturedPhotos.size(),
            selectedUser.getFullName()
        ));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performTraining();
            }
        });
    }
    
    /**
     * Perform model training in background
     */
    private void performTraining() {
        trainingBox.setVisible(true);
        trainingProgress.setProgress(-1); // Indeterminate
        trainingStatusLabel.setText("Training model...");
        trainButton.setDisable(true);
        
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                String facesDir = registrationService.getFacesBaseDirectory();
                int count = recognitionService.trainFromDirectory(facesDir);
                
                // Save model
                String modelPath = AppConfig.getModelPath();
                recognitionService.saveModel(modelPath);
                
                return count;
            }
            
            @Override
            protected void succeeded() {
                int count = getValue();
                trainingProgress.setProgress(1.0);
                trainingStatusLabel.setText(String.format("✅ Training complete! Trained with %d faces", count));
                
                ModernToast.success("Face recognition model trained successfully!");
                logger.info("Model trained with {} faces", count);
                
                // Hide progress after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> trainingBox.setVisible(false));
                    } catch (InterruptedException ignored) {}
                }).start();
                
                trainButton.setDisable(false);
            }
            
            @Override
            protected void failed() {
                Throwable e = getException();
                trainingProgress.setProgress(0);
                trainingStatusLabel.setText("❌ Training failed: " + e.getMessage());
                trainButton.setDisable(false);
                
                logger.error("Training failed", e);
                ModernToast.error("Training failed: " + e.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Perform auto-training after capturing recommended photos
     */
    private void performAutoTraining() {
        trainingBox.setVisible(true);
        trainingProgress.setProgress(-1);
        trainingStatusLabel.setText("Auto-training model with captured faces...");
        captureButton.setDisable(true);
        trainButton.setDisable(true);
        
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                String facesDir = registrationService.getFacesBaseDirectory();
                int count = recognitionService.trainFromDirectory(facesDir);
                
                // Save model
                String modelPath = AppConfig.getModelPath();
                recognitionService.saveModel(modelPath);
                
                return count;
            }
            
            @Override
            protected void succeeded() {
                int count = getValue();
                trainingProgress.setProgress(1.0);
                trainingStatusLabel.setText("✅ Training complete! Model ready for recognition");
                
                ModernToast.success("Training complete! Window will close...");
                logger.info("Auto-training succeeded with {} faces", count);
                
                // Close window after short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> {
                            Stage stage = (Stage) closeButton.getScene().getWindow();
                            stage.close();
                        });
                    } catch (InterruptedException ignored) {}
                }).start();
            }
            
            @Override
            protected void failed() {
                Throwable e = getException();
                trainingProgress.setProgress(0);
                trainingStatusLabel.setText("❌ Auto-training failed: " + e.getMessage());
                captureButton.setDisable(false);
                trainButton.setDisable(false);
                
                logger.error("Auto-training failed", e);
                ModernToast.error("Auto-training failed. You can try manually.");
                
                // Hide progress after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> trainingBox.setVisible(false));
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Clear captured photos
     */
    @FXML
    private void handleClearPhotos() {
        if (selectedUser == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Photos");
        confirm.setHeaderText("Delete all captured photos?");
        confirm.setContentText("This will delete all photos for " + selectedUser.getFullName());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int deleted = registrationService.deleteUserFaces(selectedUser.getUserId());
                capturedPhotos.clear();
                currentAngleIndex = 0;
                updatePhotosGrid();
                updateCaptureCount();
                updateAngleGuide();
                updateTrainButtonState();
                ModernToast.success(String.format("Deleted %d photos", deleted));
            }
        });
    }
    
    /**
     * Close window
     */
    @FXML
    private void handleClose() {
        // Stop camera if running
        if (cameraRunning) {
            handleStopCamera();
        }
        
        // Close window
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Cleanup when window closes
     */
    public void cleanup() {
        logger.info("Cleaning up FaceRegistrationController");
        
        if (cameraRunning) {
            handleStopCamera();
        }
        
        if (converter != null) {
            converter.close();
        }
    }
}
