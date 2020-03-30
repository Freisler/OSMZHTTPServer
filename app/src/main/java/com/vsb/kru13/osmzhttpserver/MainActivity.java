package com.vsb.kru13.osmzhttpserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ALL = 1;
    public Integer threadsCount;

    TextView msgTextView;
    TextView threadsTextView;
    Button btn1;
    Button btn2;
    Button btnCam;
    String cameraFile;
    private Intent service;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn1 = findViewById(R.id.button1);
        btn2 = findViewById(R.id.button2);
        btnCam = findViewById(R.id.buttonCamera);
        msgTextView = findViewById(R.id.message);
        threadsTextView = findViewById(R.id.threads);

        threadsCount = 0;
        String text = "Active threads: <font color='red'>" + threadsCount + "</font>/" + SocketServer.MAX_THREADS;
        threadsTextView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);

        msgTextView.setMovementMethod(new ScrollingMovementMethod());
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btnCam.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button1) {
            String[] PERMISSIONS = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            };
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, ALL);
            } else {
                cameraFile = "";
                service = new Intent(this,ServiceActivity.class);
                startService(service);
                bindService(service,mConnection,Context.BIND_AUTO_CREATE);

                btn1.setText(R.string.serverRunning);
                btn1.setEnabled(false);
                btnCam.setVisibility(View.VISIBLE);
                msgTextView.setText("");
                msgTextView.setVisibility(View.VISIBLE);
                threadsTextView.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
            }
        }
        if (v.getId() == R.id.button2) {
            if(mBound) {
                unbindService(mConnection);
                mBound = false;
            }
            stopService(service);

            btn1.setText(R.string.startServer);
            btn1.setEnabled(true);
            btnCam.setVisibility(View.INVISIBLE);
            msgTextView.setVisibility(View.INVISIBLE);
            threadsTextView.setVisibility(View.INVISIBLE);
            btn2.setVisibility(View.INVISIBLE);
        }
        if (v.getId() == R.id.buttonCamera) {
            Intent i = new Intent(MainActivity.this,CameraActivity.class);
            i.putExtra("threadsCount",threadsCount);
            startActivityForResult(i,1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                service = new Intent(this, ServiceActivity.class);
                startService(service);
                bindService(service, mConnection, Context.BIND_AUTO_CREATE);
                btn1.setText(R.string.serverRunning);
                btn1.setEnabled(false);
                btnCam.setVisibility(View.VISIBLE);
                msgTextView.setVisibility(View.VISIBLE);
                threadsTextView.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
            ServiceActivity serviceActivity = ((ServiceActivity.LocalBinder)iBinder).getService();
            serviceActivity.setSettings(messageHandler,threadHandler);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private Handler messageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
                String status = (String)inputMessage.obj;
                Log.d("SERVER_HANDLER",status + "\n");
                msgTextView.append(status + "\n");
        }
    };

    private Handler threadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            threadsCount += inputMessage.what;
            if(threadsCount < 0)
                threadsCount = 0;
            String text = "Active threads: <font color='red'>" + threadsCount + "</font>/" + SocketServer.MAX_THREADS;
            threadsTextView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);

        }
    };

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                cameraFile = data.getStringExtra("cameraFile");
                if(cameraFile == null) {
                    msgTextView.append("No photo was taken.\n");
                } else {
                    msgTextView.append("Photo " + cameraFile + " was taken.\n");
                }
            }
        }
    }




}
