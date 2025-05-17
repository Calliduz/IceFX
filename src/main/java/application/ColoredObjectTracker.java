package application;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvMoments;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class ColoredObjectTracker implements Runnable {

    final int INTERVAL = 1; // 1sec
    final int CAMERA_NUM = 0; // Default camera for this time
    FrameGrabber grabber;
    OpenCVFrameConverter.ToIplImage converter;
    IplImage img;

    // Color thresholds (tweak depending on target object)
    static CvScalar rgba_min = cvScalar(0, 0, 130, 0); // RED
    static CvScalar rgba_max = cvScalar(80, 80, 255, 0);

    IplImage image;
    CanvasFrame canvas;
    CanvasFrame path;
    int ii = 0;
    JPanel jp = new JPanel();

    public void init() {
        canvas = new CanvasFrame("Web Cam Live");
        path = new CanvasFrame("Detection");
        path.setContentPane(jp);
    }

    @Override
    public void run() {
        try {
            grabber = FrameGrabber.createDefault(CAMERA_NUM);
            converter = new OpenCVFrameConverter.ToIplImage();
            grabber.start();

            int posX = 0;
            int posY = 0;

            while (true) {
                img = converter.convert(grabber.grab());
                if (img != null) {
                    cvFlip(img, img, 1); // Mirror horizontally
                    canvas.showImage(converter.convert(img));

                    IplImage detectThrs = getThresholdImage(img);
                    CvMoments moments = new CvMoments();
                    cvMoments(detectThrs, moments, 1);

                    double mom10 = cvGetSpatialMoment(moments, 1, 0);
                    double mom01 = cvGetSpatialMoment(moments, 0, 1);
                    double area = cvGetCentralMoment(moments, 0, 0);

                    posX = (int) (mom10 / area);
                    posY = (int) (mom01 / area);

                    if (posX > 0 && posY > 0) {
                        paint(img, posX, posY);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void paint(IplImage img, int posX, int posY) {
        Graphics g = jp.getGraphics();
        path.setSize(img.width(), img.height());
        g.clearRect(0, 0, img.width(), img.height());
        g.setColor(Color.RED);

        try {
            Robot mouseController = new Robot();
            mouseController.mouseMove(posX, posY);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        g.fillOval(posX, posY, 40, 40);
        g.drawString("Detected Here", posX, posY);
        g.drawOval(posX, posY, 40, 40);
        System.out.println("X,Y: " + posX + " , " + posY);
    }

    private IplImage getThresholdImage(IplImage orgImg) {
        IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
        cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold); // red
        cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 15, 0, 0, 0);
        return imgThreshold;
    }

    public IplImage Equalize(BufferedImage bufferedimg) {
        Java2DFrameConverter converter1 = new Java2DFrameConverter();
        OpenCVFrameConverter.ToIplImage converter2 = new OpenCVFrameConverter.ToIplImage();

        IplImage iplOriginal = converter2.convert(converter1.convert(bufferedimg));
        IplImage srcImg = IplImage.create(iplOriginal.width(), iplOriginal.height(), IPL_DEPTH_8U, 1);
        IplImage destImg = IplImage.create(iplOriginal.width(), iplOriginal.height(), IPL_DEPTH_8U, 1);

        cvCvtColor(iplOriginal, srcImg, CV_BGR2GRAY);
        cvEqualizeHist(srcImg, destImg);
        return destImg;
    }

    public void stop() {
        img = null;
        try {
            grabber.stop();
            grabber.release();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        grabber = null;
    }
}
