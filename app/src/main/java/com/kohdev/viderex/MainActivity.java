package com.kohdev.viderex;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.Camera2Renderer;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    public CameraBridgeViewBase.CvCameraViewListener2 camListener;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.MainCameraView);
        mOpenCvCameraView.setMaxFrameSize(1920, 1080);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if OpenCV has loaded properly
        if (!OpenCVLoader.initDebug()) {
            Log.e("Error", "There is something wrong with OpenCV");
        } else {
            mOpenCvCameraView.enableView();
            run();
        }
    }

    private void run() {
        Log.e("entered", "hello there");
        camListener = new CameraBridgeViewBase.CvCameraViewListener2() {

            @Override
            public void onCameraViewStopped() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onCameraViewStarted(int width, int height) {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat rgb = inputFrame.rgba();
                Mat gray = new Mat();
                Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY);
                return gray;
            }
        };

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(camListener);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

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
}