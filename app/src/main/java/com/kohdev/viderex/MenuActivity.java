package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This activity will provide hte user with options to either follow a route or record a route.
 */
public class MenuActivity extends AppCompatActivity implements SensorEventListener {


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        OpenCVLoader.initDebug();

        checkPermissions();
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

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        testView = (ImageView) findViewById(R.id.imageTest);


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
    }


    /**
     * This method will launch the single match method.
     */
    private void launchSingleMatch() {
        dispatchTakePictureIntent();
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

    public void loadSavedData() {

    }

    private void uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            testView.setImageBitmap(image);
            parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


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


}