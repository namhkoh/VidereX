package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

/**
 * This activity will provide hte user with options to either follow a route or record a route.
 */
public class MenuActivity extends AppCompatActivity {

    private Button selectRouteBtn;
    private Button followRouteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        checkPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        selectRouteBtn = findViewById(R.id.selectRoute);
        selectRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent 
            }
        });

        followRouteBtn = findViewById(R.id.followRoute);
    }

    /**
     * Checking permission with the Device.
     */
    private void checkPermission() {
        if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                !(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        ) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            finish();
        }
    }


}