package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

//import org.bytedeco.javacv.AndroidFrameConverter;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class VideoRecordRoute extends AppCompatActivity {

    VideoView videoView;
    String absPath;
    Uri videoUri;
    InputStream videoStream;

    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        setContentView(R.layout.activity_video_record_route);
        videoView = (VideoView) findViewById(R.id.routeVideo);
        Button getFrames = findViewById(R.id.extractFramesButton);

        getFrames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    extractImageFrame(videoUri);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (FFmpegFrameGrabber.Exception e) {
//                    e.printStackTrace();
//                }
            }
        });

        dispatchTakeVideoIntent();
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            File videoFile = null;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                Log.e("ERROR", String.valueOf(ex));
            }
            if (videoFile != null) {
                Log.e("Video captured ", String.valueOf(true));
                videoUri = FileProvider.getUriForFile(this, "com.kohdev.viderex", videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        }
    }

    private File createVideoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = "VID_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        // Create the storage directory if it does not exist
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.d("APP_TAG", "failed to create directory");
        }

        // Create the storage directory if it does not exist
        Log.e("Storage dir", String.valueOf(storageDir));
        File newStorageDir = new File(storageDir + "/Videos/");
        newStorageDir.mkdir();
        File image = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                newStorageDir     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        absPath = image.getAbsolutePath();
        Log.e("current video", absPath);
        return image;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case 0:
                Log.i("Capture", "Cancelled by the user");
                break;
            case 1:
                Log.e("Capture", "Success!");
                videoStream = null;
                try {
                    videoStream = getContentResolver().openInputStream(videoUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                videoView.setVideoURI(videoUri);
                videoView.setZOrderOnTop(true);//this line solve the problem
                videoView.start();
        }
    }

//    private void extractImageFrame(Uri uriPath) throws FileNotFoundException, FFmpegFrameGrabber.Exception {
//        InputStream inputStream = new FileInputStream(String.valueOf(uriPath));
//        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream);
//        AndroidFrameConverter convertToBitmap = new AndroidFrameConverter();
//        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
//
//        grabber.start();
//        Mat img = new Mat();
//
//        for (int frameCount = 0; frameCount < grabber.getLengthInVideoFrames(); frameCount++) {
//            Frame nthFrame = grabber.grabImage();
//            Bitmap bitmap = convertToBitmap.convert(nthFrame);
//            Mat mat = converterToMat.convertToOrgOpenCvCoreMat(nthFrame);
//            System.out.println(bitmap.getWidth());
////            img = preprocessImage(mat);
////            saveImageToInternalStorage(bitmap,img,framecount,dir)
//        }
//
//    }


}