package com.icefx.service;

import com.icefx.model.User;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;

/**
 * Service for registering faces via camera capture.
 * 
 * Features:
 * - Capture multiple photos from camera for training
 * - Face quality validation (brightness, sharpness, face detection)
 * - Save training images organized by user ID
 * - Provide guidance for capturing different angles
 * - Automatic model retraining after registration
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class FaceRegistrationService {
    private static final Logger logger = LoggerFactory.getLogger(FaceRegistrationService.class);
    
    // Quality thresholds
    private static final int MIN_FACE_SIZE = 100;  // Increased minimum face size (was 80)
    private static final int MAX_FACE_SIZE = 350; // Reduced maximum to prevent zoomed-in faces (was 400)
    private static final double MIN_BRIGHTNESS = 50;  // Minimum average brightness
    private static final double MAX_BRIGHTNESS = 200; // Maximum average brightness
    
    // Recommended number of photos for good training
    public static final int RECOMMENDED_PHOTOS = 10;
    public static final int MINIMUM_PHOTOS = 5;
    
    // Base directory for storing face images
    private final String facesBaseDirectory;
    
    // Face detector for quality validation
    private final CascadeClassifier faceDetector;
    
    /**
     * Capture angle guidance for users
     */
    public enum CaptureAngle {
        FRONT("Looking straight at camera"),
        LEFT("Turn head slightly left"),
        RIGHT("Turn head slightly right"),
        UP("Tilt head slightly up"),
        DOWN("Tilt head slightly down"),
        SMILE("Smile at camera"),
        NEUTRAL("Neutral expression"),
        DISTANCE_NEAR("Move closer to camera"),
        DISTANCE_FAR("Move back from camera"),
        GOOD_LIGHTING("Good lighting condition");
        
        private final String instruction;
        
        CaptureAngle(String instruction) {
            this.instruction = instruction;
        }
        
        public String getInstruction() {
            return instruction;
        }
    }
    
    /**
     * Result of face quality validation
     */
    public static class QualityResult {
        private final boolean passed;
        private final String message;
        private final Rect faceRect; // Detected face rectangle
        private final double brightness;
        
        public QualityResult(boolean passed, String message, Rect faceRect, double brightness) {
            this.passed = passed;
            this.message = message;
            this.faceRect = faceRect;
            this.brightness = brightness;
        }
        
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public Rect getFaceRect() { return faceRect; }
        public double getBrightness() { return brightness; }
        
        public static QualityResult pass(String message, Rect faceRect, double brightness) {
            return new QualityResult(true, message, faceRect, brightness);
        }
        
        public static QualityResult fail(String message) {
            return new QualityResult(false, message, null, 0);
        }
    }
    
    /**
     * Create face registration service
     * 
     * @param facesBaseDirectory Base directory for storing face images (e.g., "faces")
     * @param cascadePath Path to Haar cascade file for face detection
     */
    public FaceRegistrationService(String facesBaseDirectory, String cascadePath) {
        this.facesBaseDirectory = facesBaseDirectory;
        
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
        
        // Ensure base directory exists
        try {
            Files.createDirectories(Paths.get(facesBaseDirectory));
            logger.info("Face registration service initialized - Base directory: {}", facesBaseDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create faces directory: " + facesBaseDirectory, e);
        }
    }
    
    /**
     * Validate if the captured frame contains a suitable face for training
     * 
     * @param frame The captured frame from camera
     * @return Quality validation result
     */
    public QualityResult validateFaceQuality(Mat frame) {
        if (frame == null || frame.empty()) {
            return QualityResult.fail("Empty frame");
        }
        
        try {
            // Convert to grayscale for detection
            Mat gray = new Mat();
            if (frame.channels() > 1) {
                cvtColor(frame, gray, COLOR_BGR2GRAY);
            } else {
                frame.copyTo(gray);
            }
            
            // Detect faces
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, 
                new Size(MIN_FACE_SIZE, MIN_FACE_SIZE), 
                new Size(MAX_FACE_SIZE, MAX_FACE_SIZE));
            
            // Check if exactly one face detected
            if (faces.size() == 0) {
                return QualityResult.fail("No face detected - Position yourself in frame");
            }
            if (faces.size() > 1) {
                return QualityResult.fail("Multiple faces detected - Ensure only one person in frame");
            }
            
            Rect faceRect = faces.get(0);
            
            // Check face size
            int faceWidth = faceRect.width();
            int faceHeight = faceRect.height();
            
            if (faceWidth < MIN_FACE_SIZE || faceHeight < MIN_FACE_SIZE) {
                return QualityResult.fail("Face too small - Move closer to camera");
            }
            if (faceWidth > MAX_FACE_SIZE || faceHeight > MAX_FACE_SIZE) {
                return QualityResult.fail("Face too large - Move back from camera");
            }
            
            // Check face proportions to avoid capturing partial faces
            double aspectRatio = (double) faceWidth / faceHeight;
            if (aspectRatio < 0.7 || aspectRatio > 1.4) {
                return QualityResult.fail("Incomplete face detected - Ensure full face is visible");
            }
            
            // Check if face takes up reasonable portion of frame (not too small or zoomed in)
            double faceArea = faceWidth * faceHeight;
            double frameArea = frame.cols() * frame.rows();
            double faceRatio = faceArea / frameArea;
            
            if (faceRatio < 0.05) {
                return QualityResult.fail("Face too small in frame - Move closer");
            }
            if (faceRatio > 0.45) {
                return QualityResult.fail("Face too close - Move back slightly");
            }
            
            // Check brightness
            Mat faceROI = new Mat(gray, faceRect);
            Scalar meanBrightness = mean(faceROI);
            double brightness = meanBrightness.get(0);
            
            if (brightness < MIN_BRIGHTNESS) {
                return QualityResult.fail(String.format("Too dark (%.0f) - Improve lighting", brightness));
            }
            if (brightness > MAX_BRIGHTNESS) {
                return QualityResult.fail(String.format("Too bright (%.0f) - Reduce lighting", brightness));
            }
            
            // Check if face is well-centered (stricter to avoid partial faces/ears)
            int frameCenterX = frame.cols() / 2;
            int frameCenterY = frame.rows() / 2;
            int faceCenterX = faceRect.x() + faceRect.width() / 2;
            int faceCenterY = faceRect.y() + faceRect.height() / 2;
            
            int offsetX = Math.abs(frameCenterX - faceCenterX);
            int offsetY = Math.abs(frameCenterY - faceCenterY);
            
            // Stricter centering: face must be within 20% of center (was 25%)
            if (offsetX > frame.cols() / 5 || offsetY > frame.rows() / 5) {
                return QualityResult.fail("Face not centered - Position yourself in center of frame");
            }
            
            // Ensure face is not cut off at edges
            if (faceRect.x() < 10 || faceRect.y() < 10 ||
                faceRect.x() + faceRect.width() > frame.cols() - 10 ||
                faceRect.y() + faceRect.height() > frame.rows() - 10) {
                return QualityResult.fail("Face too close to edge - Center yourself in frame");
            }
            
            // All checks passed
            return QualityResult.pass(
                String.format("Good quality (brightness: %.0f)", brightness),
                faceRect,
                brightness
            );
            
        } catch (Exception e) {
            logger.error("Error validating face quality", e);
            return QualityResult.fail("Error: " + e.getMessage());
        }
    }
    
    /**
     * Capture and save a face image for training
     * 
     * @param frame The camera frame containing the face
     * @param user The user to register this face for
     * @param angleHint Optional hint about the capture angle
     * @return Path to the saved image file, or null if validation failed
     */
    public String captureFace(Mat frame, User user, CaptureAngle angleHint) {
        // Validate quality first
        QualityResult quality = validateFaceQuality(frame);
        if (!quality.isPassed()) {
            logger.warn("Face quality check failed: {}", quality.getMessage());
            return null;
        }
        
        try {
            // Create user directory if doesn't exist
            String userDir = getUserDirectory(user.getUserId());
            Files.createDirectories(Paths.get(userDir));
            
            // Generate unique filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String angle = angleHint != null ? angleHint.name().toLowerCase() : "capture";
            String filename = String.format("face_%s_%s.png", timestamp, angle);
            String filepath = Paths.get(userDir, filename).toString();
            
            // Extract face region for saving
            Mat faceImage = new Mat(frame, quality.getFaceRect());
            
            // Optionally resize to standard size for consistency
            Mat resized = new Mat();
            resize(faceImage, resized, new Size(200, 200));
            
            // Save image
            imwrite(filepath, resized);
            
            logger.info("Captured face for user {} - {}", user.getUserCode(), filepath);
            return filepath;
            
        } catch (Exception e) {
            logger.error("Failed to save captured face", e);
            return null;
        }
    }
    
    /**
     * Get the directory path for a user's face images
     */
    public String getUserDirectory(int userId) {
        return Paths.get(facesBaseDirectory, String.valueOf(userId)).toString();
    }
    
    /**
     * Get all captured faces for a user
     * 
     * @param userId User ID
     * @return List of image file paths
     */
    public List<String> getUserFaces(int userId) {
        List<String> faces = new ArrayList<>();
        File userDir = new File(getUserDirectory(userId));
        
        if (!userDir.exists() || !userDir.isDirectory()) {
            return faces;
        }
        
        File[] files = userDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
        
        if (files != null) {
            for (File file : files) {
                faces.add(file.getAbsolutePath());
            }
        }
        
        return faces;
    }
    
    /**
     * Count captured faces for a user
     */
    public int countUserFaces(int userId) {
        return getUserFaces(userId).size();
    }
    
    /**
     * Delete all captured faces for a user
     * 
     * @param userId User ID
     * @return Number of files deleted
     */
    public int deleteUserFaces(int userId) {
        List<String> faces = getUserFaces(userId);
        int deleted = 0;
        
        for (String facePath : faces) {
            try {
                Files.delete(Paths.get(facePath));
                deleted++;
            } catch (IOException e) {
                logger.error("Failed to delete face image: {}", facePath, e);
            }
        }
        
        // Try to delete empty directory
        try {
            Files.deleteIfExists(Paths.get(getUserDirectory(userId)));
        } catch (IOException e) {
            logger.debug("Could not delete user directory (may not be empty)");
        }
        
        logger.info("Deleted {} face images for user {}", deleted, userId);
        return deleted;
    }
    
    /**
     * Delete a specific face image
     */
    public boolean deleteFace(String filepath) {
        try {
            Files.delete(Paths.get(filepath));
            logger.info("Deleted face image: {}", filepath);
            return true;
        } catch (IOException e) {
            logger.error("Failed to delete face image: {}", filepath, e);
            return false;
        }
    }
    
    /**
     * Check if user has enough faces for training
     */
    public boolean hasEnoughFaces(int userId) {
        return countUserFaces(userId) >= MINIMUM_PHOTOS;
    }
    
    /**
     * Check if user has recommended number of faces
     */
    public boolean hasRecommendedFaces(int userId) {
        return countUserFaces(userId) >= RECOMMENDED_PHOTOS;
    }
    
    /**
     * Get capture angle recommendations in order
     */
    public static List<CaptureAngle> getRecommendedAngles() {
        List<CaptureAngle> angles = new ArrayList<>();
        angles.add(CaptureAngle.FRONT);
        angles.add(CaptureAngle.LEFT);
        angles.add(CaptureAngle.RIGHT);
        angles.add(CaptureAngle.UP);
        angles.add(CaptureAngle.DOWN);
        angles.add(CaptureAngle.SMILE);
        angles.add(CaptureAngle.NEUTRAL);
        angles.add(CaptureAngle.DISTANCE_NEAR);
        angles.add(CaptureAngle.DISTANCE_FAR);
        angles.add(CaptureAngle.GOOD_LIGHTING);
        return angles;
    }
    
    /**
     * Get faces base directory path
     */
    public String getFacesBaseDirectory() {
        return facesBaseDirectory;
    }
}
