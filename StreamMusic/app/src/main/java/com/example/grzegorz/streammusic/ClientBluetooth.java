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

public class ClientBluetooth extends Thread{
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private ComunicationWithServer comunicationWithServer = null;
    private LinkedList<MediaPlayer> arrMediaPlayer;
    private PlayMediaPlayer playMediaPlayer;
    private static Download download;

    public ClientBluetooth(String mac, Context context){
        Log.i("INFO", "Poczatek konstruktora ClientBLuetooth");
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
        this.comunicationWithServer = null;
        arrMediaPlayer = new LinkedList<>();
        this.playMediaPlayer = null;
        this.download = null;
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
        bluetoothDevice.fetchUuidsWithSdp();
        BluetoothSocket downloadSocket = null;
        BluetoothSocket comunicationWithServerSocket = null;
        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID uuid2 = UUID.fromString("54d1cc90-1169-11e2-892e-0800200c9a66");

            downloadSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            comunicationWithServerSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid2);
        } catch (IOException e) {
            Log.e("ERROR", "Nie udalo sie utworzyc bluetooth socket");
            e.printStackTrace();
        }

        while(bluetoothConnect(downloadSocket) == false){
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while(bluetoothConnect(comunicationWithServerSocket) == false){
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            comunicationWithServer = new ComunicationWithServer(comunicationWithServerSocket);
        } catch (IOException e) {
            Log.e("ERROR", "Nie udalo sie zwrocic inputStream z konstruktora ComunicationWithServer");
            e.printStackTrace();
        }
        comunicationWithServer.start();

        try {
            download = new Download(downloadSocket);
            download.start();
        } catch (IOException e) {
            Log.e("ERROR", "Konstruktor klasy Download rzucil blad");
            e.printStackTrace();
        }
        Log.i("INFO", "Koniec konstruktora ClientBLuetooth");
    }

    private boolean bluetoothConnect(BluetoothSocket socket){
            Log.i("INFO", "Proba polaczenia z serwerem");
            bluetoothAdapter.cancelDiscovery();
            try{
                socket.connect();
                Log.i("INFO", "Udalo sie polaczyc z serwerem");
                return true;
            }catch (IOException e){
                Log.e("ERROR", "Nie udalo sie polaczyc z serverem");
                e.printStackTrace();
                return false;
            }
    }
    private static class ComunicationWithServer extends Thread{
        private static BluetoothSocket socket;
        private ObjectInputStream objectInputStream;
        private byte[] odpByte;
        private byte[] status;

        public ComunicationWithServer(BluetoothSocket socket) throws IOException {
            Log.i("INFO", "Poczatek konstruktora ComunicationWithServer");
            this.socket = socket;
            this.odpByte = null;
            this.status = null;
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            Log.i("INFO", "Koniec konstruktora ComunicationWithServer");
        }

        public void run(){
            Log.i("INFO", "Start watku ComunicationWithServer");
            if (this.socket.isConnected()){
                while (true){
                    try {
                        odpByte = new byte[ComunicationClientServer.sizeOfResponse];
                        if (objectInputStream.read(odpByte) != -1){
                            if(ComunicationClientServer.checkResponse(ComunicationClientServer.lastPackage, odpByte)){//ostatnia paczka
                                Log.i("INFO", "Serwer przeslal informacje ze to ostatnia paczka");
                                download.lastPackage = true;
                                download.sizeOfPackage = objectInputStream.readInt();
                            }else {
                                status = odpByte;
                                Log.i("INFO", "Odczytalem od serwera wiadomosc: " + status[0] + "" + status[1] + "" + status[2]);
                            }
                        }
                    } catch (IOException e) {
                        Log.e("ERROR", "read w ComunicationWithServer rzucilo blad");
                        e.printStackTrace();
                    }
                }
            }else {
                Log.e("ERROR", "socket jest nie polaczony w ComunicationWithServer");
            }
            Log.i("INFO", "Koniec watku ComunicationWithServer");
        }
    }

        private class Download extends Thread{
            private int licz;
            private int n;
            private int mod;
            private byte[] buf;
            private File path;
            private FileOutputStream fileOutputStream;
            private ObjectInputStream objectInputStream;
            private ObjectOutputStream objectOutputStream;
            private boolean lastPackage;
            private int sizeOfPackage;

            public Download(BluetoothSocket sock) throws IOException {
                Log.i("INFO", "Początek konstruktora download");
                this.licz = 0;
                this.n = 0;
                this.mod = 0;
                this.buf = new byte[1024];
                this.path = new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                this.fileOutputStream = new FileOutputStream(path);
                this.sizeOfPackage = ComunicationClientServer.sizeOfPackage;
                if (sock.isConnected()){
                    this.objectInputStream = new ObjectInputStream(sock.getInputStream());
                    this.objectOutputStream = new ObjectOutputStream(sock.getOutputStream());
                }
                this.lastPackage = false;
//                this.sizeLastPackage = -1;
                Log.i("INFO", "Koniec konstruktora download");
            }

            private void clearAndContinue(){
                Log.i("INFO", "Czyscimy zmienne w download");
                try {
                    this.fileOutputStream.close();
                    this.fileOutputStream = new FileOutputStream(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.licz = 0;
                this.mod = 0;
                this.buf = new byte[1024];
                arrMediaPlayer.clear();
            }

            private MediaPlayer createMediaPayerPackage() throws IOException {
                this.fileOutputStream.close();
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(path.toString());
                mp.prepare();
                Log.i("INFO", "Stworzylem " + mod + " paczke");
                return mp;
            }

            public void run() {
                Log.i("INFO", "Start watka Download");
                try {
                    while ((n = objectInputStream.read(buf)) != -1){
                        licz++;
                        this.fileOutputStream.write(buf, 0, n);
                        buf = new byte[1024];

                        if (licz == sizeOfPackage){
                            try{
                                arrMediaPlayer.add(this.createMediaPayerPackage());
                            }catch (IOException e){
                                Log.e("ERROR", "Nie udalo sie utworzyc mediaPlayer w createMediaPayerPackage");
                                e.printStackTrace();
                            }
                            if (mod >= 1){
                                arrMediaPlayer.get(mod-1).setNextMediaPlayer(arrMediaPlayer.get(mod));
                                Log.i("INFO", "Przypisano następnik: " + mod + " do piosenki: " + (mod - 1));
                                if (mod == 2){
                                    comunicationWithServer.status = ComunicationClientServer.play;
                                    playMediaPlayer = new PlayMediaPlayer();
                                    playMediaPlayer.start();
                                    Log.i("INFO", "Start watku playMediaPlayer w download");
                                }
                            }
                            Log.i("INFO", "Pisze do servera aby przeslal kolejna paczke");
                            objectOutputStream.write(ComunicationClientServer.nextPack);
                            objectOutputStream.flush();
                            Log.i("INFO", "Napisałem do servera aby przeslal kolejna paczke");
                            if (lastPackage){
                                Log.i("INFO", "Dodano ostatnia paczke do piosenki");
                                arrMediaPlayer.add((MediaPlayer)null);
                                sizeOfPackage = ComunicationClientServer.sizeOfPackage;
                                lastPackage = false;
                            }
                            licz = 0;
                            mod++;
                            path = new File(context.getCacheDir()+"/musicfile"+mod+".3gp");
                            fileOutputStream = new FileOutputStream(path);

                            if (ComunicationClientServer.checkResponse(ComunicationClientServer.changeSong, comunicationWithServer.status)){//dziala to tu bo w momecie odebrania niekompletnej paczki troche zostawalo w bufforze i nie zgadzaly sie wielkosci
                                //todo trzeba by to jakos poprawic zeby nie czekac do odebrania calej paczki, da się czyscic jakos objectInputStream?
                                Log.e("INFO", "Zmiana odbieranej piosenki");
                                try {
                                    playMediaPlayer.join();
                                } catch (InterruptedException e) {
                                    Log.e("ERROR", "Join rzucil blad w download");
                                    e.printStackTrace();
                                }
                                clearAndContinue();
                                comunicationWithServer.status = ComunicationClientServer.play;
                            }
                        }

                    }
                    Log.e("Koniec petli", "Koniec petli");
                } catch (IOException e) {
                    Log.e("ERROR", "Wystapil blad w watku Download");
                    e.printStackTrace();
                }
            }
        }

        private class PlayMediaPlayer extends Thread{
            int isPlaying;

            public PlayMediaPlayer(){
                Log.i("INFO", "Konstruktor play");
                this.isPlaying = -1;
            }

            public void run() {
                Log.i("INFO", "Start watka PlayMediaPlayer");
                int tmp;
                while (true){
                    if (ComunicationClientServer.checkResponse(ComunicationClientServer.play, comunicationWithServer.status)){
                        tmp = ktoryGra(isPlaying);
                        if (tmp == -1){//nic nie gra
                            if (arrMediaPlayer.size() > isPlaying+1){//czy jest cos jeszcze na liscie? jak nie to znaczy ze czyta bo na koncu musi byc null
                                if (arrMediaPlayer.get(isPlaying+1) == null){//jesli nie ma to koniec
                                    Log.i("INFO", "Koniec watku playMediaPlayer");
                                    return;
                                }else {//jesli jest co puscic to play
                                    isPlaying++;
                                    if (!arrMediaPlayer.get(isPlaying).isPlaying())
                                        arrMediaPlayer.get(isPlaying).start();
                                }
                            }else {
                                if (ComunicationClientServer.checkResponse(ComunicationClientServer.changeSong, comunicationWithServer.status)){
                                    return;
                                }
                                Log.i("INFO", "Cos jeszcze powinno być na liscie wiec pewnie jest pobierane");
                                try {
                                    sleep(500);
                                } catch (InterruptedException e) {
                                    Log.e("ERROR", "Sleep rzucil blad w PlayMediaPlayer");
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            isPlaying = tmp;
                        }
                    }else if (ComunicationClientServer.checkResponse(ComunicationClientServer.pasue, comunicationWithServer.status) && arrMediaPlayer.get(isPlaying).isPlaying()){
                        arrMediaPlayer.get(isPlaying).pause();
                        Log.i("INFO", "Muzyka zostala pause ze względu na komunikat pause od servera");
                        //todo dac tu sleep?
                    }
                    else if (ComunicationClientServer.checkResponse(ComunicationClientServer.changeSong, comunicationWithServer.status)){
                        if (arrMediaPlayer.get(isPlaying).isPlaying()){
                            arrMediaPlayer.get(isPlaying).stop();
                            Log.i("INFO", "Muzyka zostala zatrzymana w ze względu na zmianę piosenki");
                            Log.i("INFO", "Koniec watku playMediaPlayer");
                        }
                        return;
                    }
                }
            }

            private int ktoryGra(int start){
                if (start < 0){
                    return -1;
                }
                for (int i = start; i < arrMediaPlayer.size()-1; i++){
                    if (arrMediaPlayer.get(i).isPlaying()){
                        return i;
                    }
                }
                return -1;
            }
        }
    }