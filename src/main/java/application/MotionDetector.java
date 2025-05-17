package application;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;

public class MotionDetector {

    private Mat previousGray;

    public void detectMotion(Mat currentFrame) {
        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat diff = new Mat();
        Mat thresh = new Mat();

        // Convert to grayscale and blur
        opencv_imgproc.cvtColor(currentFrame, gray, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.GaussianBlur(gray, blur, new Size(21, 21), 0);

        if (previousGray != null) {
            // Compute absolute difference
            opencv_core.absdiff(previousGray, blur, diff);
            opencv_imgproc.threshold(diff, thresh, 25, 255, opencv_imgproc.THRESH_BINARY);
            opencv_imgproc.dilate(thresh, thresh, new Mat());

            // Find contours
            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            opencv_imgproc.findContours(thresh.clone(), contours, hierarchy,
                    opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            for (long i = 0; i < contours.size(); i++) {
                Mat contour = contours.get(i);
                Rect boundingRect = opencv_imgproc.boundingRect(contour);

                if (boundingRect.area() > 500) {
                    Point pt1 = new Point(boundingRect.x(), boundingRect.y());
                    Point pt2 = new Point(boundingRect.x() + boundingRect.width(), boundingRect.y() + boundingRect.height());

                    opencv_imgproc.rectangle(currentFrame, pt1, pt2,
                            new Scalar(0, 255, 0, 0),
                            2, // thickness
                            opencv_imgproc.LINE_8,
                            0  // shift
                    );
                }
            }
        }

        // Save for next frame comparison
        previousGray = blur.clone();
    }
}
