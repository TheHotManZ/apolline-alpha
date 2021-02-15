package com.apolline.sensorviewer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Connection extends AppCompatActivity {
    private ListView listView;
    private Switch autoconnect;
    private ArrayList<String> mDeviceStrList = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, BluetoothDevice> mDetectedDevices = new HashMap<String, BluetoothDevice>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                /*
                    Add discovered device to the array
                 */
                if(device.getName() == null) {
                   /* mDeviceStrList.add("Unknown Device\n" + device.getAddress());
                    mDetectedDevices.put(device.getAddress(), device);*/
                } else {
                    if(device.getName().toLowerCase().contains("appa")||device.getName().toLowerCase().contains("test")) {
                        mDeviceStrList.add(device.getName() + "\n" + device.getAddress());
                        mDetectedDevices.put(device.getAddress(), device);
                    }
                }

                listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, mDeviceStrList));
            }
        }
    };

    public void goBack(View view) {
        finish();
    }

    /* Dirty context storage */
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ctx = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        listView = findViewById(R.id.devices);
        autoconnect = findViewById(R.id.switch1);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                mBluetoothAdapter.cancelDiscovery();
                String mac = mDeviceStrList.get(i).split("\\n")[1];
                Set<BluetoothDevice> boundedDevices = mBluetoothAdapter.getBondedDevices();
                if(boundedDevices.contains(mDetectedDevices.get(mac)))
                {
                    Toast.makeText(getApplicationContext(),"No pairing required, connecting to device",Toast.LENGTH_SHORT).show();
                    Log.i("BLE", "No pairing required");
                    Utils.registerAddress(mac);
                    Utils.setSelectedDevice(mDetectedDevices.get(mac));
                } else {
                    Log.i("BLE", "Device requires pairing - default PIN is 4545");
                    Toast.makeText(getApplicationContext(),"Pairing required, default PIN is 4545",Toast.LENGTH_SHORT).show();
                    mDetectedDevices.get(mac).createBond();
                    Utils.registerAddress(mac);
                    Utils.setSelectedDevice(mDetectedDevices.get(mac));
                }

                /* Start BLE Service */
                Log.i("BLE", "Starting Bluetooth Low Energy service");
                Intent serviceIntent = new Intent(ctx, BLEService.class);
                serviceIntent.setAction("BLE_START");
                ContextCompat.startForegroundService(ctx, serviceIntent);

                finish();
            }
        });
        // Bluetooth discovery
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADMIN},
        1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}