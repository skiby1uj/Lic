package com.example.grzegorz.streammusic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Path;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

//TODO odbieranie tez musi byc w oddzielnym watku zeby nie blokowac ekranu

public class ClientBluetooth extends Thread {
    private final BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private Queue <Queue<byte[]> > queueBuff;

    public ClientBluetooth(String mac, Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
        BluetoothSocket tmp = null;
        queueBuff = new LinkedList<>();
        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            Log.e("device", bluetoothDevice+"");
            tmp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        }catch (Exception e){
            Log.e("ERROR 1", e+"");
        }
        Log.e("tmp", tmp+"");
        this.socket = tmp;
    }

    public void run(){
        bluetoothConnect();
        try{
            if (socket.isConnected()){
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
//                playMp3(ois);
                downsloadStreamAndPlay(ois, oos);
            }
        }catch (IOException e){
            Log.e("Error InputStream", "Nie udalo sie zwrucic inputStream");
            e.printStackTrace();
        }
    }

    private void bluetoothConnect(){
            Log.e("INFO", "Proba polaczenia");
            bluetoothAdapter.cancelDiscovery();
            try{
                socket.connect();
                Log.e("INFO", "Serwer zaakceptowal");
            }catch (IOException e){
                Log.e("Error Connect", "Nie udalo sie polaczyc z serverem");
                e.printStackTrace();
            }
    }

    private void downsloadStreamAndPlay(InputStream inputStream, ObjectOutputStream oos) throws IOException {
        int licz = 0;
        int n = 0;
        int mod = 0;
        byte[] buf = new byte[1024];
        LinkedList<MediaPlayer> arrMediaPlayer = new LinkedList<>();

        File path=new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
        FileOutputStream fos = new FileOutputStream(path);

        while ((n = inputStream.read(buf)) != -1){
            licz++;
            fos.write(buf, 0, n);
            buf = new byte[1024];

            if (licz == 500){
                licz = 0;
                fos.close();
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(path.toString());
                mp.prepare();
                arrMediaPlayer.add(mp);
                if (mod >= 1){
                    arrMediaPlayer.get(mod-1).setNextMediaPlayer(arrMediaPlayer.get(mod));
                    if (mod == 2){
                        arrMediaPlayer.getFirst().start();
                    }
                }
                Log.e("INFO", "Pisze do servera");
                oos.write(ComunicationClientServer.nextPack);
                oos.flush();
                Log.e("INFO", "Napisalem do servera");

                mod++;
                path = new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                fos = new FileOutputStream(path);
            }
        }
    }
}
