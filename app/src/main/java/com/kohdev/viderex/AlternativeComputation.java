package com.kohdev.viderex;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.karlotoy.perfectune.instance.PerfectTune;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This class will compute the difference value of the current view against the entire set of images instead of stepping through the image list.
 */
public class AlternativeComputation extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Bitmap bestMatchImage;
    private ImageView best_ting;
    private TextView diffVal, bestMatchIndex;


    ImageView storedView;
    ImageView differenceView;
    ImageView bestMatchView;
    int frameCount;
    int counter;
    int threshold = 7000;
    int ind;
    Bitmap errorBit;
    Bitmap memoryBit;
    boolean good_match;

    ArrayList<String> framePath = new ArrayList<String>(Arrays.asList("file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103177410_table_4455769652820168832.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103174408_table_7847894203507333637.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103175153_table_4526138246348440732.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103175888_table_424657081078961058.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103176618_table_3783842397861269742.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103178157_table_3793754083131125710.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103178901_table_8397094335423158983.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103179614_table_1100287399432042272.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103180297_table_2681245729131468553.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103180954_table_2601496536046572860.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103181599_table_8956216974277843310.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103182243_table_999459685166328274.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103182907_table_2936559660522759923.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103183619_table_2668138700630887461.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103184308_table_524995832434001519.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103185014_table_1578785801739261834.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103185767_table_7385242400029980649.jpg",
            "file:///storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/FRAME_1629103186437_table_575752531978606963.jpg"));


    Double min = -1000000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternative_computation);

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        bestMatchView = (ImageView) findViewById(R.id.bestMatch);
        differenceView = (ImageView) findViewById(R.id.DifferenceView);
        best_ting = (ImageView) findViewById(R.id.best_ting);


        diffVal = (TextView) findViewById(R.id.differenceValue);
        bestMatchIndex = (TextView) findViewById(R.id.bestMatchIndex);
        frameCount = 0;
        counter = 0;


        System.out.println(framePath);

    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    /**
     * OnCameraFrame method which will compute the difference value every 5 frames.
     *
     * @param inputFrame
     * @return
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //gray for gray scale
        Double max = 100000.0;

        Mat frame = inputFrame.rgba();
        Mat resizedFrame = prepare_data(frame, 100, 50);
        if (frameCount == 5) {
            for (String img : framePath) {
                try {
                    Bitmap goalImage = uriToBitmap(Uri.parse(img));
                    Mat goalTmp = bitmapToMat(goalImage);
                    Mat resizedImage = prepare_data(goalTmp, 100, 50);
                    final double diff = computeDifference(resizedImage, resizedFrame);
                    if (diff < max) {
                        max = diff;
                        bestMatchImage = goalImage;
                        Log.e("best match at index: ", String.valueOf(framePath.indexOf(img)));
                        ind = framePath.indexOf(img);
                    }
                    runOnUiThread(() -> {
                        best_ting.setImageBitmap(bestMatchImage);
                        bestMatchView.setImageBitmap(goalImage);
                        bestMatchIndex.setText(String.valueOf(ind));
                        diffVal.setText("difference: " + diff);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            frameCount = 0;
        } else {
            frameCount++;
        }
        return frame;
    }

    /**
     * onResume method for controlling the sensor and camera intents.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Check if OpenCV has loaded properly
        if (!OpenCVLoader.initDebug()) {
            Log.e("Error", "There is something wrong with OpenCV");
        } else {
            mOpenCvCameraView.enableView();
        }
    }

    /**
     * onPause activity to control the Camera and tone generation library when application is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    /**
     * onDestroy implementation method used for destroying the camera nad sensor intent activity.
     */
    @Override
    protected void onDestroy() {
        // Destroying the camera.
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }


    public Double computeDifference(Mat currentImage, Mat memoryImage) {
        int w = currentImage.width();
        int h = currentImage.height();

        Mat error = Mat.zeros(w, h, CV_8UC1);
        Mat error_image = Mat.zeros(w, h, CV_8UC1);

        Mat current_norm = new Mat();
        Mat goal_norm = new Mat();

        Core.normalize(currentImage, current_norm, 255, Core.NORM_L2);
        Core.normalize(memoryImage, goal_norm, 255, Core.NORM_L2);

        Core.absdiff(current_norm, goal_norm, error);

        Core.absdiff(currentImage, memoryImage, error_image);
        //Convert error to bitmap
        if (frameCount == 5) {
            errorBit = matToBitmap(error_image);
            System.out.println(errorBit.getHeight());
            System.out.println(errorBit.getWidth());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    differenceView.setImageBitmap(errorBit);
                }
            });
        }
        Scalar s = Core.sumElems(error);
        if (s.val[0] <= threshold) {
            diffVal.setTextColor(Color.GREEN);
            good_match = true;
        } else {
            diffVal.setTextColor(Color.RED);
            good_match = false;
        }

        return s.val[0];
    }

    /**
     * ALL UTILITY METHODS
     */


    /**
     * Utility method that converts the uri to a bitmap image.
     *
     * @param selectedFileUri - URI imageFile
     * @return image - Bitmap image
     * @throws IOException
     */
    private Bitmap uriToBitmap(Uri selectedFileUri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedFileUri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }


    /**
     * Utility method that converts bitmap image to Mat for OpenCV
     *
     * @param image
     * @return
     */
    static private Mat bitmapToMat(Bitmap image) {

        Mat mat = null;

        if (image != null) {

            int w = image.getWidth();
            int h = image.getHeight();

            Bitmap.Config config = image.getConfig();

            if (config == Bitmap.Config.ARGB_8888 && w > 0 && h > 0) {

                mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC1);
                Utils.bitmapToMat(image, mat);
            } else {
                Log.e("Snapshot", "Error loading snapshot image: Incorrect bitmap type, expected ARGB_8888.");
            }
        } else {
            Log.e("Snapshot", "NULL Bitmap object passed for conversion to Mat.");
        }

        return mat;
    }

    /**
     * This method will preprocess the data before feeding to the model
     * The images are resized, normalized and then histogram equalized
     *
     * @param img    - Incoming Mat image
     * @param width  - Image width
     * @param height - Image height
     * @return resizedImage
     */
    private Mat prepare_data(Mat img, int width, int height) {
        // Resize image
        Mat resizeImage = new Mat();
        Size size = new Size(width, height);
        Imgproc.resize(img, resizeImage, size);
        // Gray scale the image
        Imgproc.cvtColor(resizeImage, resizeImage, Imgproc.COLOR_BGR2GRAY);
        // Apply Histogram eq to image
        Imgproc.equalizeHist(resizeImage, resizeImage);
        return resizeImage;
    }


    /**
     * Utility method to convert the Mat image to bitmap image.
     *
     * @param orig_image - Original image
     * @return Bitmap image
     */
    private Bitmap matToBitmap(Mat orig_image) {

        // Clone image! Important otherwise colour conversion is applied to original...
        Mat mat_image = orig_image.clone();

        int w = mat_image.width();
        int h = mat_image.height();
        int type = mat_image.type();

        // Convert image to bitmap
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        final Bitmap ciBmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap

        if (type == CV_8UC1) {
            Imgproc.cvtColor(mat_image, mat_image, Imgproc.COLOR_GRAY2RGBA);
        }
        Utils.matToBitmap(mat_image, ciBmp);

        return ciBmp;
    }


}