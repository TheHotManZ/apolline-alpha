package com.apolline.sensorviewer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button connectBtn;
    private Button cfgBtn;
    private Button sensorBtn;
    private Button stopBtn;

    private Boolean stop;

    public void showSensors(View view) {
        startActivity(new Intent(this, Sensors.class));
    }

    public void openConfigurationActivity(View view) {
        startActivity(new Intent(this, Settings.class));
    }

    public void openConnectionActivity(View view) {
        if(!Utils.hasRegistered()) {
            startActivity(new Intent(this, Connection.class));
        } else {
            stop = true;
            Utils.unregisterAddress();
            connectBtn.setText("Connexion");
            sensorBtn.setEnabled(false);

            /* Stop BLE service */
            Intent stopIntent = new Intent(this, BLEService.class);
            stopIntent.setAction("BLE_STOP");
            startService(stopIntent);

            new AlertDialog.Builder(this)
                    .setTitle("Message")
                    .setMessage("Déconnexion réussie")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {}
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectBtn = findViewById(R.id.connectbutton);
        cfgBtn = findViewById(R.id.cfgbutton);
        sensorBtn = findViewById(R.id.sensorbutton);
        stopBtn = findViewById(R.id.btnStopAll);
        stop = true;

        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if(bt == null || !bt.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth non détecté")
                    .setMessage("Veuillez activer le Bluetooth dans les paramètres de " +
                            "votre smartphone.")
                    .setPositiveButton("Quitter", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();
        }

        if(Utils.hasRegistered()) {
            connectBtn.setText("Déconnexion");
            cfgBtn.setEnabled(true);
            sensorBtn.setEnabled(true);
            stop = false;
        } else {
            stop = true;
            connectBtn.setText("Connexion");
            sensorBtn.setEnabled(false);
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        if(Utils.hasRegistered()) {

            connectBtn.setText("Déconnexion");
            cfgBtn.setEnabled(true);
            sensorBtn.setEnabled(true);
            stop = false;
        } else {
            stop = true;
            connectBtn.setText("Connexion");
            sensorBtn.setEnabled(false);
        }
    }

    @Override
    public void onPause() {
        stop = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        stop = true;
        super.onDestroy();
    }

    public void emergencyExit(View view) {
        Intent stopIntent = new Intent(MainActivity.this, BLEService.class);
        stopIntent.setAction("BLE_STOP");
        startService(stopIntent);
    }
}
