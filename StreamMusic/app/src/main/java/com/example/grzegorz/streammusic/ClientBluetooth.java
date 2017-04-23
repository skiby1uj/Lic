package com.example.grzegorz.streammusic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

public class ClientBluetooth{
    private final BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private LinkedList<MediaPlayer> arrMediaPlayer = new LinkedList<>();

    public ClientBluetooth(String mac, Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
        BluetoothSocket tmp = null;
        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            Log.e("device", bluetoothDevice+"");
            tmp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        }catch (Exception e){
            Log.e("ERROR 1", e+"");
        }
        Log.e("tmp", tmp+"");
        this.socket = tmp;
        bluetoothConnect();
        try {
            download d = new download(socket);
            d.start();
        } catch (IOException e) {
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

        private class download extends Thread{
            int licz = 0;
            int n = 0;
            int mod = 0;
            byte[] buf = null;
            File path = null;
            FileOutputStream fos = null;
            public ObjectInputStream ois;
            public ObjectOutputStream oos;

            public download(BluetoothSocket sock) throws IOException {
                path = new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                fos = new FileOutputStream(path);
                if (sock.isConnected()){
                    ois = new ObjectInputStream(sock.getInputStream());
                    oos = new ObjectOutputStream(sock.getOutputStream());
                }
                buf = new byte[1024];
                Log.e("INFO", "Konstruktor download");
            }

            public void run() {
                try {
                    while ((n = ois.read(buf)) != -1){
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
                                    new playMediaPlayer().start();
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
                } catch (IOException e) {
                    e.printStackTrace();
                    arrMediaPlayer.add((MediaPlayer)null);
                }
            }
        }

        private class playMediaPlayer extends Thread{
            int isPlaying;

            public playMediaPlayer(){
                Log.e("INFO", "Konstruktor play");
                isPlaying = -1;
            }
            public void run() {
                int tmp;
                while (true){
                    tmp = ktoryGra(isPlaying);
                    if (tmp == -1){//nic nie gra
                        if (arrMediaPlayer.size() > isPlaying){//czy jest cos jeszcze na liscie? jak nie to znaczy ze czyta bo na koncu musi byc null
                            if (arrMediaPlayer.get(isPlaying+1) == null){//jesli nie ma to koniec
                                break;
                            }else {//jesli jest co puscic to play
                                isPlaying++;
                                arrMediaPlayer.get(isPlaying).start();
                            }
                        }
                    }
                    else {
                        isPlaying = tmp;
                    }
                }
            }

            private int ktoryGra(int start){
                if (start < 0){
                    return -1;
                }
                for (int i = start; i < arrMediaPlayer.size(); i++){
                    if (arrMediaPlayer.get(i).isPlaying()){
                        return i;
                    }
                }
                return -1;
            }
        }
    }



