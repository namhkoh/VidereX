package com.kohdev.viderex;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Vibrator;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.karlotoy.perfectune.instance.PerfectTune;

import net.mabboud.android_tone_player.ContinuousBuzzer;
import net.mabboud.android_tone_player.OneTimeBuzzer;

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
import java.util.ArrayList;
import java.util.Arrays;

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
    int threshold = 7000;
    Sensor accelerometer, magnetometer;
    Vibrator v;
    float azimuth, pitch, roll;
    float incoming_azimuth, incoming_pitch, incoming_roll;
    TextView azimuthTv, pitchTv, rollTv;
    int frameCount;
    Bitmap errorBit;
    private TextToSpeech textToSpeech;
    SeekBar seekBar;


    // Charting tools
    private LineChart mChart;
    private Thread thread;
    private boolean plotData = true;

    PerfectTune perfectTune = new PerfectTune();
    ArrayList<Integer> badTones = new ArrayList<Integer>(Arrays.asList(0, 100, 200, 300, 400));
    ArrayList<Integer> goodTones = new ArrayList<Integer>(Arrays.asList(500, 600, 700, 800, 900, 10000));


    public static double maxFreq = 1760.0;
    public static double minFreq = 220.0;
    public static double toneLength = 75.0;
    public static boolean useOldSound = false;
//    // Tone parameters
//    private static final int TONE_BAD = ToneGenerator.TONE_DTMF_1;
//    private static final int TONE_OK = ToneGenerator.TONE_DTMF_5;
//    private static final int TONE_GOOD = ToneGenerator.TONE_PROP_ACK;
//    private static final int TONE_MISORIENTED = ToneGenerator.TONE_SUP_ERROR;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO: Insert a debug mode for the difference image
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_single_match);


        Intent intent = getIntent();

        mOpenCvCameraView = findViewById(R.id.OpenCVCamera);
        mOpenCvCameraView.setCvCameraViewListener(this);

        differenceImageView = findViewById(R.id.differenceView);
        diffVal = findViewById(R.id.differenceValue);

        azimuthTv = findViewById(R.id.azimut);
        pitchTv = findViewById(R.id.pitch);
        rollTv = findViewById(R.id.roll);

        mChart = (LineChart) findViewById(R.id.differenceChart);

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

        seekBar = (SeekBar) findViewById(R.id.thresholder);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold = progress + 1000;
                System.out.println(threshold);
                Toast.makeText(getApplicationContext(), "seekbar progress: " + threshold, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });

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
        leftAxis.setAxisMaximum(12000);
        leftAxis.setAxisMinimum(4400);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

        feedMultiple();
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String wideAngleCameraId = getWideAngleCameraId(cameraManager);
            Log.d("cheesecake",wideAngleCameraId);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                    if (plotData) {
                        addEntry((float) diff);
                        plotData = false;
                    }
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
            perfectTune.stopTune();
        }
    }


    @Override
    protected void onDestroy() {
        // Destroying the camera.
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        perfectTune.stopTune();
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

    private String getWideAngleCameraId(CameraManager cameraManager) throws CameraAccessException {
        String[] cameraIdList = cameraManager.getCameraIdList();
        for (String cameraId : cameraIdList) {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            Float maxFocusDistance = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            if (maxFocusDistance != null && maxFocusDistance <= 0.5) { // This is a rough estimate, you may need to adjust this value
                return cameraId;
            }
        }
        return null;
    }

    /**
     * This method will compute the absolute difference of two images.
     *
     * @param current - Current view
     * @param goal    - Goal view
     * @return Difference value
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Double computeAbsDiff(Mat current, Mat goal) {
        int range = 1;
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
                    differenceImageView.setImageBitmap(errorBit);
                }
            });
        }
//    ArrayList<Integer> badTones = new ArrayList<Integer>(Arrays.asList(0,100,200,300,400));
//    ArrayList<Integer> goodTones = new ArrayList<Integer>(Arrays.asList(500,600,700,800,900,10000));
        Scalar s = Core.sumElems(error);
        System.out.println(s);
        if (s.val[0] <= threshold && Math.abs(incoming_azimuth - azimuth) <= range && Math.abs(incoming_pitch - pitch) <= range && Math.abs(incoming_roll - roll) <= range) {
            diffVal.setTextColor(Color.GREEN);
            azimuthTv.setTextColor(Color.GREEN);
            pitchTv.setTextColor(Color.GREEN);
            rollTv.setTextColor(Color.GREEN);
            v.vibrate(100);
        } else {
            diffVal.setTextColor(Color.RED);
            azimuthTv.setTextColor(Color.RED);
            pitchTv.setTextColor(Color.RED);
            rollTv.setTextColor(Color.RED);
        }
//        perfectTune.setTuneFreq(setTune(s.val[0]));
        perfectTune.setTuneFreq(setTune(s.val[0]));
        perfectTune.playTune();

        return s.val[0];
    }

    private int setFrequency(double difference) {
        int normalizedValue = 0;
        normalizedValue = (int) (difference - 3500 / 16000 - difference * 1000);
        System.out.println(Math.abs(normalizedValue));
        Log.e("freq", String.valueOf(Math.abs(normalizedValue)));
        return Math.abs(normalizedValue);
    }

    private int setTune(double difference) {
        int tuneValue = 0;
        if (difference >= 16000) {
            tuneValue = 40;
        } else if (difference >= 15500) {
            tuneValue = 50;
        } else if (difference >= 15000) {
            tuneValue = 60;
        } else if (difference >= 14500) {
            tuneValue = 70;
        } else if (difference >= 14000) {
            tuneValue = 80;
        } else if (difference >= 13500) {
            tuneValue = 90;
        } else if (difference >= 13000) {
            tuneValue = 100;
        } else if (difference >= 12500) {
            tuneValue = 110;
        } else if (difference >= 12000) {
            tuneValue = 120;
        } else if (difference >= 11500) {
            tuneValue = 130;
        } else if (difference >= 11000) {
            tuneValue = 140;
        } else if (difference >= 10500) {
            tuneValue = 150;
        } else if (difference >= 10000) {
            tuneValue = 160;
        } else if (difference >= 9500) {
            tuneValue = 170;
        } else if (difference >= 9000) {
            tuneValue = 180;
        } else if (difference >= 8500) {
            tuneValue = 190;
        } else if (difference >= 8000) {
            tuneValue = 200;
        } else if (difference >= 7500) {
            tuneValue = 210;
        } else if (difference >= 7000) {
            tuneValue = 220;
        } else if (difference >= 6500) {
            tuneValue = 230;
        } else if (difference >= 6000) {
            tuneValue = 240;
        }
        return tuneValue;
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

    /**
     * Mat to bitmap method
     * @param orig_image - Original image
     * @return Bitmap image
     */
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

    /**
     * A voice reads the text given in the method.
     *
     * @param selectedText The String text that is read.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initTTS(String selectedText) {
        int speechStatus = textToSpeech.speak(selectedText, TextToSpeech.QUEUE_FLUSH, null, "1");
        textToSpeech.setSpeechRate((float) 1.5);
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }

    /**
     * Add entry method for the chart
     * @param difference - the difference value
     */
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

    /**
     * Method for the create set.
     */
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

    /**
     * Chart utility method to feed.
     */
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