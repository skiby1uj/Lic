package com.example.grzegorz.streammusic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

//todo sprawdzic czy da sie zrobic dwa polaczenia z jednym urzadzeniem

public class ServerBluetooth extends Thread {
    private static BluetoothServerSocket serverSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    public ServerBluetooth(Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket tmp = null;
//        File f = new File(Environment.getExternalStorageDirectory() + "/Music/biegnij.mp3");
//        long len = f.length();

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
        int odp;
        try{
            Log.e("INFO", "Czekam na poloczenie");
            bluetoothAdapter.cancelDiscovery();
            socket = serverSocket.accept();
            Log.e("`INFO", socket+"");

        }catch (Exception e){
            Log.e("ERROR", "Cos poszlo nie tak" + e);
        }

        if(socket != null){
            Log.e("INFO", "Zaczynam wysylanie");
            try {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ooi = new ObjectInputStream(socket.getInputStream());
                InputStream inputStream = context.getResources().openRawResource(R.raw.biegnij);

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

                        byte[] odpByte = new byte[ComunicationClientServer.sizeOfResponse];
                        Log.e("INFO", "Chce czytac od clienta");
                        if (ooi.read(odpByte) != -1){
                            Log.e("INFO", "odpowiedz klienta : " + odpByte[0] + " " + odpByte[1]);
                            if (ComunicationClientServer.checkResponse(ComunicationClientServer.nextPack, odpByte)){
                                Log.e("Info", "Udalo sie odebrac odp i jest taka sama");
                            }
                        }else {
                            Log.e("ERROR", "Cos poszlo nie tak nie udalo sie odczytac odp clienta");
                        }
                        Log.e("INFO", "Odczytalem od clienta");
                    }
                }
                oos.flush();
                inputStream.close();
                oos.close();

                Log.e("INFO", "Koniec polaczenia");
            }catch (Exception e){
                Log.e("ERROR", e+"");
            }
        }
    }
}

