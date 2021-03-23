package com.kohdev.viderex;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Vibrator;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;

/**
 * This activity will handle the Single matching ability between the current view and the image taken
 */
public class SingleMatchActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Bitmap goalImage;
    private Mat resizedImage;
    private ImageView differenceImageView;
    private SensorManager mSensorManager;
    private TextView diffVal;
    private TextView matchQuality;
    int threshold = 7000;
    Sensor accelerometer, magnetometer;
    Vibrator v;
    float azimuth, pitch, roll;
    float incoming_azimuth, incoming_pitch, incoming_roll;
    TextView azimuthTv, pitchTv, rollTv;
    int frameCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO: Insert a debug mode for the difference image
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_single_match);
        Intent intent = getIntent();

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

//        differenceImageView = findViewById(R.id.differenceView);
        diffVal = findViewById(R.id.differenceValue);
        matchQuality = findViewById(R.id.matchQuality);

        azimuthTv = findViewById(R.id.azimut);
        pitchTv = findViewById(R.id.pitch);
        rollTv = findViewById(R.id.roll);

        incoming_azimuth = intent.getFloatExtra("azimuth", -1);
        incoming_pitch = intent.getFloatExtra("pitch", -1);
        incoming_roll = intent.getFloatExtra("roll", -1);

        System.out.println("azimuth_inc : " + incoming_azimuth + " " + "pitch_inc : " + incoming_pitch + " " + "roll_inc: " + incoming_roll);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        frameCount = 0;
        // Get instance of Vibrator from current Context
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));

        // Loading goal image from previous activity
        String goalImagePath = String.valueOf(intent.getStringExtra("goalImagePath"));
        goalImage = loadBitmapFromUrl(goalImagePath);
        // Resizing image to match the preview frame
        Mat goalTmp = bitmapToMat(goalImage);
        resizedImage = new Mat();
        resizedImage = prepare_data(goalTmp, 100, 50);


    }

    /**
     * This method will prepare the data to be used for the absolute diffferencing function
     *
     * @param img    input image
     * @param width  input image width
     * @param height - input image height
     * @return resizedImage processed
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


    private static Double computeRMSE(Mat current, Mat goal) {
        int w = current.width();
        int h = current.height();
        Mat difference = Mat.zeros(w, h, CV_8UC1);
        double num_pixels = current.size().area();

        Core.absdiff(current, goal, difference);
        difference.convertTo(difference, CV_32F);
        difference = difference.mul(difference);

        Scalar s = Core.sumElems(difference);
        System.out.println(s);
        double sse = s.val[0];

        // Mean and root-mean squared error
        double mse = sse / num_pixels;
        double rmse = Math.sqrt(mse);
        Log.e("error ", String.valueOf(rmse));
        return rmse;
    }

    /**
     * This method will compute the absolute difference of two images.
     *
     * @param current - Current view
     * @param goal    - Goal view
     * @return Difference value
     */
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
            v.vibrate(100);
        } else {
            diffVal.setTextColor(Color.RED);
        }

        return s.val[0];
    }

    /**
     * This method will normalize the input image (L2 norm)
     *
     * @param image - input image
     * @return normalized array
     */
    private static Mat normalize(Mat image) {
        Core.MinMaxLocResult mmr = Core.minMaxLoc(image);
        double range = mmr.maxVal - mmr.minVal;
        double amin = mmr.minVal;
        Core.subtract(image, new Scalar(amin), image);
        Core.multiply(image, new Scalar(255), image);
        Core.divide(image, new Scalar(range), image);
        return image;
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