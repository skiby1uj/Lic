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
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.UUID;

public class ServerBluetooth extends Thread implements AdapterView.OnItemClickListener {
    private BluetoothSocket socket;
    private Context context;
    private String path;
    private int isPlayingId;
    private Thread threadSendMusic;
    private ComunicationWithClient comunicationWithClient;
    private ObjectOutputStream oos;
    private ObjectInputStream ooi;
    private FileInputStream inputStream;
    private ListMusicAdapter listMusicAdapter;


    public ServerBluetooth(Context context){
        Log.i("INFO", "Poczatek konstruktora ServerBluetooth");
        socket = null;
        this.context = context;
        this.path = null;
        this.threadSendMusic = null;
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
                Log.i("INFO", "Udalo sie nawiazac polaczenie w sendMusicServerSocket " + socket.toString());
                oos = new ObjectOutputStream(this.socket.getOutputStream());
            }else {
                Log.i("INFO", "Nie udalo sie nawiazac polaczenia w sendMusicServerSocket");
            }
            Log.i("INFO", "Czekam na poloczenie comunicationWithClientServerSocket");
            BluetoothSocket bluetoothSocket = comunicationWithClientServerSocket.accept();
            if (bluetoothSocket != null){
                comunicationWithClient = new ComunicationWithClient(bluetoothSocket);
                Log.i("INFO", "Udalo sie nawiazac polaczenie w comunicationWithClientServerSocket " + bluetoothSocket.toString());
            }else {
                Log.i("INFO", "Nie udalo sie nawiazac polaczenia w comunicationWithClientServerSocket");
            }
            Log.i("INFO", "Udalo się poloczenie comunicationWithClientServerSocket");
            Toast.makeText(context, "Udalo sie nawiazac polaczenie", Toast.LENGTH_LONG).show();

        }catch (Exception e){
            Log.e("ERROR", "Cos poszlo nie tak przy polaczeniu" + e);
        }
    }

    public void showMusic(ListView listView){
        ContentResolver cr = context.getContentResolver();
        LinkedList<ListMusicRow> listMusic = new LinkedList<>();

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
                    String name = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    int len = name.length();
                    if (name.charAt(len-1) == '3' && name.charAt(len-2) == 'p' && name.charAt(len-3) == 'm'){//tylko mp3
                        String path = cur.getString(idx);
                        listMusic.add(new ListMusicRow(title, path));
                    }
                }
                listMusicAdapter = new ListMusicAdapter(context, R.layout.row_music_element, listMusic);
                listView.setAdapter(listMusicAdapter);
                listView.setOnItemClickListener(this);
            }
        }
        cur.close();
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private int getSizeTag(FileInputStream inputStream){
        byte[] buf = new byte[3];
        String hex = "";
        try {
            inputStream.read(buf);
            if ( buf[0] == 73 && buf[1] == 68 && buf[2] == 51 )//ID3
            {
                inputStream.skip(3);
                buf = new byte[4];
                inputStream.read(buf);
            }
            else
                return 0;
            for ( int i = 0; i < 4; i++)
                hex += Integer.toHexString(buf[i]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int out = 0, mask = 0x7F000000;
        int doc = Integer.parseInt(hex.trim(), 16 );
        while (mask > 0 ) {
            out >>= 1;
            out |= doc & mask;
            mask >>= 8;
        }
        return out;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (this.path == listMusicAdapter.getItem(position).getPath()){//path sie rownaja, znaczy to tez ze cos juz gralo/gra
            if (comunicationWithClient.status == ComunicationClientServer.play){//to na co kliknolem gra wiec pause
                comunicationWithClient.status = ComunicationClientServer.pasue;
                Toast.makeText(context, "Pause", Toast.LENGTH_LONG).show();
                listMusicAdapter.getItem(position).setRaw(R.drawable.play);
                listMusicAdapter.notifyDataSetChanged();

            }else if (comunicationWithClient.status == ComunicationClientServer.pasue) {//to w co kliknolem jest pause, uruchom to
                comunicationWithClient.status = ComunicationClientServer.play;
                Toast.makeText(context, "Play", Toast.LENGTH_LONG).show();
                listMusicAdapter.getItem(position).setRaw(R.drawable.pause);
                listMusicAdapter.notifyDataSetChanged();
            }
            comunicationWithClient.run();
        }else {//path sa rozne
            if (this.path == null){//czyli jeszcze nic nie gralo bo path pusty
                comunicationWithClient.status = ComunicationClientServer.play;
                listMusicAdapter.getItem(position).setRaw(R.drawable.pause);
                listMusicAdapter.notifyDataSetChanged();
                this.path = listMusicAdapter.getItem(position).getPath();
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
                listMusicAdapter.getItem(isPlayingId).setRaw(0);
                listMusicAdapter.getItem(position).setRaw(R.drawable.pause);
                listMusicAdapter.notifyDataSetChanged();

                this.path = listMusicAdapter.getItem(position).getPath();
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
        //todo zastanawiam sie czy nie dac tutaj calkowitej komunikacji miedzy client server, tzn
        //todo zeby usunac polaczenie odbierajace przy wysylaniu w serverze a zeby bylo tu
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
                    byte[] odpByte = null;
                    int licz = 0;
                    Log.i("INFO", "Pominieto " + inputStream.skip(getSizeTag(inputStream)) + "byte");
                    while ((len = inputStream.read(buf)) != -1 && !ComunicationClientServer.checkResponse(ComunicationClientServer.changeSong, comunicationWithClient.status)) {
                        licz++;
                        oos.write(buf, 0, len);
                        Log.i("loop", licz+"");
                        if (licz == ComunicationClientServer.sizeOfPackage){
                            licz = 0;
                            Log.i("INFO", "Zerujemy licz i wysylamy zaraz");
                            oos.flush();

                            odpByte = new byte[ComunicationClientServer.sizeOfResponse];
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