package com.example.grzegorz.streammusic;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.rtp.AudioStream;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

public class ServerBluetooth extends Thread {
    private static BluetoothServerSocket serverSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private OutputStream outputStream;

    public ServerBluetooth(Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket tmp = null;
        File f = new File(Environment.getExternalStorageDirectory() + "/Music/biegnij.mp3");
        long len = f.length();

        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("usluga witajaca", uuid);
            Log.e("tmp", tmp+"");
        }catch (Exception e){
            Log.e("ERROR", e+"");
        }
        serverSocket = tmp;
    }

    public void run(){
        Log.e("INFO", "Uruchamiam serwer");
        BluetoothSocket socket = null;
        if (true){
            try{
                Log.e("INFO", "Czekam na poloczenie");

                bluetoothAdapter.cancelDiscovery();
                socket = serverSocket.accept();
                Log.e("`INFO", socket+"");

            }catch (Exception e){
                Log.e("ERROR", "Cos poszlo nie tak" + e);
//                break;
            }

            if(socket != null){
                try {
                    Log.e("INFO", "KOniec polaczenia");


//                    File file = new File(Environment.getExternalStorageDirectory(), uri);

//                    MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.biegnij);


                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    InputStream inputStream = context.getResources().openRawResource(R.raw.biegnij);

//                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    playMp3(inputStream);
//                    sleep(5000);
                    int len;
                    byte[] buf = new byte[1024];
                    int licz = 0;
                    while ((len = inputStream.read(buf)) != -1) {
                        licz++;
                        oos.write(buf, 0, len);
                        Log.e("loop", licz+"");
                        if (licz == 500){
                            licz = 0;
                            oos.flush();
                        }
                    }
                    oos.flush();
                    inputStream.close();
                    oos.close();

//                    Log.e("to URI", file.toURI()+"");
//                    oos.writeObject();
//                    oos.flush();



//                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//                    out.println("Witaj kliencie");
//                    serverSocket.close();
                    Log.e("server Socket", serverSocket+"");
//                    socket = null;
                }catch (Exception e){
                    Log.e("ERROR", e+"");
//                    break;
                }
            }
        }
    }


    private void playMp3(InputStream inputStream)
    {
        try
        {

            File path=new File(context.getCacheDir()+"/musicfile.3gp");

            FileOutputStream fos = new FileOutputStream(path);
            byte[] buf = new byte[4096];
            int len = 0;
            while ((len = inputStream.read(buf)) > 0){
                fos.write(buf, 0, len);

            }
            fos.close();

            MediaPlayer mediaPlayer = new MediaPlayer();

            FileInputStream fis = new FileInputStream(path);
            mediaPlayer.setDataSource(context.getCacheDir()+"/musicfile.3gp");

            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        catch (IOException ex)
        {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }
}

