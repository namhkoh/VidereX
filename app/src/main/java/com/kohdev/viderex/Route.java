package com.kohdev.viderex;

import android.content.Context;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Vibrator;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.HashSet;
import java.util.Set;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * Class representing the route
 */
public class Route {

    private String name;
    Vibrator v;
    private Set<Snapshot> snapshots;

    public Route() {
        this.snapshots = new HashSet<>();
    }

    public Route(String name, Set<Snapshot> snapshots) {
        this.name = name;
        this.snapshots = snapshots;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Set<Snapshot> getSnapshots() {
        return this.snapshots;
    }

    public void addNewSnapshot(Mat image, Uri imageUri, double azimuth, double pitch, double roll) {
        snapshots.add(new Snapshot(image, imageUri, azimuth, pitch, roll));
    }

    public void addNewSnapshot(Context context, Uri imageUri, double azimuth, double pitch, double roll) {
        snapshots.add(new Snapshot(context, imageUri, azimuth, pitch, roll));
    }

    public Snapshot getBestMatch(Mat current) {
        Snapshot bestMatch = null;
        double min_error = Double.MIN_VALUE;

        for (Snapshot snapshot : snapshots) {
            double absDiff = computeAbsDiff(current, snapshot.getPrepoImage());
            if (absDiff <= min_error) {
                bestMatch = snapshot;
                min_error = absDiff;
            }
        }
        return bestMatch;

    }

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


        if (s.val[0] <= 7000) {
            v.vibrate(100);
        }

//        if (s.val[0] <= 7000) {
//            if (Math.abs(incoming_azimuth - azimuth) <= range && Math.abs(incoming_pitch - pitch) <= range && Math.abs(incoming_roll - roll) <= range) {
//                v.vibrate(100);
//            }
//        }

        return s.val[0];
    }
}
