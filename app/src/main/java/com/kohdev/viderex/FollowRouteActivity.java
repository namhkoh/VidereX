package com.kohdev.viderex;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This class will handle the recording of the route.
 */
public class FollowRouteActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    public CameraBridgeViewBase.CvCameraViewListener2 camListener;

    private Route route;
    float azimuth, pitch, roll;

    private SensorManager mSensorManager;
    Sensor accelerometer, magnetometer;

    private Vibrator v;
    TextView diff;

    int counter = 0;
    private int frameCount;

    ArrayList<Uri> uriList = new ArrayList<Uri>();
    String json;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        super.onCreate(savedInstanceState);

        uriList = (ArrayList<Uri>) getIntent().getSerializableExtra("uriList");
        json = (String) getIntent().getSerializableExtra("route_json");
        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            String routeName = obj.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println(uriList);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_follow_route);
        mOpenCvCameraView = findViewById(R.id.MainCameraView);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        diff = findViewById(R.id.difference);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        frameCount = 0;

//        // Set up route
//        String routeName;
//        if (savedInstanceState == null) {
//            Bundle extras = getIntent().getExtras();
//            if (extras == null) {
//                routeName = null;
//            } else {
//                routeName = extras.getString("ROUTE_NAME");
//            }
//        } else {
//            routeName = (String) savedInstanceState.getSerializable("");
//        }
//        route = MenuActivity.routes.get(routeName);
//        Log.e("instance route", String.valueOf(route));
//        Log.e("instance route: ", String.valueOf(route.getName()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if OpenCV has loaded properly
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
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
                Mat frame = inputFrame.rgba();
                // look at the dependencies, run a timing function, the more the better
                final Snapshot currentView = new Snapshot(frame, azimuth, pitch, roll);
                Snapshot best_match = route.getBestMatch(currentView.getPreprocessed_img());
                final double diff_val = route.computeAbsDiff(currentView.getPreprocessed_img(), best_match.getPreprocessed_img());
                Log.e("diff", String.valueOf(diff_val));
                if (frameCount == 1) {
//                    final String a = String.valueOf(azimuth);
//                    final String p = String.valueOf(pitch);
//                    final String r = String.valueOf(roll);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            diff.setText(String.valueOf(diff_val));
                            if (diff_val <= 7000) {
                                v.vibrate(100);
                            }
                        }
                    });
                    frameCount = 0;
                } else {
                    frameCount++;
                }
                return frame;
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

    float[] mGravity;
    float[] mGeomagnetic;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
                pitch = orientation[1];
                roll = orientation[2];
//                azimuthTv.setText("Azimuth: " + azimuth);
//                pitchTv.setText("Pitch: " + pitch);
//                rollTv.setText("Roll: " + roll);
                //System.out.println("azimut: " + azimut +  " " + "pitch: " + pitch + " " + "roll: "+ roll);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}