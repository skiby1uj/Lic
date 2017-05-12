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
    private final BluetoothSocket socket2;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    ComunicationWithServer comunicationWithServer = null;
    private LinkedList<MediaPlayer> arrMediaPlayer = new LinkedList<>();
    private playMediaPlayer playMediaPlayer;
    private byte[] comunicationClientServer;
    private download d;

    public ClientBluetooth(String mac, Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
        bluetoothDevice.fetchUuidsWithSdp();
        BluetoothSocket tmp = null;
        BluetoothSocket tmp2 = null;
        comunicationClientServer = null;
        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID uuid2 = UUID.fromString("54d1cc90-1169-11e2-892e-0800200c9a66");
            Log.e("device", bluetoothDevice+"");
            tmp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            tmp2 = bluetoothDevice.createRfcommSocketToServiceRecord(uuid2);
        }catch (Exception e){
            Log.e("ERROR 1", e+"");
        }
        Log.e("tmp", tmp+"");
        this.socket = tmp;
        this.socket2 = tmp2;
        while(bluetoothConnect(socket) == false){}
        while(bluetoothConnect(socket2) == false){}
        comunicationWithServer = new ComunicationWithServer(socket2);

        try {
            d = new download(socket);
            d.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean bluetoothConnect(BluetoothSocket socket){
            Log.e("INFO", "Proba polaczenia");
            bluetoothAdapter.cancelDiscovery();
            try{
                socket.connect();
                Log.e("INFO", "Serwer zaakceptowal");
                return true;
            }catch (IOException e){
                Log.e("Error Connect", "Nie udalo sie polaczyc z serverem");
                e.printStackTrace();
                return false;
            }
    }

    private class ComunicationWithServer extends Thread{
        private BluetoothSocket socket;
        private ObjectInputStream ois;

        public ComunicationWithServer(BluetoothSocket socket){
            this.socket = socket;
            try {
                this.ois = new ObjectInputStream(socket.getInputStream());
                this.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            if (this.socket.isConnected()){
                while (true){
                    try {
                        byte[] odpByte = new byte[ComunicationClientServer.sizeOfResponse];
                        if (ois.read(odpByte) != -1){
                            if(ComunicationClientServer.checkResponse(ComunicationClientServer.lastPackage, odpByte)){//ostatnia paczka
                                d.lastPackage = true;
                                d.sizeLastPackage = ois.readInt();
                            }else {
                                comunicationClientServer = odpByte;
                                Log.e("asfd", "odczytalem od servera");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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
            public boolean lastPackage;
            public int sizeLastPackage;

            public download(BluetoothSocket sock) throws IOException {
                path = new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                fos = new FileOutputStream(path);
                if (sock.isConnected()){
                    ois = new ObjectInputStream(sock.getInputStream());
                    oos = new ObjectOutputStream(sock.getOutputStream());
                }
                buf = new byte[1024];
                lastPackage = false;
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
                                    playMediaPlayer = new playMediaPlayer();
                                    playMediaPlayer.start();
//                                    new playMediaPlayer().start();
                                }
                            }
                            Log.e("INFO", "Pisze do servera");
                            oos.write(ComunicationClientServer.nextPack);
                            oos.flush();
                            Log.e("INFO", "Napisalem do servera");

                            mod++;
                            path = new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                            fos = new FileOutputStream(path);
                        }else if(lastPackage && sizeLastPackage == licz){//ostatnia paczka todo do optymalizacji ten syf!!!
                            Log.e("PackageLast", "Last package");
                            licz = 0;
                            fos.close();
                            MediaPlayer mp = new MediaPlayer();
                            mp.setDataSource(path.toString());
                            mp.prepare();
                            arrMediaPlayer.add(mp);
                            if (mod >= 1){
                                arrMediaPlayer.get(mod-1).setNextMediaPlayer(arrMediaPlayer.get(mod));
                                if (mod < 2){
                                    playMediaPlayer = new playMediaPlayer();
                                    playMediaPlayer.start();
                                }
                            }
                            arrMediaPlayer.add((MediaPlayer)null);//oznaczenie konca piosenki
                            break;
                        }
                    }
                    Log.e("Koniec petli", "Koniec petli");
                } catch (IOException e) {
                    e.printStackTrace();
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
                    if (ComunicationClientServer.checkResponse(ComunicationClientServer.play, comunicationClientServer)){
                        tmp = ktoryGra(isPlaying);
                        if (tmp == -1){//nic nie gra
                            if (arrMediaPlayer.size() > isPlaying+1){//czy jest cos jeszcze na liscie? jak nie to znaczy ze czyta bo na koncu musi byc null
                                if (arrMediaPlayer.get(isPlaying+1) == null){//jesli nie ma to koniec
                                    break;
                                }else {//jesli jest co puscic to play
                                    isPlaying++;
                                    if (!arrMediaPlayer.get(isPlaying).isPlaying())
                                        arrMediaPlayer.get(isPlaying).start();
                                }
                            }
                        }
                        else {
                            isPlaying = tmp;
                        }
                    }else if (ComunicationClientServer.checkResponse(ComunicationClientServer.pasue, comunicationClientServer) && arrMediaPlayer.get(isPlaying).isPlaying()){
                        arrMediaPlayer.get(isPlaying).pause();
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



