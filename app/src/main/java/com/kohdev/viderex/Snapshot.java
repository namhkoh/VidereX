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
 * d
 */
public class Snapshot {

    private Mat preprocessedImg;
    private Uri imageUri;
    int orientThreshold = 10;

    private double azimuth, pitch, roll;

    public Snapshot(Mat image, Uri imageUri, double azimuth, double pitch, double roll) {

        this.imageUri = imageUri;
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
        this.preprocessedImg = preprocessImage(image, 100, 50);
    }

    public Snapshot(Context context, Uri imageUri, double azimuth, double pitch, double roll) {

        this.imageUri = imageUri;

        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;

        Mat mat = uriToMat(context, imageUri);

        this.preprocessedImg = preprocessImage(mat, 100, 50);

    }

    public Snapshot(Mat frame, float azimuth, float pitch, float roll) {
        this.imageUri = null;

        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;


        this.preprocessedImg = preprocessImage(frame, 100, 50);
    }

    public Mat getPrepoImage() {
        return this.preprocessedImg;
    }

    public Uri getImageUri() {
        return this.imageUri;
    }

    public Bitmap getPrepoBitmap() {
        return matToBitmap(this.preprocessedImg);
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

    static private Mat uriToMat(Context context, Uri imageUri) {

        return bitmapToMat(uriToBitmap(context, imageUri));
    }

    static private Bitmap uriToBitmap(Context context, Uri selectedFileUri) {

        Bitmap image = null;

        try {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.e("Snapshot", "Error loading snapshot image bitmap from URI.", e);
        }

        return image;
    }

    public void restoreImageFromFile(Context context) {

        // Load image data from file URI to bitmap
        if (this.imageUri != null) {

            Bitmap image = uriToBitmap(context, this.imageUri);

            // Convert to Mat and preprocess
            Mat mat = bitmapToMat(image);

            if (mat != null) {
                this.preprocessedImg = preprocessImage(mat, 100, 50);
            } else {
                this.preprocessedImg = Mat.zeros((int) 100, (int) 50, CV_8UC1);
            }
        } else {
            Log.e("Snapshot", "Error attempting to load image from unassigned URI.");
        }
    }

    public boolean[] checkIMU(double azimuth, double pitch, double roll) {

        boolean a = (Math.abs(this.azimuth - azimuth) >= orientThreshold);
        boolean p = (Math.abs(this.pitch - pitch) >= orientThreshold);
        boolean r = (Math.abs(this.roll - roll) >= orientThreshold);

        return new boolean[]{a, p, r};

    }

    /**
     * This method will prepare the data to be used for the absolute diffferencing function
     *
     * @param img    input image
     * @param width  input image width
     * @param height - input image height
     * @return resizedImage processed
     */
    private Mat preprocessImage(Mat img, int width, int height) {
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
