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

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;

/**
 * This activity will handle the Single matching ability between the current view and the image taken
 */
public class SingleMatchActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
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

        differenceImageView = findViewById(R.id.differenceView);
        diffVal = findViewById(R.id.differenceValue);

        frameCount = 0;

        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));

        // Loading goal image from previous activity
        Intent intent = getIntent();
        String goalImagePath = String.valueOf(intent.getStringExtra("goalImagePath"));
        goalImage = loadBitmapFromUrl(goalImagePath);
        // Resizing image to match the preview frame
        Mat goalTmp = bitmapToMat(goalImage);
        resizedImage = new Mat();
//        resizedImage = prepare_data(goalTmp, 1920, 864);


//        ImageView goalImageView = findViewById(R.id.goalView);
//        goalImageView.setImageBitmap(convertMatToBitMap(resizedImage));

        Size size = new Size(1920, 864);
        Imgproc.resize(goalTmp, resizedImage, size);
        //debug to show output.

//        ImageView testView = findViewById(R.id.testView);
//        testView.setImageBitmap(convertMatToBitMap(resizedImage));

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
        // Apply Histogram equlisation to image
        Imgproc.equalizeHist(resizeImage, resizeImage);
        return resizeImage;
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
        //.gray for gray scale
        Mat frame = inputFrame.rgba();
//        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.equalizeHist(frame, frame);

        if (frameCount == 3) {
            final double diff = computeAbsDiff(resizedImage, frame);

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
        Log.e("frame width", String.valueOf(frame.width()));
        Log.e("frame height", String.valueOf(frame.height()));
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


    private static Double computeRMSE(Mat current, Mat goal) {
        int w = current.width();
        int h = current.height();
        Mat difference = Mat.zeros(w, h, CV_8UC1);
        double num_pixels = current.size().area();

        // Compute the absolute difference across the two images.
        Core.absdiff(current, goal, difference);
        //CV_32F
        difference.convertTo(difference, CV_32F);
        difference = difference.mul(difference);

        Scalar s = Core.sumElems(difference);
        double sse = s.val[0];

        // Mean and root-mean squared error
        double mse  = sse / num_pixels;
        double rmse = Math.sqrt(mse);
        Log.e("Similarity metric", String.valueOf(rmse));


//        if ((goal != null) && (current != null)) {
//            if ((current.height() == goal.height()) && (current.width() == goal.width()) && (current.type() == CV_8UC1) && (goal.type() == CV_8UC1)) {
//
//                // Calculate per-pixel absolute differences of current and goal images
//                Core.absdiff(current, goal, error);
//
//            }
//        }
        return rmse;
    }

    /**
     * This method will compute the absolute difference of two images.
     *
     * @param current - Current view
     * @param goal - Goal view
     * @return Difference value
     */
    private static Double computeAbsDiff(Mat current, Mat goal) {
        int w = current.width();
        int h = current.height();
        Mat m_norm = Mat.zeros(w,h,CV_8UC1);

        Core.absdiff(current,goal,m_norm);
        Scalar s = Core.sumElems(m_norm);
        System.out.println(s);
        return 0.0;
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