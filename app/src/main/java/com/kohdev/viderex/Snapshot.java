package com.kohdev.viderex;

import android.net.Uri;

import org.opencv.core.Mat;

/**
 * Class representing the snapshot.
 */
public class Snapshot {

    private Mat preprocessedImg;
    private Uri imageUri;

    private double azimuth, pitch, roll;

    public Snapshot(Mat image, double azimuth, double pitch, double roll) {
        this.imageUri =  null;
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }
}
