package application;

import org.bytedeco.leptonica.PIX;
import org.bytedeco.leptonica.global.lept;
import org.bytedeco.tesseract.TessBaseAPI;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class OCR {

    private final TessBaseAPI api;

    public OCR() {
        api = new TessBaseAPI();

        // Path to tessdata directory (make sure it exists)
        String datapath = "resources/tessdata"; // directory containing eng.traineddata
        if (api.Init(datapath, "eng") != 0) {
            throw new RuntimeException("❌ Could not initialize Tesseract.");
        }
    }

    public String extractText(Mat mat) {
        // Convert OpenCV Mat to BufferedImage
        BufferedImage bufferedImage = matToBufferedImage(mat);

        // Convert BufferedImage to PIX (Leptonica)
        PIX image = lept.pixReadMem(convertToByteArray(bufferedImage), bufferedImage.getWidth() * bufferedImage.getHeight());

        api.SetImage(image);
        String outText = api.GetUTF8Text().getString();

        lept.pixDestroy(image);
        return outText.trim();
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
        return java2DConverter.convert(converter.convert(mat));
    }

    private byte[] convertToByteArray(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public void close() {
        api.End();
    }
}
