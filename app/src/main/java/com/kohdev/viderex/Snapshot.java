package com.kohdev.viderex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This class represents an instance of a snapshot.
 */
public class Snapshot {

    private Mat preprocessed_img;
    private Uri preprocessed_img_uri;
    private double azimuth, pitch, roll;

    public Snapshot(Uri imageUri) {
        this.preprocessed_img_uri = imageUri;
    }

    public Snapshot(Mat image, Uri imageUri, double azimuth, double pitch, double roll) {
        this.preprocessed_img = prep_img(image, 100, 50);
        this.preprocessed_img_uri = imageUri;
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }

    public Snapshot(Context context, Uri imageUri, double azimuth, double pitch, double roll) {

        this.preprocessed_img_uri = imageUri;

        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;

        Mat mat = uriToMat(context, imageUri);

        this.preprocessed_img = prep_img(mat, 100, 50);

    }

    public Snapshot(Mat frame, float azimuth, float pitch, float roll) {
        this.preprocessed_img_uri = null;

        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;


        this.preprocessed_img = prep_img(frame, 100, 50);
    }

    public Mat getPreprocessed_img() {
        return this.preprocessed_img;
    }

    public Uri getPreprocessed_img_uri() {
        return this.preprocessed_img_uri;
    }

    public Bitmap getProcessedBitmap() {
        return matToBitmap(this.preprocessed_img);
    }

    public static Bitmap UriToBitmap(Context context, Uri imgPath) {
        Bitmap image = null;
        try {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(imgPath, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.e("Snapshot", "Error loading snapshot image bitmap from URI.", e);
        }
        return image;
    }


    static private Mat uriToMat(Context context, Uri imageUri) {

        return bitmapToMat(UriToBitmap(context, imageUri));
    }


    static private Mat bitmapToMat(Bitmap image) {

        Mat mat = null;

        if (image != null) {

            int w = image.getWidth();
            int h = image.getHeight();

            Bitmap.Config config = image.getConfig();

            if (config == Bitmap.Config.ARGB_8888 && w > 0 && h > 0) {

                mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC1);
                Utils.bitmapToMat(image, mat);
            } else {
                Log.e("Snapshot", "Error loading snapshot image: Incorrect bitmap type, expected ARGB_8888.");
            }
        } else {
            Log.e("Snapshot", "NULL Bitmap object passed for conversion to Mat.");
        }

        return mat;
    }

    private Bitmap matToBitmap(Mat orig_image) {

        // Clone image! Important otherwise colour conversion is applied to original...
        Mat mat_image = orig_image.clone();

        int w = mat_image.width();
        int h = mat_image.height();
        int type = mat_image.type();

        // Convert image to bitmap
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        final Bitmap ciBmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap

        if (type == CV_8UC1) {
            Imgproc.cvtColor(mat_image, mat_image, Imgproc.COLOR_GRAY2RGBA);
        }
        Utils.matToBitmap(mat_image, ciBmp);

        return ciBmp;
    }

    private Mat prep_img(Mat img, int width, int height) {
        // Resize image
        Mat resizeImage = new Mat();
        Size size = new Size(width, height);
        Imgproc.resize(img, resizeImage, size);
        // Gray scale the image
        Imgproc.cvtColor(resizeImage, resizeImage, Imgproc.COLOR_BGR2GRAY);
        // Apply Histogram eq to image
        Imgproc.equalizeHist(resizeImage, resizeImage);
        return resizeImage;
    }
}
