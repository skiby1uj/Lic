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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by grzegorz on 23.03.17.
 */

public class ServerBluetooth extends Thread implements AdapterView.OnItemClickListener {
    private static BluetoothServerSocket serverSocket;
    private BluetoothServerSocket serverSocket2;
    BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private String path;
    private int isPlayingId;
    private static byte[] comunicationClientServer;//przerzucic to do klasy ComunicationWithClient
    private Thread t;
    LinkedList<ListMusicRow> listMusic = new LinkedList<>();
    ComunicationWithClient comunicationWithClient = null;

    public ServerBluetooth(Context context/*, String path*/){
        this.context = context;
        this.path = null;
        this.comunicationClientServer = null;
        this.t = null;
        connetClient();
    }

    public void connetClient(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try{
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID uuid2 = UUID.fromString("54d1cc90-1169-11e2-892e-0800200c9a66");
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("usluga witajaca", uuid);
            serverSocket2 = bluetoothAdapter.listenUsingRfcommWithServiceRecord("usluga witajaca", uuid2);

            Log.e("tmp", serverSocket+"");
        }catch (Exception e){
            Log.e("ERROR", e+"");
        }
        try{
            Log.e("INFO", "Czekam na poloczenie");
            bluetoothAdapter.cancelDiscovery();
            socket = serverSocket.accept();
            comunicationWithClient = new ComunicationWithClient(serverSocket2.accept());
            Toast.makeText(context, "Udalo sie nawiazac polaczenie", Toast.LENGTH_LONG).show();
            Log.e("`INFO", socket+"");

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

    private class ComunicationWithClient extends Thread{
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
                    oos.write(ComunicationClientServer.lastPackage);
                    oos.flush();
                    oos.writeInt(sizeLastPackage);
                    oos.flush();
                }else {
                    oos.write(comunicationClientServer);
                    oos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ImageView imageView = (ImageView) parent.getChildAt(position).findViewById(R.id.imageVplay);

        if (this.path == listMusic.get(position).getPath()){//path sie rownaja, znaczy to tez ze cos juz gralo/gra
            if (comunicationClientServer == ComunicationClientServer.play){//to na co kliknolem gra wiec pause
                comunicationClientServer = ComunicationClientServer.pasue;
                Toast.makeText(context, "Pause", Toast.LENGTH_LONG).show();
                imageView.setImageResource(R.drawable.play);
            }else if (comunicationClientServer == ComunicationClientServer.pasue) {//to w co kliknolem jest pause, uruchom to
                comunicationClientServer = ComunicationClientServer.play;
                Toast.makeText(context, "Play", Toast.LENGTH_LONG).show();
                imageView.setImageResource(R.drawable.pause);
            }
        }else {//path sa rozne
            if (this.path == null){//czyli jeszcze nic nie gralo bo path pusty
                comunicationClientServer = ComunicationClientServer.play;
                imageView.setImageResource(R.drawable.pause);
                this.path = listMusic.get(position).getPath();
                this.isPlayingId = position;
                Toast.makeText(context, "First PLay", Toast.LENGTH_LONG).show();
                if (t == null){
                    t = this;
                    t.start();
                }
            }
            else {//zmiana piosenki
                //ToDo dopisac obsluge zmiany piosenki
            }
        }
        comunicationWithClient.run();
    }

    public void run(){
        Log.e("INFO", "Uruchamiam serwer");


        if(socket != null){
            Log.e("INFO", "Zaczynam wysylanie");
            //while (true){
            ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream ooi = new ObjectInputStream(socket.getInputStream());
                    FileInputStream inputStream = new FileInputStream(new File(path));

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

                            while(comunicationClientServer == ComunicationClientServer.pasue){}
                        }
                    }
                    oos.flush();
                    inputStream.close();
                    comunicationWithClient.lastPackage = true;
                    comunicationWithClient.sizeLastPackage = licz;
                    comunicationWithClient.run();
                    comunicationWithClient.lastPackage = false;
//                    oos.close();//TODO NIE CHCE TU ZRYWAC POLACZENIA, CHCE MIEC JE STAŁE?

                    Log.e("INFO", "Koniec polaczenia");
                }catch (Exception e){
                    Log.e("ERROR", e+"");
                    try {
                        oos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            //}
        }
    }
}