package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class ClientThread extends Thread {
    private Socket s;
    private Handler messageHandler;
    private Handler threadHandler;
    private Semaphore lock;
    private Message message;
    private String messageSuccess = "File URI: ";
    private boolean adbShellRan;
    private static final String CGI = "cgi-bin";
    private static final String MSG_END = "Client closed the connection.";

    ClientThread(Socket s, Handler h, Semaphore lock, Handler t) {
        this.s = s;
        this.messageHandler = h;
        this.lock = lock;
        this.threadHandler = t;
    }

    @Override
    public void run(){
        while(true) {
            try {
                thread();
            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                lock.release();
                threadHandler.sendEmptyMessage(-1);
                if (!adbShellRan) {
                   break;
                }
            }
        }
    }

    private void thread() throws IOException {
        threadHandler.sendEmptyMessage(1);
        OutputStream o = s.getOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OSMZ";
        String tmp = in.readLine();
        adbShellRan = false;
        if (tmp != null) {

            Log.d("THREAD", "-----------------------------------------------------------------------------");
            Log.d("THREAD", tmp);

            String file;

            if (tmp.startsWith("GET")) {
                String[] parts = tmp.split(" ");
                file = parts[1];
                if (file.equals("/"))
                    file = "/index.html";
                if (file.contains(CGI)) {

                    Log.d("CGI_REQUEST", file);
                    int cgiIndex = file.indexOf(CGI)+CGI.length()+1;
                    int endIndex = file.length();
                    String command = file.substring(cgiIndex, endIndex);
                    Log.d("CGI_COMMAND", command);
                    cgiCommand(command, out, sdPath);
                    s.close();
                    return;
                }
            } else {
                file = "/.html";
            }

            if(!adbShellRan) {
                boolean fileExists;
                boolean isHtml;
                String filePath;

                filePath = sdPath + file;
                fileExists = isFileExisting(filePath);

                isHtml = file.endsWith(".html");

                try {
                    if(fileExists)
                        Log.d("SERVER_CLIENT_THREAD", "Opening file " + filePath);

                    if (filePath.contains("/camera/snapshot/image.jpg")) {

                        byte[] data;
                        data = CameraActivity.getImage();
                        if (data != null && data.length > 0) {
                            out.write("HTTP/1.0 200 OK\n");
                            out.write("Content-Type: image/jpg, image/jpeg\n");
                            out.write("Content-Length: " + data.length + "\n");
                            out.write("\n");
                            out.flush();
                            o.write(data, 0, data.length);
                            message = messageHandler.obtainMessage();
                            message.obj = "Image loaded from method onPhotoTaken(); Size: " + data.length + " B";
                            messageHandler.sendMessage(message);
                        }
                        s.close();

                    } else {
                        if (fileExists && isHtml) {
                            out.write("HTTP/1.0 200 OK\n");
                            out.write("Content-Type: text/html\n");
                        }
                        if (fileExists && !isHtml) {
                            out.write("HTTP/1.0 200 OK\n");
                            out.write("Content-Type: image/jpeg, image/png\n");
                        }
                        File f = new File(filePath);
                        FileInputStream fileIS = new FileInputStream(f);
                        out.write("Content-Length: " + f.length() + "\n");
                        long sizeOfFile = f.length();
                        out.write("\n");
                        out.flush();

                        int c;
                        byte[] buffer = new byte[Math.toIntExact(sizeOfFile)];

                        while ((c = fileIS.read(buffer)) != -1) {
                            o.write(buffer, 0, c);
                        }
                        messageSuccess += filePath + "; Size: " + sizeOfFile + " B";
                        message = messageHandler.obtainMessage();
                        message.obj = messageSuccess;
                        messageHandler.sendMessage(message);

                        fileIS.close();
                        s.close();
                    }

                } catch (FileNotFoundException e) {
                    Log.d("SERVER_CLIENT_THREAD", "HTTP/1.0 404 Not Found");
                    out.write("HTTP/1.0 404 Not Found\n\n");
                    out.write("<h1>404 Not Found</h1>");
                    out.flush();
                    message = messageHandler.obtainMessage();
                    message.obj = "File not found.";
                    messageHandler.sendMessage(message);
                    threadHandler.sendEmptyMessage(-1);
                    Log.d("SERVER_CLIENT_THREAD", "Socket Closed");
                    s.close();
                }
            }
        } else {
            message = messageHandler.obtainMessage();
            message.obj = MSG_END;
            messageHandler.sendMessage(message);
            s.close();
            threadHandler.sendEmptyMessage(-1);
            Log.d("SERVER_CLIENT_THREAD", "Socket Closed");
        }
    }

    private void cgiCommand(String command, BufferedWriter o, String sdPath){
        StringBuilder cgiBuilder = new StringBuilder();

        try {
            File cgiDirectory = new File(sdPath + "/CGI");
            File temp = new  File(sdPath + "/CGI/cgi.txt");
            if (! cgiDirectory.exists()){
                if (! cgiDirectory.mkdirs()){
                    Log.d("CGI_ERROR", "CGI directory failed to create.");
                    return;
                }
            }
            ProcessBuilder pb;
            if(command.contains("cat")){
                String[] parts;
                if(command.contains(" ")) {
                    parts = command.split(" ");
                    pb = new ProcessBuilder(parts[0], parts[1]);
                } else if(command.contains("%20")) {
                    parts = command.split("%20");
                    pb = new ProcessBuilder(parts[0], parts[1]);
                } else {
                    pb = new ProcessBuilder(command);
                }
            } else {
                pb = new ProcessBuilder(command);
            }
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                cgiBuilder.append(line).append("\n");
            }
            writer.close();

            if(cgiBuilder.length() > 0) {
                Log.d("CGI", cgiBuilder.toString());
                o.write("HTTP/1.0 200 OK\n");
                o.write("\n");
                o.write(cgiBuilder + "\n");
                o.flush();
            }

            message = messageHandler.obtainMessage();
            message.obj = CGI + "/" + command;
            messageHandler.sendMessage(message);
            //s.close();
            threadHandler.sendEmptyMessage(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isFileExisting(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

}