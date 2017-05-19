package com.example.grzegorz.streammusic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

public class ServerBluetooth extends Thread implements AdapterView.OnItemClickListener {
    private BluetoothSocket socket;
    private Context context;
    private String path;
    private int isPlayingId;
    private Thread threadSendMusic;
    private LinkedList<ListMusicRow> listMusic;
    private ComunicationWithClient comunicationWithClient;
    private ObjectOutputStream oos;
    private ObjectInputStream ooi;
    private FileInputStream inputStream;

    public ServerBluetooth(Context context){
        Log.i("INFO", "Poczatek konstruktora ServerBluetooth");
        socket = null;
        this.context = context;
        this.path = null;
        this.threadSendMusic = null;
        this.listMusic = new LinkedList<>();
        this.comunicationWithClient = null;
        this.oos = null;
        this.ooi = null;
        this.inputStream = null;
        connetClient();
        try {
            ooi = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("INFO", "Koniec konstruktora ServerBluetooth");
    }

    public void connetClient(){
        Log.i("INFO", "Poczatek connectClient");
        BluetoothServerSocket sendMusicServerSocket = null; //wysylanie daynch
        BluetoothServerSocket comunicationWithClientServerSocket = null;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID uuid2 = UUID.fromString("54d1cc90-1169-11e2-892e-0800200c9a66");
            sendMusicServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("usluga witajaca", uuid);
            comunicationWithClientServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("usluga witajaca", uuid2);

        }catch (Exception e){
            Log.e("ERROR", e+"");
        }
        try{
            Log.i("INFO", "Czekam na poloczenie sendMusicServerSocket");
            bluetoothAdapter.cancelDiscovery();
            socket = sendMusicServerSocket.accept();
            if (socket != null){
                Log.i("INFO", "Udalo sie nawiazac polaczenie w sendMusicServerSocket");
                oos = new ObjectOutputStream(this.socket.getOutputStream());
            }else {
                Log.i("INFO", "Nie udalo sie nawiazac polaczenia w sendMusicServerSocket");
            }
            Log.i("INFO", "Czekam na poloczenie comunicationWithClientServerSocket");
            comunicationWithClient = new ComunicationWithClient(comunicationWithClientServerSocket.accept());
            Log.i("INFO", "Udalo się poloczenie comunicationWithClientServerSocket");
            Toast.makeText(context, "Udalo sie nawiazac polaczenie", Toast.LENGTH_LONG).show();

        }catch (Exception e){
            Log.e("ERROR", "Cos poszlo nie tak przy polaczeniu" + e);
        }
    }

    public void showMusic(ListView listView){//ToDo przebudowac aby pokazywalo tylko mp3 albo zrobic konwersje na mp3
        ListMusicAdapter listMusicAdapter;
        ContentResolver cr = context.getContentResolver();


        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);

        if(cur != null)
        {
            if(cur.getCount() > 0)
            {
                while(cur.moveToNext())
                {
                    int idx = cur.getColumnIndex(MediaStore.Audio.Media.DATA);
                    String title = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String path = cur.getString(idx);
                    listMusic.add(new ListMusicRow(title, path));
                }
                listMusicAdapter = new ListMusicAdapter(context, R.layout.row_music_element, listMusic);
                listView.setOnItemClickListener(this);
                listView.setAdapter(listMusicAdapter);
            }
        }

        cur.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ImageView imageView = (ImageView) parent.getChildAt(position).findViewById(R.id.imageVplay);

        if (this.path == listMusic.get(position).getPath()){//path sie rownaja, znaczy to tez ze cos juz gralo/gra
            if (comunicationWithClient.status == ComunicationClientServer.play){//to na co kliknolem gra wiec pause
                comunicationWithClient.status = ComunicationClientServer.pasue;
                Toast.makeText(context, "Pause", Toast.LENGTH_LONG).show();
                imageView.setImageResource(R.drawable.play);
            }else if (comunicationWithClient.status == ComunicationClientServer.pasue) {//to w co kliknolem jest pause, uruchom to
                comunicationWithClient.status = ComunicationClientServer.play;
                Toast.makeText(context, "Play", Toast.LENGTH_LONG).show();
                imageView.setImageResource(R.drawable.pause);
            }
            comunicationWithClient.run();
        }else {//path sa rozne
            if (this.path == null){//czyli jeszcze nic nie gralo bo path pusty
                comunicationWithClient.status = ComunicationClientServer.play;
                imageView.setImageResource(R.drawable.pause);
                this.path = listMusic.get(position).getPath();
                try {
                    inputStream = new FileInputStream(new File(path));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                this.isPlayingId = position;
                Toast.makeText(context, "First PLay", Toast.LENGTH_LONG).show();
                if (threadSendMusic == null){
                    threadSendMusic = new sendMusic();
                    threadSendMusic.start();
                }
                comunicationWithClient.run();
            }
            else {//zmiana piosenki
                //ToDo dopisac obsluge zmiany piosenki
                ImageView lastImageView = (ImageView) parent.getChildAt(this.isPlayingId).findViewById(R.id.imageVplay);
                lastImageView.setImageDrawable(null);
                imageView.setImageResource(R.drawable.pause);
                this.path = listMusic.get(position).getPath();
                this.isPlayingId = position;
                comunicationWithClient.status = ComunicationClientServer.changeSong;
                try {
                    threadSendMusic.join();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                comunicationWithClient.run();
                try {
                    inputStream = new FileInputStream(new File(path));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                threadSendMusic.interrupt();
                comunicationWithClient.status = ComunicationClientServer.play;
                threadSendMusic = new sendMusic();
                threadSendMusic.start();

            }
        }
    }

    private static class ComunicationWithClient extends Thread{
        private static byte[] status;
        private ObjectOutputStream oos;
        public boolean lastPackage;
        public int sizeLastPackage;

        public ComunicationWithClient(BluetoothSocket socket){
            lastPackage = false;
            try {
                this.oos = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void run(){
            try {
                if (lastPackage){
                    Log.i("INFO", "Wysylam informacje do klienta o ostatnim package");
                    oos.write(ComunicationClientServer.lastPackage);
                    oos.flush();
                    oos.writeInt(sizeLastPackage);
                    oos.flush();
                }else {
                    Log.i("INFO" , "Wysylam do klienta: " + status[0] + "" + status[1] + "" + status[2]);
                    oos.write(status);
                    oos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class sendMusic extends Thread{
        public void run(){
            Log.i("INFO", "Watek sendMusic zostal uruchomiony");


            if(socket != null){
                Log.i("INFO", "Zaczynam wysylanie");
                try {

                    int len;
                    byte[] buf = new byte[1024];
                    int licz = 0;
                    while ((len = inputStream.read(buf)) != -1 && !ComunicationClientServer.checkResponse(ComunicationClientServer.changeSong, comunicationWithClient.status)) {
                        licz++;
                        oos.write(buf, 0, len);
                        Log.i("loop", licz+"");
                        if (licz == 500){
                            licz = 0;
                            oos.flush();

                            byte[] odpByte = new byte[ComunicationClientServer.sizeOfResponse];
                            Log.i("INFO", "Chce czytac od clienta");
                            if (ooi.read(odpByte) != -1){
                                Log.i("INFO", "Odpowiedz klienta: " + odpByte[0] + "" + odpByte[1] + "" + odpByte[2]);
                                if (ComunicationClientServer.checkResponse(ComunicationClientServer.nextPack, odpByte)){
                                    Log.i("Info", "Klient chce kolejna paczke");
                                }
                            }else {
                                Log.e("ERROR", "Cos poszlo nie tak nie udalo sie odczytac odp clienta");
                            }

                            while(comunicationWithClient.status == ComunicationClientServer.pasue){
                                sleep(500);
                            }
                        }
                    }
                    if(!ComunicationClientServer.checkResponse(ComunicationClientServer.changeSong, comunicationWithClient.status)){//zmiana piosenki więc nie wysylamy
                        oos.flush();
                        comunicationWithClient.lastPackage = true;
                        comunicationWithClient.sizeLastPackage = licz;
                        comunicationWithClient.run();
                        comunicationWithClient.lastPackage = false;
                    }
                    inputStream.close();

                    Log.i("INFO", "Koniec watku sendMusic");
                }catch (Exception e){
                    Log.e("ERROR", e+"");
                    try {
                        oos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}