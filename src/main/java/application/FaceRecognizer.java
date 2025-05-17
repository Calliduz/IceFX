package application;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;

/**
 * Wrapper around OpenCV's LBPH face recognizer with thresholding and debug output.
 */
public class FaceRecognizer {

    private final LBPHFaceRecognizer recognizer;
    private final double confidenceThreshold;

    /** Default constructor uses threshold = 70.0 */
    public FaceRecognizer() {
        this(70.0);
    }

    /**
     * @param threshold maximum confidence to accept (lower is better)
     */
    public FaceRecognizer(double threshold) {
        this.recognizer = LBPHFaceRecognizer.create();
        this.confidenceThreshold = threshold;
        loadTrainedData();
    }

    public void saveTrainedData() {
        File f = new File("resources/trained_faces.xml");
        recognizer.save(f.getAbsolutePath());
        System.out.println("[FaceRecognizer] Model saved: " + f.getAbsolutePath());
    }

    public void loadTrainedData() {
        File f = new File("resources/trained_faces.xml");
        if (f.exists()) {
            recognizer.read(f.getAbsolutePath());
            System.out.println("[FaceRecognizer] Loaded model: " + f.getAbsolutePath());
        } else {
            System.err.println("[FaceRecognizer] No trained model at: " + f.getAbsolutePath());
        }
    }

    /**
     * Predicts the label for the given preprocessed face Mat.
     * @return personId label, or -1 if confidence > threshold
     */
    public int predict(Mat face) {
        IntPointer lbl = new IntPointer(1);
        DoublePointer conf = new DoublePointer(1);
        recognizer.predict(face, lbl, conf);
        int label = lbl.get(0);
        double c = conf.get(0);
        System.out.printf("[FaceRecognizer] label=%d, confidence=%.2f%n", label, c);
        if (c > confidenceThreshold) {
            System.out.printf("[FaceRecognizer] Rejected (%.2f > %.2f)%n", c, confidenceThreshold);
            return -1;
        }
        return label;
    }

    /**
     * Trains the LBPH model on the provided face images & labels.
     */
    public void train(Mat[] images, int[] labels) {
        // 1) pack images into MatVector
        MatVector mv = new MatVector(images.length);
        for (int i = 0; i < images.length; i++) {
            mv.put(i, images[i]);
        }

        // 2) build an IntPointer containing your labels
        IntPointer labelPtr = new IntPointer(labels.length);
        labelPtr.put(labels);  // copy your int[] into native memory

        // 3) wrap that pointer in a Mat of type CV_32SC1
        Mat lbls = new Mat(labels.length, 1, CV_32SC1, labelPtr);

        // 4) train & save
        recognizer.train(mv, lbls);
        recognizer.save("resources/trained_faces.xml");
        System.out.println("[FaceRecognizer] Training complete; model saved.");
    }

    private void trainPersonModel(int personId) {
        File dir = new File("resources/trained_faces/" + personId);
        File[] images = dir.listFiles((d, name) -> name.endsWith(".png"));
        List<Mat> mats = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        if (images != null) {
            for (File imgFile : images) {
                Mat img = opencv_imgcodecs.imread(imgFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (img != null && !img.empty()) {
                    mats.add(img);
                    labels.add(personId);
                }
            }
        }
        if (!mats.isEmpty()) {
            Mat[] matsArray = mats.toArray(new Mat[0]);
            int[] labelsArray = labels.stream().mapToInt(i -> i).toArray();
            train(matsArray, labelsArray);
            saveTrainedData();
        }
    }
}
