package com.icefx.util;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to train the face recognition model from existing face images.
 */
public class ModelTrainer {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainer.class);
    
    public static void main(String[] args) {
        try {
            System.out.println("=== IceFX Model Training ===");
            
            // Load OpenCV
            if (!NativeLoader.loadOpenCV()) {
                logger.error("❌ Failed to load OpenCV native libraries!");
                System.exit(1);
            }
            logger.info("✅ OpenCV loaded");
            
            // Create recognizer
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            logger.info("✅ LBPH Recognizer created");
            
            // Load face images from faces directory
            File facesDir = new File("faces");
            if (!facesDir.exists()) {
                logger.error("❌ faces/ directory not found!");
                System.exit(1);
            }
            
            List<Mat> images = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();
            
            // Scan each user directory
            File[] userDirs = facesDir.listFiles(File::isDirectory);
            if (userDirs == null || userDirs.length == 0) {
                logger.error("❌ No user directories found in faces/");
                System.exit(1);
            }
            
            for (File userDir : userDirs) {
                try {
                    int userId = Integer.parseInt(userDir.getName());
                    logger.info("Loading faces for user ID: {}", userId);
                    
                    File[] faceFiles = userDir.listFiles((dir, name) -> 
                        name.endsWith(".jpg") || name.endsWith(".png"));
                    
                    if (faceFiles == null || faceFiles.length == 0) {
                        logger.warn("  ⚠️  No face images found for user {}", userId);
                        continue;
                    }
                    
                    int loadedCount = 0;
                    for (File faceFile : faceFiles) {
                        Mat img = imread(faceFile.getAbsolutePath(), IMREAD_GRAYSCALE);
                        if (img.empty()) {
                            logger.warn("  ⚠️  Could not load: {}", faceFile.getName());
                            continue;
                        }
                        
                        images.add(img);
                        labels.add(userId);
                        loadedCount++;
                    }
                    
                    logger.info("  ✅ Loaded {} images for user {}", loadedCount, userId);
                    
                } catch (NumberFormatException e) {
                    logger.warn("Skipping invalid directory: {}", userDir.getName());
                }
            }
            
            if (images.isEmpty()) {
                logger.error("❌ No images to train!");
                System.exit(1);
            }
            
            logger.info("\n📊 Training with {} images...", images.size());
            
            // Convert to OpenCV format
            MatVector imageVector = new MatVector(images.size());
            Mat labelsMat = new Mat(images.size(), 1, CV_32SC1);
            
            for (int i = 0; i < images.size(); i++) {
                imageVector.put(i, images.get(i));
                labelsMat.ptr(i).putInt(labels.get(i));
            }
            
            // Train the model
            recognizer.train(imageVector, labelsMat);
            logger.info("✅ Training complete!");
            
            // Save model
            String modelPath = "trained_faces.xml";
            recognizer.save(modelPath);
            logger.info("✅ Model saved to: {}", modelPath);
            
            // Verify file
            File modelFile = new File(modelPath);
            if (modelFile.exists()) {
                logger.info("✅ Model file verified: {} bytes", modelFile.length());
                System.out.println("\n🎉 Training successful!");
                System.out.println("Model file: " + modelFile.getAbsolutePath());
                System.out.println("You can now use student mode for face recognition.");
            } else {
                logger.error("❌ Model file was not created!");
                System.exit(1);
            }
            
            // Cleanup
            for (Mat img : images) {
                img.close();
            }
            imageVector.close();
            labelsMat.close();
            recognizer.close();
            
        } catch (Exception e) {
            logger.error("❌ Error during training", e);
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
