package com.kohdev.viderex;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This activity will handle the Single matching ability between the current view and the image taken
 */
public class SingleMatchActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private SensorManager mSensorManager;
    private Bitmap goalImage;
    private Mat resizedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_single_match);

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        Intent intent = getIntent();
        String goalImagePath = String.valueOf(intent.getStringExtra("goalImagePath"));
        Log.e("goalImage", goalImagePath);
        goalImage = loadBitmapFromUrl(goalImagePath);
        Mat goalTmp = new Mat(goalImage.getHeight(), goalImage.getWidth(), CvType.CV_8UC4);
        resizedImage = new Mat();
        Size size = new Size(1920, 864);
        Imgproc.resize(goalTmp, resizedImage, size);
        Log.e("goalTmp_resized width", String.valueOf(resizedImage.width()));
        Log.e("goalTmp_resized height", String.valueOf(resizedImage.height()));
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

    /**
     * This function will resize,greyscale and apply histeq to the input images and normalize!
     * returns: preprocessed image.
     */
    private void preprocessImg() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2GRAY);
        Mat diff = computeAbsDiff(resizedImage, frame);

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
        Mat dst = new Mat();
        // Change the images to grey scale
//
//        Core.normalize(current,current,0,255,Core.NORM_L2);
//        Core.normalize(goal,goal,0,255,Core.NORM_L2);
//
//
//        System.out.println(current);

        Core.absdiff(current, goal, difference);
        difference.convertTo(difference,CvType.CV_32F);

        Scalar s = Core.sumElems(difference);
        double sse = s.val[0];
        Log.e("Similarity metric",String.valueOf(sse));


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