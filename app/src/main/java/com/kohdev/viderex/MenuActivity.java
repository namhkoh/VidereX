package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.Interpreter;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This activity will provide hte user with options to either follow a route or record a route.
 */
public class MenuActivity extends AppCompatActivity implements SensorEventListener {


    private static final String TAG = "SPEECH";
    final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    public static Bitmap bitmap;
    Uri fileUri;

    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    String currentPhotoPath;

    SensorManager mSensorManager;
    Sensor accelerometer, magnetometer;
    float azimuth, pitch, roll;

    public static SharedPreferences prefs;
    public static HashMap<String, Route> routes;
    Set<Snapshot> snapshots;
    String routeName;
    Intent intent;
    ArrayList<Uri> uriList = new ArrayList<Uri>();
    ImageView testView;
    String json;
    TextView utterance;

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 1000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 1000;
    private static final float DETECTION_THRESHOLD = 0.50f;
    private static final int SUPPRESSION_MS = 1500;
    private static final int MINIMUM_COUNT = 3;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/conv_actions_labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/conv_actions_frozen.tflite";

    // UI elements.
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String LOG_TAG = MenuActivity.class.getSimpleName();

    // Working variables.
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;
    boolean shouldContinue = true;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();

    private List<String> labels = new ArrayList<String>();
    private List<String> displayedLabels = new ArrayList<>();
    private RecognizeCommands recognizeCommands = null;

    private Interpreter tfLite;

    private long lastProcessingTimeMs;
    private Handler handler = new Handler();
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    //Android speech
    private TextToSpeech textToSpeech;
    private ImageButton speechButton;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        OpenCVLoader.initDebug();

        checkPermissions();
        requestMicrophonePermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        Button selectRouteBtn = findViewById(R.id.selectRoute);
        selectRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording_route();
            }
        });

        Button followRouteBtn = findViewById(R.id.followRoute);
        followRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                follow_route();
            }
        });

        Button singleMatchBtn = findViewById(R.id.singleMatch);
        singleMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSingleMatch();
            }
        });

        speechButton = findViewById(R.id.speechButton);
        utterance = findViewById(R.id.utterance);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

//        testView = (ImageView) findViewById(R.id.imageTest);

//        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
//        Log.e(LOG_TAG, "Reading labels from: " + actualLabelFilename);
//        BufferedReader br = null;
//        try {
//            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
//            String line;
//            while ((line = br.readLine()) != null) {
//                labels.add(line);
//                if (line.charAt(0) != '_') {
//                    displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
//                }
//            }
//            br.close();
//        } catch (IOException e) {
//            throw new RuntimeException("Problem reading label file!", e);
//        }

        // Set up an object to smooth recognition results to increase accuracy.
//        recognizeCommands =
//                new RecognizeCommands(
//                        labels,
//                        AVERAGE_WINDOW_DURATION_MS,
//                        DETECTION_THRESHOLD,
//                        SUPPRESSION_MS,
//                        MINIMUM_COUNT,
//                        MINIMUM_TIME_BETWEEN_SAMPLES_MS);
//
//        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
//        try {
//            tfLite = new Interpreter(loadModelFile(getAssets(), actualModelFilename));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        tfLite.resizeInput(0, new int[]{RECORDING_LENGTH, 1});
//        tfLite.resizeInput(1, new int[]{1});

        if (routes == null) {
            routes = new HashMap<>();
        } else {
            json = (String) getIntent().getSerializableExtra("route_json");
            uriList = (ArrayList<Uri>) getIntent().getSerializableExtra("uriList");
            System.out.println(json);
            System.out.println("UriList: " + uriList);
            uriToBitmap(uriList.get(0));
        }
        prefs = getPreferences(Context.MODE_PRIVATE);

//        Speech block
        final SpeechRecognizer mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.d(TAG, "onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningfSpeech");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                Log.e(TAG, "this is on end of speech.");
            }

            @Override
            public void onError(int i) {
                Log.e(TAG, "on Error: " + i);

            }

            @Override
            public void onResults(Bundle bundle) {
                Log.e(TAG, "on Results");
                String userInput = "";
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                userInput = matches.get(0);
                utterance.setText("Detected utterance: " + userInput);
                Log.e("ALL MATCHES", userInput);
                runCommand(userInput);
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        speechButton.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    mSpeechRecognizer.stopListening();
//                    stopRecognition();
//                    stopRecording();
                    break;

                case MotionEvent.ACTION_DOWN:
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                    System.out.println("listening");
//                    startRecording();
//                    startRecognition();

                    break;
            }
            return false;
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {

        });

        //Results of pressing the speech button.
        textToSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        speechButton.setEnabled(false);
                    }

                    @Override
                    public void onDone(String utteranceId) {

                    }

                    @Override
                    public void onError(String utteranceId) {

                    }
                });


    }

    /**
     * This function will take the user input utterance and run the command.
     *
     * @param userUtterance String value of user input utterance
     */
    private void runCommand(String userUtterance) {
        System.out.println(userUtterance);
//        switch (userUtterance) {
//            case "single match":
//                initTTS("Entering single matching activity");
//                launchSingleMatch();
//                break;
//            case "record route":
//                initTTS("Entering record route activity");
//                recording_route();
//                break;
//            case "follow route":
//                initTTS("Entering activity to select route");
//                follow_route();
//                break;
//        }
        if (userUtterance.contains("single match") || userUtterance.contains("single") || userUtterance.contains("match")) {
            initTTS("Opening single match activity");
            launchSingleMatch();
        } else if (userUtterance.contains("record route") || userUtterance.contains("record") || userUtterance.contains("create")) {
            initTTS("Opening record route activity");
            recording_route();
        } else if (userUtterance.contains("follow route") || userUtterance.contains("follow") || userUtterance.contains("select")) {
            initTTS("Opening follow route activity");
            follow_route();
        }
    }

    /**
     * A voice reads the text given in the method.
     *
     * @param selectedText The String text that is read.
     */
    private void initTTS(String selectedText) {
        //textToSpeech.setSpeechRate(testingVal);
        int speechStatus = textToSpeech.speak(selectedText, TextToSpeech.QUEUE_ADD, null, "1");
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }


    /**
     * This method will launch the single match method.
     */
    private void launchSingleMatch() {
        //dispatchTakePictureIntent();
        Intent intent = new Intent(this, AlternativeComputation.class);
        startActivity(intent);
    }

    /**
     * Method to route the user to select route
     */
    private void recording_route() {
//        Intent intent = new Intent(this, RecordRouteActivity.class);
//        startActivity(intent);
        Intent intent = new Intent(this, VideoRecordRoute.class);
        startActivity(intent);
    }

    /**
     * Method to route the user to follow route
     */
    private void follow_route() {

        Intent intent = new Intent(this, RouteListViewActivity.class);
        //intent.putExtra("images", uriList);
        startActivity(intent);

//        Intent intent = new Intent(this, FollowRouteActivity.class);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
//        intent.putExtra("uriList",uriList);
//        startActivity(intent);

        //        Intent intent = new Intent(this, SelectRouteActivity.class);
        //        startActivity(intent);
    }

    /**
     * Utility method to convert the URI image path to Bitmap image.
     *
     * @param selectedFileUri
     */
    private void uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
//            testView.setImageBitmap(image);
            parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will launch the image capture intent.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("ERROR", String.valueOf(ex));
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Log.e("Image captured", String.valueOf(true));
                fileUri = FileProvider.getUriForFile(this,
                        "com.kohdev.viderex",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }
    }

    /**
     * Method to create the image file.
     *
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Create the storage directory if it does not exist
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.d("APP_TAG", "failed to create directory");
        }

        // Create the storage directory if it does not exist
        Log.e("Storage dir", String.valueOf(storageDir));
        File newStorageDir = new File(storageDir + "/Navigant/");
        newStorageDir.mkdir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                newStorageDir     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.e("current photo", currentPhotoPath);
        return image;
    }

    /**
     * OnActivity result method
     *
     * @param requestCode - requestCode used for defining the action
     * @param resultCode  - the result code of the action
     * @param intent      - Android intent object
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
                switch (resultCode) {
                    case 0:
                        Log.i("CAPTURE", "Cancelled by User");
                        break;
                    case -1:
                        Log.e("Capture", "Success");
                        InputStream image_stream = null;
                        try {
                            image_stream = getContentResolver().openInputStream(fileUri);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        this.bitmap = BitmapFactory.decodeStream(image_stream);

                        intent = new Intent(MenuActivity.this, SingleMatchActivity.class);
                        intent.putExtra("azimuth", azimuth);
                        intent.putExtra("pitch", pitch);
                        intent.putExtra("roll", roll);
                        intent.putExtra("goalImagePath", currentPhotoPath);
                        startActivity(intent);
                }
        }
    }

    /**
     * Checking for device permissions.
     */
    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        }


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * Method that checks for android permission results
     *
     * @param requestCode  - Request code
     * @param permissions  - Permission
     * @param grantResults - the results from the grant
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:

            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permission status: ", "Success");
                } else {
                    Log.e("Permission status: ", "Failed");
                }
                return;
        }
    }

    /**
     * Method to request the microphone.
     */
    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }


    float[] mGravity;
    float[] mGeomagnetic;

    /**
     * onSensorChanged method
     *
     * @param event - event type
     */
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
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
                pitch = orientation[1];
                roll = orientation[2];
//                System.out.println("azimuth: " + azimuth + " " + "pitch: " + pitch + " " + "roll: " + roll);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.unregisterListener(this);
    }


    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    /**
     * OnListen recognize method.
     */
    private void recognize() {

        Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[][] floatInputBuffer = new float[RECORDING_LENGTH][1];
        float[][] outputScores = new float[1][labels.size()];
        int[] sampleRateList = new int[]{SAMPLE_RATE};

        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            long startTime = new Date().getTime();
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                int maxLength = recordingBuffer.length;
                int firstCopyLength = maxLength - recordingOffset;
                int secondCopyLength = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                floatInputBuffer[i][0] = inputBuffer[i] / 32767.0f;
            }

            Object[] inputArray = {floatInputBuffer, sampleRateList};
            Map<Integer, Object> outputMap = new HashMap<>();
            System.out.println(outputMap);
            outputMap.put(0, outputScores);

            // Run the model.
            tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

            // Use the smoother to figure out if we've had a real recognition event.
            long currentTime = System.currentTimeMillis();
            final RecognizeCommands.RecognitionResult result =
                    recognizeCommands.processLatestResults(outputScores[0], currentTime);
            lastProcessingTimeMs = new Date().getTime() - startTime;

            System.out.println(result.foundCommand);

//            if (!result.foundCommand.startsWith("_") && result.isNewCommand) {
//                int labelIndex = -1;
//                for (int i = 0; i < labels.size(); ++i) {
//                    if (labels.get(i).equals(result.foundCommand)) {
//                        labelIndex = i;
//                    }
//                }
//                final String score = Math.round(result.score * 100) + "%";
//                System.out.println(result.foundCommand + " " + score);
//            }
//            runOnUiThread(
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            // If we do have a new command, highlight the right list entry.
//                            if (!result.foundCommand.startsWith("_") && result.isNewCommand) {
//                                int labelIndex = -1;
//                                for (int i = 0; i < labels.size(); ++i) {
//                                    if (labels.get(i).equals(result.foundCommand)) {
//                                        labelIndex = i;
//                                    }
//                                }
//                                final String score = Math.round(result.score * 100) + "%";
//                            }
//                        }
//                    });
            try {
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
            } catch (InterruptedException e) {
            }
        }

        Log.v(LOG_TAG, "End recognition");
    }


    /**
     * Method that starts recording the audio.
     */
    public synchronized void startRecording() {
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();
    }

    /**
     * Method to execute to record the user's audio input.
     */
    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            int newRecordingOffset = recordingOffset + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }
        }

        record.stop();
        record.release();
    }

    /**
     * TF method to start recognition.
     */
    public synchronized void startRecognition() {
        if (recognitionThread != null) {
            return;
        }
        shouldContinueRecognition = true;
        recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                recognize();
                            }
                        });
        recognitionThread.start();
    }

    /**
     * Stop Recognition method.
     */
    public synchronized void stopRecognition() {
        if (recognitionThread == null) {
            return;
        }
        shouldContinueRecognition = false;
        recognitionThread = null;
    }

    public synchronized void stopRecording() {
        if (recordingThread == null) {
            return;
        }
        shouldContinue = false;
        recordingThread = null;
    }


}