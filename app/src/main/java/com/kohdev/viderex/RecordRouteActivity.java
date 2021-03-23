package com.kohdev.viderex;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.$Gson$Preconditions;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This activity will handle the following route ability for the user.
 */
public class RecordRouteActivity extends AppCompatActivity {

    TextView viewCount;
    Uri fileUri;
    Route route;
    String currentPhotoPath;
    String routeName;
    EditText routeNameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        setContentView(R.layout.activity_record_route);

        Button recordRoute = findViewById(R.id.startRoute);
        Button saveRoute = findViewById(R.id.finishRoute);

        routeNameInput = findViewById(R.id.routeName);
        viewCount = findViewById(R.id.viewCount);
        route = new Route();

        recordRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureViews();
            }
        });

        saveRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeViews();
            }
        });
    }

    /**
     * This function will store the taken view into views.
     */
    private void storeViews() {
        routeName = routeNameInput.getText().toString();
        route = new Route(routeName, route.getSnapshots());
        // Save the current route as bundle? and pass this to the list view containing all the routes.
        System.out.println("------------------------");
        System.out.println("storing views!");
        System.out.println("route name: " + route.getName());
        System.out.println("snapshots: " + route.getSnapshots());
        System.out.println("route size: " + route.getSnapshots().size());
        System.out.println("------------------------");
        // Send this bundle to the selection activity? 
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

                        //TODO: pass in the actual a,p,r values of the snapshots taken
                        float azimuth = -1;
                        float pitch = -1;
                        float roll = -1;

//                        // Update the stored view count here.
                        route.addNewSnapshot(fileUri);
                        viewCount.setText(route.getSnapshots().size() + " images stored");
                }
        }
    }
}