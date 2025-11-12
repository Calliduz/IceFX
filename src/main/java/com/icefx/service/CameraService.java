package com.icefx.service;

import com.icefx.config.AppConfig;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Thread-safe camera service that handles video capture in background thread.
 * 
 * CRITICAL: This prevents native crashes by isolating OpenCV calls from JavaFX thread.
 * All OpenCV operations run on a dedicated camera thread, while UI updates
 * are posted to the JavaFX Application Thread via Platform.runLater().
 * 
 * Features:
 * - Non-blocking camera operations
 * - Automatic FPS calculation
 * - Frame callback for face detection
 * - Clean resource management
 * - Comprehensive error handling
 * 
 * @author IceFX Team
 * @version 2.0
 */
public class CameraService {
    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);
    
    // Thread-safe state management
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    
    // Dedicated camera thread (prevents UI blocking)
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CameraService-Thread");
        t.setDaemon(true);  // Allow JVM to exit even if camera is running
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });
    
    // Camera hardware
    private FrameGrabber grabber;
    private final int cameraIndex;
    private final int targetFps;
    
    // Frame converter for JavaFX
    private final JavaFXFrameConverter converter = new JavaFXFrameConverter();
    
    // JavaFX properties (thread-safe for UI binding)
    private final ObjectProperty<Image> currentFrame = new SimpleObjectProperty<>();
    private final StringProperty statusText = new SimpleStringProperty("Disconnected");
    private final DoubleProperty fpsProperty = new SimpleDoubleProperty(0.0);
    private final IntegerProperty framesProcessed = new SimpleIntegerProperty(0);
    
    // Callback for frame processing (face detection, etc.)
    private FrameCallback callback;
    
    // Configuration
    private boolean mirrorHorizontally = true; // Mirror camera by default for intuitive display
    
    /**
     * Callback interface for processing captured frames.
     */
    public interface FrameCallback {
        /**
         * Called for each captured frame on the camera thread.
         * IMPORTANT: This runs on background thread - safe for OpenCV operations.
         * 
         * @param frame The captured frame (Mat format)
         */
        void onFrameCaptured(Mat frame);
    }
    
    /**
     * Create a new camera service.
     * 
     * @param cameraIndex Camera device index (0 for default camera)
     * @param targetFps Target frames per second (typically 30)
     */
    public CameraService(int cameraIndex, int targetFps) {
        this.cameraIndex = cameraIndex;
        this.targetFps = targetFps;
        logger.info("CameraService created for camera {} at {} FPS", cameraIndex, targetFps);
    }
    
    /**
     * Convenience constructor that reads camera settings from AppConfig.
     */
    public CameraService() {
        this(AppConfig.getCameraIndex(), AppConfig.getInt("camera.fps", 30));
    }
    
    /**
     * Convenience constructor with default FPS from AppConfig.
     */
    public CameraService(int cameraIndex) {
        this(cameraIndex, AppConfig.getInt("camera.fps", 30));
    }
    
    /**
     * Start camera capture on background thread.
     * This method returns immediately without blocking.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting camera service...");
            Platform.runLater(() -> statusText.set("Starting..."));
            executor.submit(this::captureLoop);
        } else {
            logger.warn("Camera already running");
        }
    }
    
    /**
     * Stop camera capture and release resources.
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping camera service...");
            Platform.runLater(() -> statusText.set("Stopping..."));
        } else {
            logger.warn("Camera not running");
        }
    }
    
    /**
     * Pause frame processing without stopping camera.
     */
    public void pause() {
        isPaused.set(true);
        logger.info("Camera paused");
    }
    
    /**
     * Resume frame processing.
     */
    public void resume() {
        isPaused.set(false);
        logger.info("Camera resumed");
    }
    
    /**
     * Main capture loop - runs on background thread.
     * NEVER call this from JavaFX Application Thread!
     */
    private void captureLoop() {
        try {
            // Initialize camera
            logger.info("Initializing camera {} ...", cameraIndex);
            grabber = new OpenCVFrameGrabber(cameraIndex);
            grabber.setImageWidth(AppConfig.getInt("camera.width", 640));
            grabber.setImageHeight(AppConfig.getInt("camera.height", 480));
            grabber.setFrameRate(targetFps);
            
            try {
                grabber.start();
                logger.info("✅ Camera opened successfully");
                Platform.runLater(() -> statusText.set("Running"));
            } catch (FrameGrabber.Exception e) {
                logger.error("Failed to open camera {}", cameraIndex, e);
                Platform.runLater(() -> {
                    statusText.set("Camera Error");
                });
                showCameraError("Failed to open camera. Check if camera is connected and not in use.");
                return;
            }
            
            // FPS calculation variables
            long lastTime = System.currentTimeMillis();
            int frameCount = 0;
            long frameDelay = 1000 / targetFps;  // milliseconds per frame
            
            // Main capture loop
            while (isRunning.get()) {
                try {
                    // Grab frame from camera
                    Frame frame = grabber.grab();
                    
                    if (frame == null || frame.imageWidth == 0) {
                        logger.warn("Received null or empty frame");
                        Thread.sleep(100);
                        continue;
                    }
                    
                    // Skip processing if paused
                    if (!isPaused.get()) {
                        // Convert to Mat for OpenCV processing
                        org.bytedeco.javacv.OpenCVFrameConverter.ToMat matConverter = 
                            new org.bytedeco.javacv.OpenCVFrameConverter.ToMat();
                        Mat mat = matConverter.convert(frame);
                        
                        // Mirror horizontally if enabled (makes movements intuitive)
                        if (mirrorHorizontally && mat != null) {
                            Mat flippedMat = new Mat();
                            opencv_core.flip(mat, flippedMat, 1); // 1 = horizontal flip
                            mat.release();
                            mat = flippedMat;
                        }
                        
                        // Notify callback for face detection (runs on this thread - safe!)
                        if (callback != null && mat != null) {
                            callback.onFrameCaptured(mat);
                        }
                        
                        // Convert flipped Mat back to Frame for display
                        Frame displayFrame = mat != null ? matConverter.convert(mat) : frame;
                        
                        // Convert to JavaFX Image and update UI (must use Platform.runLater!)
                        Image image = converter.convert(displayFrame);
                        if (image != null) {
                            Platform.runLater(() -> currentFrame.set(image));
                        }
                        
                        frameCount++;
                        
                        // Update frame counter
                        final int count = frameCount;
                        Platform.runLater(() -> framesProcessed.set(count));
                    }
                    
                    // Calculate and update FPS every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTime >= 1000) {
                        double fps = frameCount / ((currentTime - lastTime) / 1000.0);
                        Platform.runLater(() -> fpsProperty.set(fps));
                        logger.debug("FPS: {}", String.format("%.1f", fps));
                        frameCount = 0;
                        lastTime = currentTime;
                    }
                    
                    // Limit to target FPS
                    Thread.sleep(frameDelay);
                    
                } catch (InterruptedException e) {
                    logger.info("Camera thread interrupted");
                    break;
                } catch (Exception e) {
                    logger.error("Error processing frame", e);
                    Thread.sleep(100);  // Brief pause before retry
                }
            }
            
        } catch (Exception e) {
            logger.error("Fatal error in camera loop", e);
            Platform.runLater(() -> statusText.set("Fatal Error"));
        } finally {
            cleanup();
        }
    }
    
    /**
     * Convert JavaCV Frame to OpenCV Mat for processing.
     */
    private Mat convertFrameToMat(Frame frame) {
        try {
            // This is a simplified conversion - in production you might use:
            // OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            // Mat mat = converter.convert(frame);
            // For now, we'll rely on the callback receiving the frame directly
            return null;  // Placeholder
        } catch (Exception e) {
            logger.error("Failed to convert frame to Mat", e);
            return null;
        }
    }
    
    /**
     * Clean up resources.
     */
    private void cleanup() {
        logger.info("Cleaning up camera resources...");
        
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
                logger.info("Camera released successfully");
            }
        } catch (Exception e) {
            logger.error("Error releasing camera", e);
        }
        
        Platform.runLater(() -> {
            statusText.set("Disconnected");
            currentFrame.set(null);
            fpsProperty.set(0.0);
        });
        
        logger.info("Camera cleanup complete");
    }
    
    /**
     * Show user-friendly camera error dialog.
     */
    private void showCameraError(String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Camera Error");
            alert.setHeaderText("Failed to Access Camera");
            alert.setContentText(
                message + "\n\n" +
                "Troubleshooting:\n" +
                "• Check if camera is connected\n" +
                "• Close other apps using the camera\n" +
                "• Try restarting the application\n" +
                "• Check camera permissions"
            );
            alert.showAndWait();
        });
    }
    
    /**
     * Shutdown executor and release all resources.
     * Call this when application closes.
     */
    public void shutdown() {
        logger.info("Shutting down camera service...");
        stop();
        executor.shutdownNow();
        converter.close();
        logger.info("Camera service shutdown complete");
    }
    
    // === Property Getters for JavaFX Binding ===
    
    public ObjectProperty<Image> currentFrameProperty() {
        return currentFrame;
    }
    
    public StringProperty statusTextProperty() {
        return statusText;
    }
    
    public DoubleProperty fpsProperty() {
        return fpsProperty;
    }
    
    public IntegerProperty framesProcessedProperty() {
        return framesProcessed;
    }
    
    public Image getCurrentFrame() {
        return currentFrame.get();
    }
    
    public String getStatusText() {
        return statusText.get();
    }
    
    public double getFps() {
        return fpsProperty.get();
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public boolean isPaused() {
        return isPaused.get();
    }
    
    /**
     * Set callback for frame processing.
     */
    public void setFrameCallback(FrameCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Enable or disable horizontal mirroring of camera feed.
     * When enabled (default), camera acts like a mirror for intuitive interaction.
     * 
     * @param mirror true to mirror, false for normal orientation
     */
    public void setMirrorHorizontally(boolean mirror) {
        this.mirrorHorizontally = mirror;
        logger.info("Camera mirroring {}", mirror ? "enabled" : "disabled");
    }
    
    /**
     * Check if horizontal mirroring is enabled.
     */
    public boolean isMirrorHorizontally() {
        return mirrorHorizontally;
    }
}
