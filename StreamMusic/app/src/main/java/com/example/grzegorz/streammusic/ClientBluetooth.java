package com.example.grzegorz.streammusic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

//TODO Sprobowac dac play() w nowym watku, moze nie bedzie zauwazalna przerwa
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
        try{
            Log.e("INFO", "Proba polaczenia");
            bluetoothAdapter.cancelDiscovery();
            socket.connect();
            Log.e("INFO", "Serwer zaakceptowal");

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            playMp3(ois);
        }catch (Exception e){
            Log.e("ERROR 2", e+"");
            try{
                socket.close();
            }catch (Exception ex){
                Log.e("ERROR 3", "Nie udalo sie zamknac socket: "+socket + " "+ex+"");
            }
        }
    }

    private void playMp3(InputStream inputStream)
    {
        int mod = 0;
        int len = 0;
        int licz = 0;
        byte[] buf = new byte[1024];
        Queue<byte[]> queueByte = new LinkedList<>();
        LinkedList<MediaPlayer> list = new LinkedList<>();
        MediaPlayer mediaPlayer;

        try {
            while ((len = inputStream.read(buf)) != -1){

                queueByte.add(buf);
                licz++;
                buf = new byte[1024];

                if(licz == 500){
                    licz = 0;
                    queueBuff.add(queueByte);
                    queueByte = new LinkedList<>();
                }

                while (!queueBuff.isEmpty()) {
                    File path=new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                    FileOutputStream fos = new FileOutputStream(path);
                    Queue<byte[]> tmp = queueBuff.poll();
                    while (!tmp.isEmpty()) {
                        byte[] tmpByte = tmp.poll();
                        fos.write(tmpByte, 0, tmpByte.length);
                    }
                    fos.close();

                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(context.getCacheDir()+"/musicfile"+mod+".3gp");
                    mediaPlayer.prepare();
                    list.add(mediaPlayer);

                    if (mod > 0){
                        list.get(mod-1).setNextMediaPlayer(list.get(mod));
                        if (mod == 1){
                            list.getFirst().start();
                        }
                    }
                    mod++;
                }
                Log.e("loop", licz + " " + len);
            }
            inputStream.close();
        }catch (Exception e){
            Log.e("ERROR", e+"");
            if(!queueByte.isEmpty()){
                queueBuff.add(queueByte);
            }
        }
    }
}
