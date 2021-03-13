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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        checkPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button selectRouteBtn = findViewById(R.id.selectRoute);
        selectRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selecting_route();
            }
        });

        Button followRouteBtn = findViewById(R.id.followRoute);
        followRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                follow_route();
            }
        });
    }

    /**
     * Method to route the user to select route
     */
    private void selecting_route() {
        Intent intent = new Intent(this, RecordRouteActivity.class);
        startActivity(intent);
    }

    /**
     * Method to route the user to follow route
     */
    private void follow_route() {
        Intent intent = new Intent(this, FollowRouteActivity.class);
        startActivity(intent);
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