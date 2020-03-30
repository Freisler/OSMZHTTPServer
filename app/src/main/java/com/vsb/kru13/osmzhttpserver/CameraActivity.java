package com.vsb.kru13.osmzhttpserver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.hardware.Camera;
import java.util.Timer;
import java.util.TimerTask;


public class CameraActivity extends AppCompatActivity {
    private Camera mCamera;
    static String photoName;
    private static byte[] d;
    Timer timer;
    Button startButton;
    Button stopButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        FrameLayout preview = findViewById(R.id.camera_preview);
        startButton = findViewById(R.id.button_stream_start);
        stopButton = findViewById(R.id.button_stream_stop);
        Button captureButton = findViewById(R.id.button_capture);
        Button backButton = findViewById(R.id.button_back);
        // Create an instance of Camera
        mCamera = getCameraInstance();
        // Create our Preview view and set it as the content of our activity.
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);

        startButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startButton.setVisibility(View.INVISIBLE);
                        stopButton.setVisibility(View.VISIBLE);
                        timer = new Timer();
                        //Set the schedule function
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                mCamera.takePicture( null, null, mPicture);
                                //countFrames++;
                            }
                        }, 0, 2000);   // 1000 Millisecond  = 1 second
                    }
                });

        stopButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer.cancel();
                        startButton.setVisibility(View.VISIBLE);
                        stopButton.setVisibility(View.INVISIBLE);
                    }
                });

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture( null, null, mPicture);
                    }
                });

        backButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent();
                        i.putExtra("cameraFile",photoName);
                        setResult(RESULT_OK,i);
                        finish();
                    }
                });
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public static byte[] getImage() {
        return d;
    }

    private android.hardware.Camera.PictureCallback mPicture = new android.hardware.Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            d = data;

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after x ms
                    mCamera.startPreview();
                    stopButton.setEnabled(true);
                }
            }, 500);
        }
    };

}
