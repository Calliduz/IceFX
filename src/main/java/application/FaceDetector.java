package application;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceDetector {

    private ImageView frameView;
    private VideoCapture capture = new VideoCapture();
    private ScheduledExecutorService timer;
    private CascadeClassifier faceCascade;
    private Mat latestFaceROI = new Mat();

    /**
     * Constructs a FaceDetector, loading the face cascade file.
     * Any URI errors are wrapped in a RuntimeException.
     */
    public FaceDetector() {
        // 1) find the file on the classpath
        URL cascadeUrl = getClass().getResource("/haar/lbpcascade_frontalface.xml");
        if (cascadeUrl == null) {
            throw new RuntimeException("Could not find /haar/lbpcascade_frontalface.xml on classpath");
        }

        try {
            // 2) convert to a real filesystem path
            File cascadeFile = new File(cascadeUrl.toURI());
            String cascadePath = cascadeFile.getAbsolutePath();

            // 3) load into OpenCV
            faceCascade = new CascadeClassifier(cascadePath);
            if (faceCascade.empty()) {
                throw new RuntimeException("Failed to load cascade from " + cascadePath);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI for cascade file", e);
        }
    }

    /** Bind your ImageView here (call in initialize()). */
    public void setFrame(ImageView view) {
        this.frameView = view;
    }

    /** Try camera indices 0–2 until one opens, then start grabbing frames. */
    public boolean start() {
        for (int cam = 0; cam < 3; cam++) {
            System.out.println("[FaceDetector] Attempting camera " + cam);
            capture.open(cam);
            if (capture.isOpened()) {
                System.out.println("[FaceDetector] Opened camera " + cam);
                break;
            }
        }
        if (!capture.isOpened()) {
            System.err.println("[FaceDetector] Could not open any webcam.");
            return false;
        }

        Runnable grabFrame = () -> {
            try {
                Mat frame = new Mat();
                boolean ok = capture.read(frame);
                if (!ok || frame.empty()) return;

                // 1) Show raw frame immediately
                Image fx = mat2Image(frame);
                Platform.runLater(() -> frameView.setImage(fx));

                // 2) Detect & crop face
                Mat gray = new Mat();
                cvtColor(frame, gray, COLOR_BGR2GRAY);
                RectVector faces = new RectVector();
                faceCascade.detectMultiScale(gray, faces);
                if (faces.size() > 0) {
                    Rect r = faces.get(0);
                    Mat faceROI = new Mat(gray, r);
                    resize(faceROI, faceROI, new Size(200, 200));
                    latestFaceROI = faceROI.clone();

                    // draw rectangle for feedback
                    rectangle(frame, r, new Scalar(0, 255, 0, 0), 2, LINE_8, 0);
                    Image fx2 = mat2Image(frame);
                    Platform.runLater(() -> frameView.setImage(fx2));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(grabFrame, 0, 33, TimeUnit.MILLISECONDS);
        System.out.println("[FaceDetector] Webcam loop started.");
        return true;
    }

    /** Stops the grab loop and clears the view. */
    public void stop() {
        if (timer != null) timer.shutdown();
        if (capture.isOpened()) capture.release();
        Platform.runLater(() -> frameView.setImage(null));
        System.out.println("[FaceDetector] Webcam stopped.");
    }

    /** Returns the last detected face ROI (200×200 gray), or null if none. */
    public Mat getFaceROI() {
        return (latestFaceROI != null && !latestFaceROI.empty())
            ? latestFaceROI.clone()
            : null;
    }

    /** Utility: convert OpenCV Mat to JavaFX Image. */
    private Image mat2Image(Mat frame) {
        BufferedImage buf = new BufferedImage(
            frame.cols(), frame.rows(),
            BufferedImage.TYPE_3BYTE_BGR
        );
        byte[] data = ((DataBufferByte) buf.getRaster()
                             .getDataBuffer()).getData();
        frame.data().get(data);
        return SwingFXUtils.toFXImage(buf, null);
    }
}