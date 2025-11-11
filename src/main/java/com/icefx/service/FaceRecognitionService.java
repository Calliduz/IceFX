package com.icefx.service;

import com.icefx.config.AppConfig;
import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.bytedeco.opencv.global.opencv_face.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Face recognition service using LBPH (Local Binary Patterns Histograms) algorithm.
 * 
 * Features:
 * - LBPH face recognition (lightweight, CPU-friendly)
 * - Confidence threshold filtering
 * - Recognition debouncing (prevents duplicate recognitions)
 * - Thread-safe operations
 * - Comprehensive error handling
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class FaceRecognitionService {
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);
    
    // Recognition settings (loaded from AppConfig)
    private static final int FACE_SIZE = 100;  // Standard face size for recognition
    
    // Cascade classifier for face detection
    private final CascadeClassifier faceDetector;
    
    // LBPH recognizer
    private final LBPHFaceRecognizer recognizer;
    
    // Configuration
    private final double confidenceThreshold;
    private final long debounceMs;
    
    // DAO for user data
    private final UserDAO userDAO;
    
    // Debouncing map: userId -> lastRecognitionTime
    private final Map<Integer, LocalDateTime> recentRecognitions = new ConcurrentHashMap<>();
    
    // Training status
    private boolean isTrained = false;
    private int trainedFacesCount = 0;
    
    /**
     * Recognition result wrapper.
     */
    public static class RecognitionResult {
        public enum Status {
            RECOGNIZED,    // Successfully recognized
            UNKNOWN,       // Face detected but not in database
            DEBOUNCED,     // Recently recognized (skip logging)
            LOW_CONFIDENCE,// Confidence below threshold
            NO_FACE,       // No face detected in image
            ERROR          // Processing error
        }
        
        private final Status status;
        private final Integer userId;
        private final String userName;
        private final double confidence;
        private final String message;
        
        private RecognitionResult(Status status, Integer userId, String userName, double confidence, String message) {
            this.status = status;
            this.userId = userId;
            this.userName = userName;
            this.confidence = confidence;
            this.message = message;
        }
        
        // Factory methods
        public static RecognitionResult recognized(int userId, String userName, double confidence) {
            return new RecognitionResult(Status.RECOGNIZED, userId, userName, confidence, 
                String.format("Recognized: %s (%.1f%%)", userName, confidence));
        }
        
        public static RecognitionResult unknown(double confidence) {
            return new RecognitionResult(Status.UNKNOWN, null, null, confidence,
                String.format("Unknown person (confidence: %.1f%%)", confidence));
        }
        
        public static RecognitionResult debounced(int userId, String userName, double confidence) {
            return new RecognitionResult(Status.DEBOUNCED, userId, userName, confidence,
                String.format("%s recognized recently - skipping", userName));
        }
        
        public static RecognitionResult lowConfidence(double confidence) {
            return new RecognitionResult(Status.LOW_CONFIDENCE, null, null, confidence,
                String.format("Low confidence: %.1f%%", confidence));
        }
        
        public static RecognitionResult noFace() {
            return new RecognitionResult(Status.NO_FACE, null, null, 0.0, "No face detected");
        }
        
        public static RecognitionResult error(String errorMsg) {
            return new RecognitionResult(Status.ERROR, null, null, 0.0, "Error: " + errorMsg);
        }
        
        // Getters
        public Status getStatus() { return status; }
        public Integer getUserId() { return userId; }
        public String getUserName() { return userName; }
        public double getConfidence() { return confidence; }
        public String getMessage() { return message; }
        public boolean shouldLogAttendance() { 
            return status == Status.RECOGNIZED; 
        }
        
        @Override
        public String toString() {
            return message;
        }
    }
    
    /**
     * Create face recognition service with defaults from AppConfig.
     */
    public FaceRecognitionService(UserDAO userDAO, String cascadePath) {
        this(userDAO, cascadePath, 
             AppConfig.getConfidenceThreshold(), 
             AppConfig.getDebounceMillis());
    }
    
    /**
     * Create face recognition service with custom settings.
     */
    public FaceRecognitionService(UserDAO userDAO, String cascadePath, 
                                   double confidenceThreshold, long debounceMs) {
        this.userDAO = userDAO;
        this.confidenceThreshold = confidenceThreshold;
        this.debounceMs = debounceMs;
        
        // Initialize face detector - load from resources
        logger.info("Initializing face detector with cascade: {}", cascadePath);
        
        // Try to load from resources first
        String actualPath = cascadePath;
        if (cascadePath.startsWith("/")) {
            // Resource path - need to extract to temp file
            try {
                java.io.InputStream is = getClass().getResourceAsStream(cascadePath);
                if (is == null) {
                    throw new IllegalStateException("Cascade file not found in resources: " + cascadePath);
                }
                
                // Create temp file
                java.io.File tempFile = java.io.File.createTempFile("cascade", ".xml");
                tempFile.deleteOnExit();
                
                // Copy resource to temp file
                java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                is.close();
                
                actualPath = tempFile.getAbsolutePath();
                logger.info("Extracted cascade to temp file: {}", actualPath);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load cascade from resources: " + cascadePath, e);
            }
        }
        
        this.faceDetector = new CascadeClassifier(actualPath);
        
        if (faceDetector.empty()) {
            throw new IllegalStateException("Failed to load cascade classifier from: " + actualPath);
        }
        
        // Create LBPH recognizer with optimized parameters
        this.recognizer = LBPHFaceRecognizer.create(
            1,      // radius
            8,      // neighbors
            8,      // grid_x
            8,      // grid_y
            confidenceThreshold  // threshold
        );
        
        logger.info("✅ Face recognition service initialized");
        logger.info("  Confidence threshold: {}", confidenceThreshold);
        logger.info("  Debounce time: {}ms", debounceMs);
    }
    
    /**
     * Train recognizer with faces from database/filesystem.
     * 
     * @param facesDirectory Directory containing face images
     * @return Number of faces trained
     */
    public int trainFromDirectory(String facesDirectory) {
        try {
            logger.info("Training recognizer from directory: {}", facesDirectory);
            
            File dir = new File(facesDirectory);
            if (!dir.exists() || !dir.isDirectory()) {
                logger.error("Faces directory does not exist: {}", facesDirectory);
                return 0;
            }
            
            List<Mat> faceImages = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();
            
            // Load all face images
            for (File userDir : Objects.requireNonNull(dir.listFiles())) {
                if (!userDir.isDirectory()) continue;
                
                try {
                    int userId = Integer.parseInt(userDir.getName());
                    
                    for (File imageFile : Objects.requireNonNull(userDir.listFiles())) {
                        if (!imageFile.getName().endsWith(".png") && 
                            !imageFile.getName().endsWith(".jpg")) {
                            continue;
                        }
                        
                        Mat face = imread(imageFile.getAbsolutePath(), IMREAD_GRAYSCALE);
                        if (face.empty()) {
                            logger.warn("Failed to load image: {}", imageFile);
                            continue;
                        }
                        
                        // Preprocess face
                        Mat processed = preprocessFace(face);
                        faceImages.add(processed);
                        labels.add(userId);
                    }
                    
                } catch (NumberFormatException e) {
                    logger.warn("Invalid user directory name: {}", userDir.getName());
                }
            }
            
            if (faceImages.isEmpty()) {
                logger.warn("No face images found for training");
                return 0;
            }
            
            // Convert lists to OpenCV format
            MatVector facesVector = new MatVector(faceImages.size());
            Mat labelsMat = new Mat(labels.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);
            
            for (int i = 0; i < faceImages.size(); i++) {
                facesVector.put(i, faceImages.get(i));
                // Use Mat put method directly
                labelsMat.ptr(i).putInt(labels.get(i));
            }
            
            // Train recognizer
            recognizer.train(facesVector, labelsMat);
            
            isTrained = true;
            trainedFacesCount = faceImages.size();
            
            logger.info("✅ Training complete! Trained with {} face images", trainedFacesCount);
            return trainedFacesCount;
            
        } catch (Exception e) {
            logger.error("Failed to train recognizer", e);
            return 0;
        }
    }
    
    /**
     * Recognize a person from face image.
     * 
     * @param faceImage Face image (Mat in grayscale)
     * @return Recognition result
     */
    public RecognitionResult recognize(Mat faceImage) {
        if (!isTrained) {
            logger.warn("Recognizer not trained yet");
            return RecognitionResult.error("Recognizer not trained");
        }
        
        if (faceImage == null || faceImage.empty()) {
            return RecognitionResult.noFace();
        }
        
        try {
            // Preprocess face (resize, equalize)
            Mat prepared = preprocessFace(faceImage);
            
            // Predict
            IntPointer label = new IntPointer(1);
            DoublePointer confidence = new DoublePointer(1);
            recognizer.predict(prepared, label, confidence);
            
            int userId = label.get(0);
            double conf = confidence.get(0);
            
            logger.debug("Recognition result: userId={}, confidence={}", userId, conf);
            
            // Check confidence threshold
            if (conf > confidenceThreshold) {
                logger.debug("Confidence {} exceeds threshold {} - unknown person", conf, confidenceThreshold);
                return RecognitionResult.unknown(conf);
            }
            
            // Get user info
            User user = userDAO.findById(userId).orElse(null);
            if (user == null) {
                logger.warn("User ID {} not found in database", userId);
                return RecognitionResult.unknown(conf);
            }
            
            // Check debouncing
            if (isRecentlyRecognized(userId)) {
                logger.debug("User {} debounced (recognized recently)", user.getFullName());
                return RecognitionResult.debounced(userId, user.getFullName(), conf);
            }
            
            // Mark as recently recognized
            recentRecognitions.put(userId, LocalDateTime.now());
            
            logger.info("✅ Recognized: {} (confidence: {:.1f})", user.getFullName(), conf);
            return RecognitionResult.recognized(userId, user.getFullName(), conf);
            
        } catch (Exception e) {
            logger.error("Recognition failed", e);
            return RecognitionResult.error(e.getMessage());
        }
    }
    
    /**
     * Detect and recognize face from full image.
     * 
     * @param image Full image (may contain multiple faces)
     * @return Recognition result for first detected face, or noFace if none found
     */
    public RecognitionResult detectAndRecognize(Mat image) {
        try {
            // Detect faces
            RectVector faces = new RectVector();
            Mat gray = new Mat();
            
            if (image.channels() > 1) {
                cvtColor(image, gray, COLOR_BGR2GRAY);
            } else {
                image.copyTo(gray);
            }
            
            faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());
            
            if (faces.size() == 0) {
                logger.debug("No faces detected in image");
                return RecognitionResult.noFace();
            }
            
            // Process first face only
            Rect faceRect = faces.get(0);
            Mat faceImage = new Mat(gray, faceRect);
            
            return recognize(faceImage);
            
        } catch (Exception e) {
            logger.error("Face detection failed", e);
            return RecognitionResult.error(e.getMessage());
        }
    }
    
    /**
     * Check if user was recognized recently (debouncing).
     */
    private boolean isRecentlyRecognized(int userId) {
        LocalDateTime lastTime = recentRecognitions.get(userId);
        if (lastTime == null) {
            return false;
        }
        
        long millisSince = ChronoUnit.MILLIS.between(lastTime, LocalDateTime.now());
        return millisSince < debounceMs;
    }
    
    /**
     * Clear debounce cache for a user (e.g., after manual logout).
     */
    public void clearDebounce(int userId) {
        recentRecognitions.remove(userId);
        logger.debug("Cleared debounce for user {}", userId);
    }
    
    /**
     * Clear all debounce cache.
     */
    public void clearAllDebounce() {
        recentRecognitions.clear();
        logger.debug("Cleared all debounce cache");
    }
    
    /**
     * Preprocess face for recognition (grayscale, resize, equalize).
     */
    private Mat preprocessFace(Mat face) {
        Mat processed = new Mat();
        
        // Ensure grayscale
        if (face.channels() > 1) {
            cvtColor(face, processed, COLOR_BGR2GRAY);
        } else {
            face.copyTo(processed);
        }
        
        // Resize to standard size
        resize(processed, processed, new Size(FACE_SIZE, FACE_SIZE));
        
        // Histogram equalization (improves lighting consistency)
        equalizeHist(processed, processed);
        
        return processed;
    }
    
    /**
     * Add a new person to the recognizer (requires retraining).
     * 
     * @param userId User ID
     * @param faceImages List of face images for the user
     */
    public void addPerson(int userId, List<Mat> faceImages) {
        // TODO: Implement incremental training or trigger full retrain
        logger.info("Adding person {} with {} face images", userId, faceImages.size());
        // For now, user must retrain from directory
    }
    
    /**
     * Save trained model to file.
     */
    public void saveModel(String filepath) {
        if (!isTrained) {
            logger.warn("Cannot save untrained model");
            return;
        }
        
        try {
            recognizer.save(filepath);
            logger.info("✅ Model saved to: {}", filepath);
        } catch (Exception e) {
            logger.error("Failed to save model", e);
        }
    }
    
    /**
     * Load trained model from file.
     */
    public void loadModel(String filepath) {
        try {
            recognizer.read(filepath);
            isTrained = true;
            logger.info("✅ Model loaded from: {}", filepath);
        } catch (Exception e) {
            logger.error("Failed to load model", e);
            isTrained = false;
        }
    }
    
    // === Getters ===
    
    public boolean isTrained() {
        return isTrained;
    }
    
    public int getTrainedFacesCount() {
        return trainedFacesCount;
    }
    
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public long getDebounceMs() {
        return debounceMs;
    }
}
