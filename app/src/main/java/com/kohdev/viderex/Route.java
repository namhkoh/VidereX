package com.kohdev.viderex;

import android.content.Context;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * Class representing the route. A route is a collection of snapshots.
 */
public class Route {

    private String name;
    private Set<Snapshot> snapshots;
    Vibrator v;

    public Route() {
        this.snapshots = new HashSet<>();
    }

    public Route(String name, Set<Snapshot> snapshots) {
        this.name = name;
        this.snapshots = snapshots;
    }

    /**
     * Sets name to the route.
     * @param name String name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the route.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Utility method to get snapshots
     * @return snapshots
     */
    public Set<Snapshot> getSnapshots() {
        return this.snapshots;
    }

    /**
     * Adds new snapshot.
     * @param imageUri - Image uri
     */
    public void addNewSnapshot(Uri imageUri) {
        Log.e("snapshot taken ", String.valueOf(imageUri));
        snapshots.add(new Snapshot(imageUri));
    }

    public void addNewSnapshot(Uri imageUri, double azimuth, double pitch, double roll) {
        Log.e("snapshot taken: ", String.valueOf(imageUri));
        Log.e("snapshot azimuth: ", String.valueOf(azimuth));
        Log.e("snapshot pitch: ", String.valueOf(pitch));
        Log.e("snapshot roll: ", String.valueOf(roll));
        snapshots.add(new Snapshot(imageUri, azimuth, pitch, roll));
    }

    public void addNewSnapshot(Mat image, Uri imageUri, double azimuth, double pitch, double roll) {
        Log.e("snapshot taken: ", String.valueOf(imageUri));
        Log.e("snapshot azimuth: ", String.valueOf(azimuth));
        Log.e("snapshot pitch: ", String.valueOf(pitch));
        Log.e("snapshot roll: ", String.valueOf(roll));
        snapshots.add(new Snapshot(image, imageUri, azimuth, pitch, roll));
    }

    public void addNewSnapshot(Context context, Uri imageUri, double azimuth, double pitch, double roll) {
        Log.e("context", String.valueOf(context));
        snapshots.add(new Snapshot(context, imageUri, azimuth, pitch, roll));
    }

    public Snapshot getBestMatch(Mat current) {
        Snapshot bestMatch = null;
        for (Snapshot snapshot : snapshots) {
            Log.e("snapshot: ", String.valueOf(snapshot.getPreprocessed_img_uri()));
            double absDiff = computeAbsDiff(current, snapshot.getPreprocessed_img());
            if (absDiff <= 7000) {
                bestMatch = snapshot;
            }
        }
        return bestMatch;
    }

    /**
     * Method to compute absolute difference.
     * @param current - Current Mat image
     * @param goal - Goal image
     * @return difference value.
     */
    public Double computeAbsDiff(Mat current, Mat goal) {
        int range = 10;
        int w = current.width();
        int h = current.height();
        Mat error = Mat.zeros(w, h, CV_8UC1);
        Mat current_norm = new Mat();
        Mat goal_norm = new Mat();
        //Normalize input
        Core.normalize(current, current_norm, 255, Core.NORM_L2);
        Core.normalize(goal, goal_norm, 255, Core.NORM_L2);
        Core.absdiff(current_norm, goal_norm, error);
        Scalar s = Core.sumElems(error);
        System.out.println(s);

//        if (s.val[0] <= 7000) {
//            if (Math.abs(incoming_azimuth - azimuth) <= range && Math.abs(incoming_pitch - pitch) <= range && Math.abs(incoming_roll - roll) <= range) {
//                v.vibrate(100);
//            }
//        }

        return s.val[0];
    }

}
