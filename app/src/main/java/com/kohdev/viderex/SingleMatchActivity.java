package com.kohdev.viderex;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Vibrator;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;

/**
 * This activity will handle the Single matching ability between the current view and the image taken
 */
public class SingleMatchActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private SensorManager mSensorManager;
    private Bitmap goalImage;
    private Mat resizedImage;
    private ImageView differenceImageView;
    private TextView diffVal;

    int frameCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_single_match);

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // vibrator test
//        // Get instance of Vibrator from current Context
//        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//
//        // Vibrate for 400 milliseconds
//        v.vibrate(400);

        differenceImageView = findViewById(R.id.differenceView);
        diffVal = findViewById(R.id.differenceValue);

        frameCount = 0;

        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        Intent intent = getIntent();
        String goalImagePath = String.valueOf(intent.getStringExtra("goalImagePath"));
        Log.e("goalImage", goalImagePath);
        goalImage = loadBitmapFromUrl(goalImagePath);
        Log.e("width", String.valueOf(goalImage.getWidth()));
        Log.e("height", String.valueOf(goalImage.getHeight()));

        Mat goalTmp = bitmapToMat(goalImage);

        Mat goalTester = new Mat(goalImage.getHeight(), goalImage.getWidth(), CV_8UC1);

        resizedImage = new Mat();
        Size size = new Size(1920, 864);
        Imgproc.resize(goalTmp, resizedImage, size);

//        ImageView test = findViewById(R.id.testView);
//        test.setImageBitmap(convertMatToBitMap(resizedImage));

//        Log.e("goalTmp_resized width", String.valueOf(resizedImage.width()));
//        Log.e("goalTmp_resized height", String.valueOf(resizedImage.height()));

//        Log.e("goalTest_w",String.valueOf(goalTester.width()));
//        Log.e("goalTest_h",String.valueOf(goalTester.height()));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    static private Mat bitmapToMat(Bitmap image) {

        Mat mat = null;

        if (image != null) {

            int w = image.getWidth();
            int h = image.getHeight();

            Bitmap.Config config = image.getConfig();

            if (config == Bitmap.Config.ARGB_8888 && w > 0 && h > 0) {

                mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC1 );
                Utils.bitmapToMat(image, mat);
            } else {
                Log.e("Snapshot", "Error loading snapshot image: Incorrect bitmap type, expected ARGB_8888.");
            }
        }
        else {
            Log.e("Snapshot", "NULL Bitmap object passed for conversion to Mat.");
        }

        return mat;
    }

    private static Bitmap convertMatToBitMap(Mat input) {
        Bitmap bmp = null;
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2GRAY);

        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }
        return bmp;
    }

    /**
     * This function will resize,greyscale and apply histeq to the input images and normalize!
     * returns: preprocessed image.
     */
    private void preprocessImg() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
//        Imgproc.cvtColor(frame,frame, Imgproc.COLOR_BGR2GRAY);
        if (frameCount == 5) {
            final double diff = getDiff(resizedImage, frame);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    diffVal.setText("Difference: " + diff);
                }
            });
            frameCount = 0;
        } else {
            frameCount++;
        }
        return frame;
    }

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

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        // Destroying the camera.
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    private static Double getDiff(Mat current, Mat goal) {
        int w = current.width();
        int h = current.height();
        Mat difference = Mat.zeros(w, h, CV_8UC1);
        double num_pixels = current.size().area();
        // Change the images to grey scale
//
//        Core.normalize(current,current,0,255,Core.NORM_L2);
//        Core.normalize(goal,goal,0,255,Core.NORM_L2);
//
//
//        System.out.println(current);

        Core.absdiff(current, goal, difference);
        difference.convertTo(difference, CvType.CV_32F);

        Scalar s = Core.sumElems(difference);
        double sse = s.val[0];

        // Mean and root-mean squared error
//        double mse  = sse / num_pixels;
//        double rmse = Math.sqrt(mse);
        Log.e("Similarity metric", String.valueOf(sse));


//        if ((goal != null) && (current != null)) {
//            if ((current.height() == goal.height()) && (current.width() == goal.width()) && (current.type() == CV_8UC1) && (goal.type() == CV_8UC1)) {
//
//                // Calculate per-pixel absolute differences of current and goal images
//                Core.absdiff(current, goal, error);
//
//            }
//        }
        return sse;
    }

    /**
     * This method will compute the absolute difference of two images.
     *
     * @param current
     * @param goal
     * @return error
     */
    private static Mat computeAbsDiff(Mat current, Mat goal) {
        int w = current.width();
        int h = current.height();
        Mat difference = Mat.zeros(w, h, CV_8UC1);
        // Change the images to grey scale
//
//        Core.normalize(current,current,0,255,Core.NORM_L2);
//        Core.normalize(goal,goal,0,255,Core.NORM_L2);
//
//
//        System.out.println(current);

        Core.absdiff(current, goal, difference);
        difference.convertTo(difference, CvType.CV_32F);

        Scalar s = Core.sumElems(difference);
        double sse = s.val[0];
        Log.e("Similarity metric", String.valueOf(sse));


//        if ((goal != null) && (current != null)) {
//            if ((current.height() == goal.height()) && (current.width() == goal.width()) && (current.type() == CV_8UC1) && (goal.type() == CV_8UC1)) {
//
//                // Calculate per-pixel absolute differences of current and goal images
//                Core.absdiff(current, goal, error);
//
//            }
//        }
        return difference;
    }


    //TODO: add try/catch for error handling
    private Bitmap loadBitmapFromUrl(String goalImageFilePath) {
        File imgFile = new File(goalImageFilePath);
        Bitmap goalImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        ImageView goalImageView = findViewById(R.id.goalView);
        goalImageView.setImageBitmap(goalImage);
        return goalImage;
    }

}