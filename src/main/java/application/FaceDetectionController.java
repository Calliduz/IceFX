package application;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.embed.swing.SwingFXUtils;

public class FaceDetectionController implements Initializable {

    @FXML
    private ImageView cameraView;

    @FXML
    private Label faceCountLabel;

    @FXML
    private Button startButton;

    private CascadeClassifier faceCascade;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load face detection model
        faceCascade = new CascadeClassifier("resources/haarcascade_frontalface_alt.xml");
        if (faceCascade.empty()) {
            System.err.println("❌ Could not load Haar Cascade file.");
        }

        startButton.setOnAction(e -> {
            // Simulated frame capture and detection
            Mat frame = captureDummyFrame(); // Replace this with actual camera input
            detectAndDisplay(frame);
        });
    }

    private void detectAndDisplay(Mat frame) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        faceCascade.detectMultiScale(gray, faces);

        for (int i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);
            opencv_imgproc.rectangle(frame, face, new Scalar(0, 255, 0, 0));
        }

        updateImageView(mat2Image(frame));
        updateLabel("Faces detected: " + faces.size());
    }

    private WritableImage mat2Image(Mat frame) {
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        frame.data().get(sourcePixels);

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void updateImageView(WritableImage image) {
        Platform.runLater(() -> cameraView.setImage(image));
    }

    private void updateLabel(String text) {
        Platform.runLater(() -> faceCountLabel.setText(text));
    }

    // Stub method: replace this with actual video capture logic
    private Mat captureDummyFrame() {
        // For testing only: creates a black image
        return new Mat(480, 640, opencv_core.CV_8UC3, new Scalar(0, 0, 0, 0));
    }
}
