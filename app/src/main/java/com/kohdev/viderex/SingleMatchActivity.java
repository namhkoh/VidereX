package com.kohdev.viderex;

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

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This activity will handle the Single matching ability between the current view and the image taken
 */
public class SingleMatchActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVLoader.initDebug();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_single_match);

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
    }

//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.i("OpenCV", "OpenCV loaded successfully");
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };

//    private void getGoalImage(){
//        Utils.bitmapToMat(MenuActivity.bitmap, temp);
//        // Convert from RGB to grayscale and histeq
//        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
//        //Imgproc.equalizeHist(temp, temp);
//        Core.flip(temp, temp, 1);
//        Size size = new Size(150,250);
//        Imgproc.resize(temp, temp, size);
//    }


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
        Imgproc.cvtColor(frame,frame,Imgproc.COLOR_RGBA2GRAY);
        Size size = new Size(150,250);
        Imgproc.resize(frame, frame, size);

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
}