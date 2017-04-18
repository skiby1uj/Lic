package com.example.grzegorz.streammusic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

public class MainActivity extends Activity {

    RadioButton chWysylaj = null;
    RadioButton chOdbieraj = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.runBluetooth();
        chWysylaj = (RadioButton)findViewById(R.id.chWysylaj);
        chOdbieraj = (RadioButton)findViewById(R.id.chOdbieraj);

//        dajSieWykryc();
//        wykryjInne();
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
            Thread t = new ServerBluetooth(this);
            t.start();
//            new ServerBluetooth(this).run();
        }
        else if (chOdbieraj.isChecked()){
            Thread t = new ClientBluetooth("60:A4:4C:C3:2C:6A", this);
            t.start();
//            new ClientBluetooth("60:A4:4C:C3:2C:6A", this).run();//NEXUS 60:A4:4C:C3:2C:6A  SONY BC:6E:64:B5:C1:45  LG 98:D6:F7:C9:98:45
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
}
