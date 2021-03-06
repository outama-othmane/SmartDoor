package smartdoor.opencv;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import smartdoor.utils.FileSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FaceMaskDetection {
    private static final double THRESHOLD = 0.5f;
    private static ComputationGraph model = null;
    private static Net faceNet = null;

    public static ComputationGraph getModelInstance() throws InvalidKerasConfigurationException, IOException, UnsupportedKerasConfigurationException {
        if (model == null) {
            model = KerasModelImport.importKerasModelAndWeights(
                    FileSystem.getModelResource("mask_detector.model")
            );
        }

        return model;
    }

    public static Net getFaceNetInstance() {
        if (faceNet == null) {
            String prototxtPath = FileSystem.getModelResource("face_detector/deploy.prototxt");
            String weightsPath = FileSystem.getModelResource("face_detector/res10_300x300_ssd_iter_140000.caffemodel");
            faceNet = Dnn.readNet(prototxtPath, weightsPath);
        }

        return faceNet;
    }

    /**
     * Detect if the frame contains a face wearing a mask
     * <p>
     * It returns :
     * -1 if no faces detected
     * 0 if at least one of the faces is not wearing a mask
     * 1 if all the faces are wearing a mask
     *
     * @param frame The image as a Mat
     * @return int
     */
    public int detect(Mat frame) {
        try {
            Net faceNet = getFaceNetInstance();

            // load the model
            ComputationGraph model = FaceMaskDetection.getModelInstance();

            Mat resizedFrame = new Mat();
            Imgproc.resize(frame, resizedFrame, new Size(400, 400));

            Mat blob = Dnn.blobFromImage(resizedFrame,
                    1.0,
                    new Size(300, 300),
                    new Scalar(104.0, 177.0, 123.0)
            );

            faceNet.setInput(blob);
            Mat detections = faceNet.forward();
            detections = detections.reshape(1, (int) detections.total() / 7);

            int cols = frame.cols();
            int rows = frame.rows();

            // Initialize our list of faces
            List<INDArray> faces = new ArrayList<INDArray>();

            for (int i = 0; i < detections.rows(); ++i) {
                double confidence = detections.get(i, 2)[0];

                if (confidence > THRESHOLD) {
                    // Detected face coordinates
                    int startX = Math.max((int) (detections.get(i, 3)[0] * cols), 0);
                    int startY = Math.max((int) (detections.get(i, 4)[0] * rows), 0);
                    int endX = Math.min((int) (detections.get(i, 5)[0] * cols), cols - 1);
                    int endY = Math.min((int) (detections.get(i, 6)[0] * rows), rows - 1);

                    // The detected face
                    Mat face = frame.submat(new Range(startY, endY), new Range(startX, endX));

                    // Convert face to RGB
                    Mat RGBFace = new Mat();
                    Imgproc.cvtColor(face, RGBFace, Imgproc.COLOR_BGR2RGB);

                    Mat resizedFace = new Mat();
                    Imgproc.resize(RGBFace, resizedFace, new Size(224, 224));

                    INDArray faceINDArray = new NativeImageLoader().asMatrix(resizedFace);
                    new ImagePreProcessingScaler().preProcess(faceINDArray);

                    faces.add(faceINDArray.reshape(1, 224, 224, 3));

                    // Add rectangle to the frame
                    Imgproc.rectangle(frame, new Point(startX, startY), new Point(endX, endY), new Scalar(255, 0, 0), 2);
                }
            }

            int facesSize = faces.size();
            if (facesSize == 0) {
                return -1;
            }

            for (INDArray face : faces) {
                INDArray[] predictions = model.output(face);

                double mask = predictions[0].getDouble(0);
                double withoutMask = predictions[0].getDouble(1);

                if (mask < withoutMask) {
                    return 0;
                }
            }

            return 1;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
}