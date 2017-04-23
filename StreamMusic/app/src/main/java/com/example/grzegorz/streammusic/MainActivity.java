package com.example.grzegorz.streammusic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.LinkedList;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener{

    LinkedList<ListMusicRow> list = new LinkedList<>();
    RadioButton chWysylaj = null;
    RadioButton chOdbieraj = null;
    Button bPlay = null;
    boolean isServer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.onCreateDialog(savedInstanceState, this).show();
        this.runBluetooth();
        chWysylaj = (RadioButton)findViewById(R.id.chWysylaj);
        chOdbieraj = (RadioButton)findViewById(R.id.chOdbieraj);
        chOdbieraj.setEnabled(false);
        chWysylaj.setEnabled(false);
        bPlay = (Button) findViewById(R.id.button);
//        dajSieWykryc();
//        wykryjInne();
    }

    public void showMusic(){

        ListMusicAdapter listMusicAdapter;
        ContentResolver cr = this.getContentResolver();
        ListView listV = (ListView)findViewById(R.id.listViewMusic);

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
                    list.add(new ListMusicRow(title, path));
                }
                listMusicAdapter = new ListMusicAdapter(this, R.layout.row_music_element, list);
                listV.setOnItemClickListener(this);
                listV.setAdapter(listMusicAdapter);
            }
        }

        cur.close();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState, final Context context) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("W jaki sposób ma działać urządzenie?")
                .setPositiveButton("Wysyłać", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        showMusic();
                        isServer = true;
                        chWysylaj.setChecked(true);
                    }
                })
                .setNegativeButton("Odbierać", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        isServer = false;
                        chOdbieraj.setChecked(true);
                    }
                });
        // Create the AlertDialog object and return it

        return builder.create();
    }

    void runBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
        }
    }

    public void Bplay(View v){
        if (chWysylaj.isChecked()){
//            Thread t = new ServerBluetooth(this);
//            t.start();

        }
        else if (chOdbieraj.isChecked()){
            new ClientBluetooth("BC:6E:64:B5:C1:45", this);
//            t.start();
            //NEXUS 60:A4:4C:C3:2C:6A  SONY BC:6E:64:B5:C1:45  LG 98:D6:F7:C9:98:45
        }
        else {
            Toast.makeText(this, "Nie wybrano funkcjonalnosci urzadzenia", Toast.LENGTH_LONG).show();
        }
    }

    public void dajSieWykryc(){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(intent);
    }

    public void wykryjInne(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(odbiorca, filter);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();
    }

    public void chWysylajClick(View v){
        if (chOdbieraj.isChecked()){
            chOdbieraj.setChecked(false);
        }
    }

    public void chOdbierajClick(View v){
        if (chWysylaj.isChecked()){
            chWysylaj.setChecked(false);
        }
    }

    private final BroadcastReceiver odbiorca = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String akcja = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(akcja)){
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                String status = "";
                Log.e("INFO", "znaleziono urządzenie: " + bluetoothDevice.getName());
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, "" + list.get(position).getTitle(), Toast.LENGTH_LONG).show();
        Log.e("INFo", "inClick");
        Thread t = new ServerBluetooth(this, list.get(position).getPath());
        t.start();
    }
}