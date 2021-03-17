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
import org.opencv.core.Mat;
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
        Log.e("width", String.valueOf(goalImage.getWidth()));
        Log.e("height", String.valueOf(goalImage.getHeight()));
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

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
//        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2GRAY);
//        Size size = new Size(150,250);
//        Imgproc.resize(frame, frame, size);

//        computeAbsDiff(temp,frame);

        return frame;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if OpenCV has loaded properly
        if (!OpenCVLoader.initDebug()) {
            Log.e("Error", "There is something wrong with OpenCV");
        } else {
            Mat temp = new Mat();
//            getGoalImage();
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
        Mat error = Mat.zeros(w, h, CV_8UC1);
        if ((goal != null) && (current != null)) {
            if ((current.height() == goal.height()) && (current.width() == goal.width()) && (current.type() == CV_8UC1) && (goal.type() == CV_8UC1)) {

                // Calculate per-pixel absolute differences of current and goal images
                Core.absdiff(current, goal, error);

            }
        }
        Log.e("ERROR", String.valueOf(error));
        return error;
    }

    //TODO: add try/catch for error handling
    private Bitmap loadBitmapFromUrl(String goalImageFilePath) {
        File imgFile = new File(goalImageFilePath);
        Bitmap goalImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        ImageView goalImageView = (ImageView) findViewById(R.id.goalView);
        goalImageView.setImageBitmap(goalImage);
        return goalImage;
    }

}