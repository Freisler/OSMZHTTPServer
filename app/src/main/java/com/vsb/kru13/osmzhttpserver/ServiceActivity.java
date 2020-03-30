package com.vsb.kru13.osmzhttpserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class ServiceActivity extends Service {
    private SocketServer s;
    public Intent intent;
    private final IBinder mIBinder = new LocalBinder();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SERVICE", "created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }
    class LocalBinder extends Binder {
        ServiceActivity getService() {
            return ServiceActivity.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        Log.d("SERVICE", "Service started");
        return super.onStartCommand(intent, flags, startId);
    }
    public void setSettings(Handler messageHandler, Handler threadHandler){
        s = new SocketServer(messageHandler,threadHandler);
        s.start();

    }

    @Override
    public void onDestroy() {
        s.close();
        try {
            s.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("SERVICE", "Service stopped"); }
}
