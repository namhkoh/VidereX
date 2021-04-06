package com.kohdev.viderex;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;


//import org.bytedeco.javacv.AndroidFrameConverter;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class VideoRecordRoute extends AppCompatActivity {

    VideoView videoView;
    String absPath;
    Uri videoUri;
    InputStream videoStream;
    //ArrayList<String> frameListPath = new ArrayList<String>();
    ArrayList<Uri> frameListPath = new ArrayList<Uri>();

    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("verify", String.valueOf(OpenCVLoader.initDebug()));
        setContentView(R.layout.activity_video_record_route);
        videoView = (VideoView) findViewById(R.id.routeVideo);
        Button getFrames = findViewById(R.id.extractFramesButton);
        Button sendFrames = findViewById(R.id.sendFrames);

        getFrames.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                try {
                    extractImageFrame(absPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        sendFrames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
                intent.putExtra("image_path", frameListPath);
                startActivity(intent);
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
                Log.e("videoUri", String.valueOf(videoUri.getPath()));
                videoView.setZOrderOnTop(true);//this line solve the problem
                videoView.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void extractImageFrame(String absPath) throws IOException {

        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        String parentPath = "/storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Frames/";
        String testPath = "/storage/emulated/0/Android/data/com.kohdev.viderex/files/Movies/Videos/VID_20210402_133213_2599549758740143761.mp4";
        String path = "content://com.kohdev.viderex/my_movies/Videos/VID_20210402_133213_2599549758740143761.mp4";

        FFmpegMediaMetadataRetriever med = new FFmpegMediaMetadataRetriever();
        med.setDataSource(absPath);
        String time = med.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        int videoDuration = Integer.parseInt(time);
        Log.e("video duration", String.valueOf(videoDuration));
        // The frames could be starting +1 the start. Investigate the main looper. 
        for (int i = 1000000; i < videoDuration * 1000; i += 1000000) {
            Bitmap bmp = med.getFrameAtTime(i, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            Log.d("Frame extracted at ", String.valueOf(i));
            saveBit(bmp);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private File saveBit(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);

        String currentTimestamp = String.valueOf(Instant.now().toEpochMilli());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String frameName = "FRAME_" + currentTimestamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        // Create the storage directory if it does not exist
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.d("APP_TAG", "failed to create directory");
        }

        // Create the storage directory if it does not exist
        Log.e("Storage dir", String.valueOf(storageDir));
        File newStorageDir = new File(storageDir + "/Frames/");
        newStorageDir.mkdir();
        File image = File.createTempFile(
                frameName,  /* prefix */
                ".jpg",         /* suffix */
                newStorageDir     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        String name = image.getName();
        absPath = image.getAbsolutePath();
        Log.e("current image path", absPath);
        Toast.makeText(this, name + " extracted", Toast.LENGTH_SHORT).show();
        frameListPath.add(Uri.fromFile(image));
        FileOutputStream fo = new FileOutputStream(image);
        fo.write(bytes.toByteArray());
        fo.close();
        return image;
    }


}