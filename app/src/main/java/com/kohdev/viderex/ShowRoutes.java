package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

import static org.opencv.core.CvType.CV_8UC1;

public class ShowRoutes extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    ArrayList<Uri> uriList = new ArrayList<Uri>();
    Button change;
    ImageView routeViews;
    int counter;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Bitmap goalImage;
    private Mat resizedImage;
    private ImageView differenceImageView;
    private ImageView goalImageView;
    private SensorManager mSensorManager;
    private TextView diffVal;
    private TextView matchQuality;
    int threshold = 7000;
    Sensor accelerometer, magnetometer;
    Vibrator v;
    float azimuth, pitch, roll;
    TextView azimuthTv, pitchTv, rollTv;
    int frameCount;
    boolean good_match = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_routes);
        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        diffVal = findViewById(R.id.differenceValue);
        azimuthTv = findViewById(R.id.azimut);
        pitchTv = findViewById(R.id.pitch);
        rollTv = findViewById(R.id.roll);
        goalImageView = (ImageView) findViewById(R.id.goalView);

        change = (Button) findViewById(R.id.changeImage);
//        routeViews = (ImageView) findViewById(R.id.routeImages);

        uriList = (ArrayList<Uri>) getIntent().getSerializableExtra("images");
        for (int i = 0; i < uriList.size(); i++) {
//            Log.e("imgs", String.valueOf(uriList.get(i)));
            System.out.println(uriList.get(i));
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        frameCount = 0;
        counter = 0;
        // Get instance of Vibrator from current Context
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));


        try {
            // the counter does not update
            goalImage = uriToBitmap(uriList.get(counter));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Mat goalTmp = bitmapToMat(goalImage);
        resizedImage = new Mat();
        resizedImage = prepare_data(goalTmp, 100, 50);

        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    goalImage = uriToBitmap(uriList.get(counter));
                    Mat goalTmp = bitmapToMat(goalImage);
                    resizedImage = new Mat();
                    resizedImage = changeGoal(uriList);
                    resizedImage = prepare_data(goalTmp, 100, 50);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private Mat changeGoal(ArrayList<Uri> uriList) throws IOException {
        System.out.println("COUNTER: " + counter);
        Log.e("CHANGE", "HAPPENED");
        goalImageView.setImageBitmap(uriToBitmap(uriList.get(counter)));
        Mat goalTmp = bitmapToMat(goalImage);
        resizedImage = new Mat();
        resizedImage = prepare_data(goalTmp, 100, 50);
        counter++;
        return resizedImage;
    }

    private Bitmap uriToBitmap(Uri selectedFileUri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedFileUri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
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
                azimuthTv.setText("Azimuth: " + azimuth);
                pitchTv.setText("Pitch: " + pitch);
                rollTv.setText("Roll: " + roll);
                //System.out.println("azimut: " + azimut +  " " + "pitch: " + pitch + " " + "roll: "+ roll);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
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

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //gray for gray scale
        Mat frame = inputFrame.rgba();
        Mat resizedFrame = prepare_data(frame, 100, 50);

        if (frameCount == 5) {
            final double diff = computeAbsDiff(resizedImage, resizedFrame);
            Log.e("diff ", String.valueOf(diff));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    diffVal.setText("difference: " + diff);
                }
            });
            frameCount = 0;
        } else {
            frameCount++;
        }
        Log.e("frame width", String.valueOf(resizedFrame.width()));
        Log.e("frame height", String.valueOf(resizedFrame.height()));
        return frame;
    }

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

    public Double computeAbsDiff(Mat current, Mat goal) {
        int range = 10;
        int w = current.width();
        int h = current.height();
        Mat error = Mat.zeros(w, h, CV_8UC1);
        Mat current_norm = new Mat();
        Mat goal_norm = new Mat();
        //Normalize input
//        current.convertTo(current_norm, CV_32F, 1.0 / 255, 0);
//        goal.convertTo(goal_norm, CV_32F, 1.0 / 255, 0);
        Core.normalize(current, current_norm, 255, Core.NORM_L2);
        Core.normalize(goal, goal_norm, 255, Core.NORM_L2);
        Core.absdiff(current_norm, goal_norm, error);
        Scalar s = Core.sumElems(error);
        System.out.println(s);
//        if (s.val[0] <= 7000) {
//            if (Math.abs(incoming_azimuth - azimuth) <= range && Math.abs(incoming_pitch - pitch) <= range && Math.abs(incoming_roll - roll) <= range) {
//                v.vibrate(100);
//            }
//        }
        //TODO: logic to set the threshold dynamically
        if (s.val[0] <= threshold) {
            diffVal.setTextColor(Color.GREEN);
            good_match = true;
            v.vibrate(100);
        } else {
            diffVal.setTextColor(Color.RED);
            good_match = false;
        }

        return s.val[0];
    }
}