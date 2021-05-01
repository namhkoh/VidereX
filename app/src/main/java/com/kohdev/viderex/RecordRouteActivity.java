package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
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
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This activity will handle the following route ability for the user.
 */
public class RecordRouteActivity extends AppCompatActivity implements SensorEventListener {

    TextView viewCount;
    Uri fileUri;
    Route route;
    String currentPhotoPath;
    String routeName;
    EditText routeNameInput;
    ArrayList<Uri> uriList = new ArrayList<Uri>();
    ImageView testView;

    private SensorManager mSensorManager;
    Sensor accelerometer, magnetometer;
    float azimuth, pitch, roll;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        setContentView(R.layout.activity_record_route);

        Button recordRoute = findViewById(R.id.startRoute);
        Button saveRoute = findViewById(R.id.finishRoute);

        routeNameInput = findViewById(R.id.routeName);
        viewCount = findViewById(R.id.viewCount);
//        testView = (ImageView) findViewById(R.id.testView);
        route = new Route();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        recordRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureViews();
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
    }

    /**
     * This function will store the taken view into views.
     */
    private void storeViews() throws JSONException {
        route.setName(routeNameInput.getText().toString());

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriSerializer())
                .create();
        String json = gson.toJson(route);
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
            uriList.add(imageUri);
            route.addNewSnapshot(getApplicationContext(), imageUri, azimuth, pitch, roll);
        }

        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("route_json", json);
        intent.putExtra("uriList", uriList);
        startActivity(intent);
    }

    private void captureViews() {
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
                startActivityForResult(takePictureIntent, MenuActivity.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
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
        File newStorageDir = new File(storageDir + "/Navigant/Routes/");
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
            case MenuActivity.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
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

                        Bitmap bitmap = BitmapFactory.decodeStream(image_stream);
                        Mat mat = new Mat();
                        Utils.bitmapToMat(bitmap, mat);
                        Log.e("mat ", String.valueOf(mat.width()));
                        Log.e("mat ", String.valueOf(mat.height()));

                        // Update the stored view count here.
                        route.addNewSnapshot(fileUri, azimuth, pitch, roll);
                        viewCount.setText(route.getSnapshots().size() + " images stored");
                }
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
}