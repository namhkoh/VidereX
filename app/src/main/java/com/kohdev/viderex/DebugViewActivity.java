package com.kohdev.viderex;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

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

public class DebugViewActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat resizedImage;
    private SensorManager mSensorManager;
    private Bitmap goalImage;
    private TextView diffVal;
    private LineChart mChart;
    private Thread thread;
    private boolean plotData = true;


    ImageView storedView;
    ImageView differenceView;
    TextView azimuthTv, pitchTv, rollTv;
    Sensor accelerometer, magnetometer;
    Vibrator v;
    float azimuth, pitch, roll;
    int frameCount;
    int counter;
    int threshold = 7000;
    Bitmap errorBit;
    boolean good_match;
    ArrayList<Uri> framePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_view);

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        storedView = (ImageView) findViewById(R.id.StoredView);
        differenceView = (ImageView) findViewById(R.id.DifferenceView);
        diffVal = (TextView) findViewById(R.id.differenceValue);
        mChart = (LineChart) findViewById(R.id.differenceChart);
        azimuthTv = findViewById(R.id.azimut);
        pitchTv = findViewById(R.id.pitch);
        rollTv = findViewById(R.id.roll);

        mChart.getDescription().setText("Difference value");
        // enable description text
        mChart.getDescription().setEnabled(true);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(15000);
        leftAxis.setAxisMinimum(2000);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

        feedMultiple();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        frameCount = 0;
        counter = 0;
        // Get instance of Vibrator from current Context
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));

        framePath = (ArrayList<Uri>) getIntent().getSerializableExtra("image_path");
        System.out.println(framePath);

        try {
            goalImage = uriToBitmap(framePath.get(counter));
            storedView.setImageBitmap(goalImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Mat goalTmp = bitmapToMat(goalImage);
        resizedImage = new Mat();
        resizedImage = prepare_data(goalTmp, 100, 50);

    }

    public void updateGoal(ArrayList<Uri> imageList) throws IOException {
        try {
            if (good_match) {
                counter++;
                goalImage = uriToBitmap(imageList.get(counter));
                storedView.setImageBitmap(goalImage);
                Mat goalTmp = bitmapToMat(goalImage);
                resizedImage = new Mat();
                resizedImage = prepare_data(goalTmp, 100, 50);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("ERROR", String.valueOf(e));
        }
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
        //gray for gray scale
        Mat frame = inputFrame.rgba();
        Mat resizedFrame = prepare_data(frame, 100, 50);
        if (frameCount == 5) {
            final double diff = computeAbsDiff(resizedImage, resizedFrame);
            try {
                updateGoal(framePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("diff ", String.valueOf(diff));
            runOnUiThread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    diffVal.setText("difference: " + diff);
                    if (plotData) {
                        addEntry((float) diff);
                        plotData = false;
                    }
                    if (counter == framePath.size()) {
                        System.out.println("end of route");
                    }
                }
            });
            frameCount = 0;
        } else {
            frameCount++;
        }
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
        // Implementing a repeating pattern..
        long[] pattern = {100, 100, 100, 100, 100, 100};
        int w = current.width();
        int h = current.height();
        Mat error = Mat.zeros(w, h, CV_8UC1);
        Mat error_image = Mat.zeros(w, h, CV_8UC1);
        Mat current_norm = new Mat();
        Mat goal_norm = new Mat();
        Core.normalize(current, current_norm, 255, Core.NORM_L2);
        Core.normalize(goal, goal_norm, 255, Core.NORM_L2);
        Core.absdiff(current_norm, goal_norm, error);

        Core.absdiff(current, goal, error_image);

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
//            v.vibrate(100);
            v.vibrate(pattern, -1);
            good_match = true;
        } else {
            diffVal.setTextColor(Color.RED);
            good_match = false;
        }
        return s.val[0];
    }

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

    private void addEntry(Float difference) {

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);
            data.addEntry(new Entry(set.getEntryCount(), difference), 0);
//            data.addEntry(new Entry(set.getEntryCount(), event.values[0] + 5), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(150);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());

        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void feedMultiple() {

        if (thread != null) {
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }
}