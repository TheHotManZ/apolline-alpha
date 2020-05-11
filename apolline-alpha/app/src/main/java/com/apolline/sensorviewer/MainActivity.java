package com.apolline.sensorviewer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Button connectBtn;
    private Button cfgBtn;
    private Button sensorBtn;
    private Button stopBtn;
    private Button influxBtn;
    private TextView tvLastSync;
    private ProgressBar pbSync;

    private Boolean stop;
    private Context ctx;

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
        influxBtn = findViewById(R.id.btnInflux);
        tvLastSync = findViewById(R.id.tvLastSync);
        pbSync = findViewById(R.id.pbSync);
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

        /* InfluxDB sync */
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        Long lastSync = sharedPreferences.getLong("influx_lastsync", Long.parseLong("0"));
        Date dtLastSync = new Date(lastSync);
        tvLastSync.setText("Dernière sync.: " + dtLastSync.toString());

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mStatusReceiver, new IntentFilter("SyncUpdate"));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusReceiver);
        super.onDestroy();
    }

    public void emergencyExit(View view) {
        Intent stopIntent = new Intent(MainActivity.this, BLEService.class);
        stopIntent.setAction("BLE_STOP");
        startService(stopIntent);
    }

    /* InfluxDB sync status receiver */
    BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* Get broadcasted data */
            int current = intent.getIntExtra("Current", 0);
            int total = intent.getIntExtra("Total", 0);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pbSync.setMax(total);
                    pbSync.setProgress(current, true);
                }
            });
        }
    };

    /* InfluxDB sync Runnable */
    private Runnable syncIDB = new Runnable() {
        @Override
        public void run() {
            /* Disable input */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cfgBtn.setEnabled(false);
                    connectBtn.setEnabled(false);
                    sensorBtn.setEnabled(false);
                    influxBtn.setEnabled(false);
                    tvLastSync.setText("Synchronisation en cours...");
                }
            });

            /* Get settings for Influx connection, and connect */
            try {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(ctx /* Activity context */);
                String url = sharedPreferences.getString("influx_url", "");
                String user = sharedPreferences.getString("influx_user", "");
                String pass = sharedPreferences.getString("influx_pass", "");
                if(InfluxDBSync.influxSetup(url, user, pass))
                {
                    /* Sync */
                    InfluxDBSync.syncSinceLastSync(ctx);

                    /* Update last sync date */
                    Long lastSync = sharedPreferences.getLong("influx_lastsync", Long.parseLong("0"));
                    Date dtLastSync = new Date(lastSync);

                    /* Enable UI again */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cfgBtn.setEnabled(true);
                            connectBtn.setEnabled(true);
                            sensorBtn.setEnabled(true);
                            influxBtn.setEnabled(true);
                            tvLastSync.setText("Dernière sync.: " + dtLastSync.toString());
                        }
                    });
                } else {
                    /* Enable UI again */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cfgBtn.setEnabled(true);
                            connectBtn.setEnabled(true);
                            sensorBtn.setEnabled(true);
                            influxBtn.setEnabled(true);
                            tvLastSync.setText("Impossible de se connecteur au serveur InfluxDB.");
                        }
                    });
                }

            }
            catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cfgBtn.setEnabled(true);
                        connectBtn.setEnabled(true);
                        sensorBtn.setEnabled(true);
                        influxBtn.setEnabled(true);
                        tvLastSync.setText("Echec de la synchronisation.");
                        System.out.println("SYNC: " + e.getMessage());
                    }
                });


            }
        }
    };

    public void syncToInflux(View view) {
        ctx = this;
        new Thread(syncIDB).start();
    }
}
