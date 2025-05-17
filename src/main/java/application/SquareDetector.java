package application;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.swing.*;
import java.io.File;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class SquareDetector {

    private final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
    private final CanvasFrame canvas = new CanvasFrame("Face Detection");
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    // Haar cascade face detector
    private final CascadeClassifier faceCascade;
    // smoothing
    private Rect2d prevBox = null;
    private double alpha = 0.5; // try 0.5 smoothing factor
    // re-detection interval
    private int frameCount = 0;
    private static final int REDETECT_INTERVAL = 15;

    public SquareDetector() throws Exception {
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Extract Haar cascade from resources/haar directory
        File xmlFile = Loader.extractResource(
            "/haar/haarcascade_frontalface_default.xml",
            null,
            "haarcascade_frontalface_default",
            ".xml"
        );
        faceCascade = new CascadeClassifier(xmlFile.getAbsolutePath());
        if (faceCascade.empty()) {
            throw new Exception("Failed to load Haar cascade from " + xmlFile.getAbsolutePath());
        }
    }

    public void loop() {
        try {
            grabber.start();
            while (canvas.isVisible()) {
                Frame frame = grabber.grab();
                if (frame == null) break;
                Mat mat = converter.convert(frame);
                Mat output = detectAndSmooth(mat);
                canvas.showImage(converter.convert(output));
            }
            grabber.stop();
            canvas.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Mat detectAndSmooth(Mat image) {
        // convert to gray and equalize
        Mat gray = new Mat();
        cvtColor(image, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        Rect2d current = null;
        frameCount++;
        // detect only every REDETECT_INTERVAL or if no previous box
        if (frameCount % REDETECT_INTERVAL == 0 || prevBox == null) {
            // detect faces with tuned parameters
            RectVector faces = new RectVector();
            faceCascade.detectMultiScale(
                gray,
                faces,
                1.1,        // scaleFactor
                5,          // minNeighbors
                0,          // flags
                new Size(80,80),  // minSize
                new Size(400,400) // maxSize
            );

            double maxArea = 0;
            for (int i = 0; i < faces.size(); i++) {
                Rect r = faces.get(i);
                double area = r.width() * r.height();
                if (area > maxArea) {
                    maxArea = area;
                    current = new Rect2d(r.x(), r.y(), r.width(), r.height());
                }
            }
        } else {
            // keep current as previous if not re-detect frame
            current = prevBox;
        }

        if (current != null) {
            // smooth against previous
            if (prevBox != null) {
                double x = alpha * current.x() + (1 - alpha) * prevBox.x();
                double y = alpha * current.y() + (1 - alpha) * prevBox.y();
                double w = alpha * current.width() + (1 - alpha) * prevBox.width();
                double h = alpha * current.height() + (1 - alpha) * prevBox.height();
                current = new Rect2d(x, y, w, h);
            }
            prevBox = current;

            // draw rectangle
            Point p1 = new Point((int) current.x(), (int) current.y());
            Point p2 = new Point(
                (int) (current.x() + current.width()),
                (int) (current.y() + current.height())
            );
            rectangle(image, p1, p2, new Scalar(0, 255, 0, 0), 2, LINE_8, 0);
        } else {
            prevBox = null;
        }

        return image;
    }

    public static void main(String[] args) {
        try {
            new SquareDetector().loop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
