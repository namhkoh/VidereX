package com.kohdev.viderex;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;


//import org.bytedeco.javacv.AndroidFrameConverter;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.OpenCVFrameConverter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class VideoRecordRoute extends AppCompatActivity implements SensorEventListener {

    VideoView videoView;
    String absPath;
    Uri videoUri;
    Route route;
    EditText routeNameInput;
    InputStream videoStream;
    String json;
    ProgressBar pb;
    int counter = 0;
    //    FileWriter writer;
    ArrayList<Uri> frameListPath = new ArrayList<Uri>();

    private SensorManager mSensorManager;
    Sensor accelerometer, magnetometer;
    float azimuth, pitch, roll;

    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 1;

    private DocumentReference mDocRef = FirebaseFirestore.getInstance().document("RouteObject/C3Mld3o8fOLPaFQttnm5");
    private CollectionReference mCollRef = FirebaseFirestore.getInstance().collection("RouteObject");

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        setContentView(R.layout.activity_video_record_route);

        videoView = (VideoView) findViewById(R.id.routeVideo);
        routeNameInput = findViewById(R.id.routeName);
        pb = (ProgressBar) findViewById(R.id.progressBar);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        route = new Route();
        System.out.println(route);

        Button getFrames = findViewById(R.id.extractFramesButton);
        Button sendFrames = findViewById(R.id.sendFrames);
        Button saveRoute = findViewById(R.id.saveButton);

        getFrames.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                try {
                    //String testPath = "/storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Videos/VID_20210420_131506_3073708790641845353.mp4";
                    //String testPath = "/storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Videos/VID_20210420_133150_6625767613026445277.mp4";
                    //String testPath = "/storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Videos/VID_20210420_134543_7997093301182495455.mp4";
                    extractImageFrame(absPath);
//                    extractImageFrame(absPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        sendFrames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
                intent.putExtra("route_json", json);
                intent.putExtra("image_path", frameListPath);
                startActivity(intent);
            }
        });

        saveRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    storeViews();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

//        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
//        mSensorThread.start();
//        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
//        mSensorManager.registerListener(this, accelerometer, 10, mSensorHandler);
//        mSensorManager.registerListener(this, magnetometer, 10, mSensorHandler);


        dispatchTakeVideoIntent();


    }

    private void dispatchTakeVideoIntent() {
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
//        File newStorageDir = new File(storageDir + "/SensorValues/");
//        newStorageDir.mkdir();
//        try {
//            writer = new FileWriter(new File(newStorageDir, "sensors_" + System.currentTimeMillis() + ".csv"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        final Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            File videoFile = null;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                Log.e("ERROR", String.valueOf(ex));
            }
            if (videoFile != null) {
                Log.e("Video captured ", String.valueOf(true));
                videoUri = FileProvider.getUriForFile(this, "com.kohdev.viderex", videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        }
    }

    private File createVideoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = "VID_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        // Create the storage directory if it does not exist
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.d("APP_TAG", "failed to create directory");
        }

        // Create the storage directory if it does not exist
        Log.e("Storage dir", String.valueOf(storageDir));
        File newStorageDir = new File(storageDir + "/Videos/");
        newStorageDir.mkdir();
        File image = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                newStorageDir     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        absPath = image.getAbsolutePath();
        Log.e("current video", absPath);
        return image;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case 0:
                Log.i("Capture", "Cancelled by the user");
                break;
            case 1:
                Log.e("Capture", "Success!");
                videoStream = null;

                try {
                    videoStream = getContentResolver().openInputStream(videoUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                videoView.setVideoURI(videoUri);
                Log.e("videoUri", String.valueOf(videoUri.getPath()));
                videoView.setZOrderOnTop(true);//this line solve the problem
                videoView.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void extractImageFrame(String absPath) throws IOException {
        System.out.println(absPath);
        double startTime = System.nanoTime();
        FFmpegMediaMetadataRetriever med = new FFmpegMediaMetadataRetriever();
        med.setDataSource(absPath);
        String time = med.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        int videoDuration = Integer.parseInt(time);
        Log.e("video duration", String.valueOf(videoDuration));
        // The frames could be starting +1 the start. Investigate the main looper. 
        for (int i = 1000000; i < videoDuration * 1000; i += 1000000) {
            Bitmap bmp = med.getFrameAtTime(i, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            pb.incrementProgressBy(i / 100000);
            saveBit(bmp);
        }
        Log.e("Measure", "Frame extraction took: " + (System.nanoTime() - startTime) / 1000000000 + "seconds ");

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private File saveBit(Bitmap bmp) throws IOException {
        counter++;
        double startTime = System.nanoTime();
//        Log.e("Measure", "Frame extraction took: " + (System.nanoTime() - startTime) / 1000000 + "ms");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);

        String currentTimestamp = String.valueOf(Instant.now().toEpochMilli());
        String routeName = routeNameInput.getText().toString();
        String frameName = "FRAME_" + currentTimestamp + "_" + routeName + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        // Create the storage directory if it does not exist
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.d("APP_TAG", "failed to create directory");
        }

        // Create the storage directory if it does not exist
        Log.e("Storage dir", String.valueOf(storageDir));
        File newStorageDir = new File(storageDir + "/Frames/");
        newStorageDir.mkdir();
        File image = File.createTempFile(
                frameName,  /* prefix */
                ".jpg",         /* suffix */
                newStorageDir     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        String name = image.getName();
        absPath = image.getAbsolutePath();
        Log.e("current image path", absPath);
        Toast.makeText(this, counter + " extracted", Toast.LENGTH_SHORT).show();
        Log.e("Counter: ", String.valueOf(counter));
        frameListPath.add(Uri.fromFile(image));
        route.addNewSnapshot(Uri.fromFile(image));
        FileOutputStream fo = new FileOutputStream(image);
        fo.write(bytes.toByteArray());
        fo.close();
        return image;
    }

    /**
     * This function will store the taken view into views.
     */
    private void storeViews() throws JSONException {
        route.setName(routeNameInput.getText().toString());

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriSerializer())
                .create();
        json = gson.toJson(route);
        System.out.println(json);

        JSONObject obj = new JSONObject(json);
        // Get route name

        Route route = new Route();
        route.setName(obj.getString("name"));

        // Reload snapshots
        JSONArray snap = obj.getJSONArray("snapshots");

        for (int i = 0; i < snap.length(); i++) {

            JSONObject snapObj = snap.getJSONObject(i);

            float azimuth = (float) snapObj.getDouble("azimuth");
            float pitch = (float) snapObj.getDouble("pitch");
            float roll = (float) snapObj.getDouble("roll");

            Uri imageUri = Uri.parse(snapObj.getString("preprocessed_img_uri"));
//            frameListPath.add(imageUri);
            System.out.println(imageUri);
            //route.addNewSnapshot(getApplicationContext(), imageUri, azimuth, pitch, roll);
            route.addNewSnapshot(imageUri);

//            mDocRef.set(dataToSave);
//            mDocRef.set(dataToSave, SetOptions.merge());
//            mDocRef.set(dataToSave).addOnSuccessListener(new OnSuccessListener<Void>() {
//                @Override
//                public void onSuccess(Void aVoid) {
//                    Log.d("Route storing...", "Document has been saved!");
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Log.w("Route Storing...", "Document was not saved!", e);
//                }
//            });

        }
        Map<String, Object> dataToSave = new HashMap<String, Object>();
        dataToSave.put("route", json);

        mCollRef.add(dataToSave);

        Intent intent = new Intent(this, RouteListViewActivity.class);
        intent.putExtra("route_json", json);
        intent.putExtra("uriList", frameListPath);
        startActivity(intent);
    }


    float[] mGravity;
    float[] mGeomagnetic;

    @SuppressLint("DefaultLocale")
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
//                System.out.println(pitch);
//                String entry = azimuth + "," + pitch + "," + roll + ",";
//                try {
//                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
//                    File newStorageDir = new File(storageDir + "/SensorValues/");
//                    newStorageDir.mkdir();
//                    File file = new File(newStorageDir, "output.csv");
//                    FileOutputStream f = new FileOutputStream(file, true);
//                    try {
//                        f.write(entry.getBytes());
//                        f.flush();
//                        f.close();
//                        Toast.makeText(getBaseContext(), "Data saved", Toast.LENGTH_LONG).show();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }


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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void storeData(float a, float p, float r) {
        String routeName = routeNameInput.getText().toString();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File newStorageDir = new File(storageDir + "/SensorValues/");
        newStorageDir.mkdir();
        String csv = (newStorageDir + routeName + "_sensors.csv");

        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(csv));
            List<String[]> data = new ArrayList<String[]>();
            data.add(new String[]{"Azimuth", "Pitch", "Roll"});
            data.add(new String[]{String.valueOf(a), String.valueOf(p), String.valueOf(r)});
            writer.writeAll(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}