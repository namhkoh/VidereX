package com.kohdev.viderex;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * This class will launch a camera image dispatch activity to take a picture
 */
public class CaptureView extends AppCompatActivity {

    ImageView imageView;
    Button btOpen;

    /**
     * OnCreate method to instantiate necessary assets.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_view);

        imageView = findViewById(R.id.image_view);
        btOpen = findViewById(R.id.bt_open);

        /**
         * SetOnClick Listener method to open the camera intent activity
         */
        btOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,100);
            }
        });

    }

    /**
     * OnActivity Implementation method that will capture the bitmap image and pass it to the imageView.
     * @param requestCode - Request code that defines the intent
     * @param resultCode - The response from intent
     * @param data - The bundle data that is being passed.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            // get capture image
            Bitmap captureImage = (Bitmap) data.getExtras().get("data");
            // set capture view to image view
            imageView.setImageBitmap(captureImage);
        }
    }
}