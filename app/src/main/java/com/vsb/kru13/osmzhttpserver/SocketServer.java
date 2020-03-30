package com.vsb.kru13.osmzhttpserver;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class SocketServer extends Thread {

    private ServerSocket serverSocket;
    private final static int port = 12345;
    private boolean bRunning;
    private Handler messageHandler;
    private Handler threadHandler;

    public static final int MAX_THREADS = 2;
    private Semaphore lock = new Semaphore(MAX_THREADS);

    public SocketServer(Handler messageHandler, Handler threadHandler) {
        this.messageHandler = messageHandler;
        this.threadHandler = threadHandler;
    }



    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER_SOCKET_SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    public void run() {
        try {
            Log.d("SERVER_SOCKET_SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER_SOCKET_SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                try {
                    lock.acquire();
                    ClientThread ct = new ClientThread(s, messageHandler, lock, threadHandler);
                    ct.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER_SOCKET_SERVER", "Normal exit");
            else {
                Log.d("SERVER_SOCKET_SERVER", "Error");
                e.printStackTrace();
            }
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }


}


