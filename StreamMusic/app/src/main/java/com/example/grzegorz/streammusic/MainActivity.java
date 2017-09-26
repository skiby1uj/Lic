package com.example.grzegorz.streammusic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener{


    LinkedList<ListDeviceConnectRow> listDeviceConnectRows = new LinkedList<>();
    RadioButton chWysylaj = null;
    RadioButton chOdbieraj = null;
    Button bPlay = null;
    String adressMac = null;
    int lastSelectionPositionMac = -1;
    Context context = null;
    ListView listViewMusic = null;
    ListDeviceConnectAdapter listDeviceConnectAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = this;
        this.runBluetooth();
        this.listViewMusic = (ListView)findViewById(R.id.listViewMusic);

        chWysylaj = (RadioButton)findViewById(R.id.chWysylaj);
        chOdbieraj = (RadioButton)findViewById(R.id.chOdbieraj);
        bPlay = (Button) findViewById(R.id.bConnect);

        chWysylaj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chOdbieraj.isChecked()){
                    chOdbieraj.setChecked(false);
                }
                bPlay.setEnabled(true);
                showSearchDeviceBLuetooth(new LinkedList<ListDeviceConnectRow>());
            }
        });

        chOdbieraj.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                wykryjInne();
                if (chWysylaj.isChecked()){
                    chWysylaj.setChecked(false);
                }
                bPlay.setEnabled(false);
                showSearchDeviceBLuetooth(listDeviceConnectRows);
            }
        });

        bPlay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {//server
                if (chWysylaj.isChecked()){
                    dajSieWykryc();//nie blokuje ekranu :)
                    ServerBluetooth serverBluetooth = new ServerBluetooth(context);
                    serverBluetooth.showMusic(listViewMusic);
                    Toast.makeText(context, "Udalo sie polaczyc z klientem", Toast.LENGTH_LONG).show();
                }
                else if (chOdbieraj.isChecked()){//klient
                    showSearchDeviceBLuetooth(listDeviceConnectRows);
                    new ClientBluetooth(adressMac, context);
                    Toast.makeText(context, "Udalo sie polaczyc z serverem", Toast.LENGTH_LONG).show();
                    unregisterReceiver(odbiorca);
                    //NEXUS 60:A4:4C:C3:2C:6A  SONY BC:6E:64:B5:C1:45  LG 98:D6:F7:C9:98:45
                }
                else {
                    Toast.makeText(v.getContext(), "Nie wybrano funkcjonalnosci urzadzenia", Toast.LENGTH_LONG).show();
                }

                //TODO w zaleznosci co zostalo wybrane to uruchamiamy (klient or server)
                //TODO jesli bedzie to server dac informacje ze czeka na polaczenie
                //TODO jesli bedzie to klient wyswietlic liste dostepnych urzadzen do podlaczenia
                Toast.makeText(v.getContext(), "Click Listener Connect", Toast.LENGTH_LONG).show();
                chWysylaj.setEnabled(false);
                chOdbieraj.setEnabled(false);
                bPlay.setEnabled(false);
            }
        });
    }

    public void showSearchDeviceBLuetooth(LinkedList<ListDeviceConnectRow> list){
        ContentResolver cr = this.getContentResolver();
        ListView listV = (ListView)findViewById(R.id.listViewMusic);
        listDeviceConnectAdapter = new ListDeviceConnectAdapter(this, R.layout.row_bluetooth_server_adress, list);
        listV.setAdapter(listDeviceConnectAdapter);
        listV.setOnItemClickListener(this);
    }

    void runBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
        }
    }

    public void dajSieWykryc(){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(intent);
    }

    public void wykryjInne(){
        Log.i("INFO", "Probuje wykryc");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(odbiorca, filter);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();
        Log.i("INFO", "Koniec wykrywania");
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
                listDeviceConnectRows.add(new ListDeviceConnectRow(bluetoothDevice.getName(), bluetoothDevice.getAddress()));
                Log.e("INFO", "znaleziono urządzenie: " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
                listDeviceConnectAdapter.notifyDataSetChanged();
            }
        }
    };


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            bPlay.setEnabled(true);
            if (adressMac == null) {
                parent.getChildAt(position).setBackgroundColor(Color.BLUE);
                lastSelectionPositionMac = position;
                TextView w = (TextView) parent.getChildAt(position).findViewById(R.id.textAdressDevice);
                adressMac = w.getText().toString();
            }
            if (adressMac != null) {
                parent.getChildAt(lastSelectionPositionMac).setBackgroundColor(Color.WHITE);
                parent.getChildAt(position).setBackgroundColor(Color.BLUE);
                lastSelectionPositionMac = position;
                TextView w = (TextView) parent.getChildAt(position).findViewById(R.id.textAdressDevice);
                adressMac = w.getText().toString();
            }
            Toast.makeText(context, "Wybrano " + listDeviceConnectRows.get(position).getName(), Toast.LENGTH_LONG).show();

    }
}