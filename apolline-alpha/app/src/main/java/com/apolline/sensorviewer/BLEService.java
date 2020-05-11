package com.apolline.sensorviewer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.UUID;

public class BLEService extends Service {
    Context ctx;
    boolean requestedDisconnect = false;
    Boolean syncToInflux = false;

    /* BLE Callback */
    @SuppressLint("NewApi")
    BluetoothGattCallback ble_callback = new BluetoothGattCallback() {
        private String buf;

        /* When a characteristic is written - unused */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            System.out.println("Characteristic was written, status " + status);
        }

        /* When the connection state changes - used to start configuration process, or reconnect when disconnected */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                System.out.println("GATT - Connected to GATT server");
                updateStatus("Connecté au capteur");
                buf = "";
                System.out.println("GATT - Attempting to start service discovery -> " + gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                System.out.println("GATT - Disconnected from GATT server, reconnecting");
                if(requestedDisconnect == false) {
                    updateStatus("Connexion perdue - reconnexion");
                    gattDevice.connect();
                }
                else {
                    /* Close device and stop service */
                    gattDevice.close();
                    stopForeground(true);
                    stopSelf();
                }
            }
        }

        /* When services have been discovered - configure the Dust Sensor once it has been found */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateStatus("Configuration du capteur...");
                System.out.println("GATT - Service discovered");

                BluetoothGattService mSVC = gatt.getService(UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"));
                BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(SampleGattAttributes.DATA_DUST_SENSOR));
                int prop = mCH.getProperties();

                /* Reads characteristic first and wait a sec */
                if (prop != 0 || BluetoothGattCharacteristic.PROPERTY_READ > 0)
                    gatt.readCharacteristic(mCH);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                /* Enable notifications */
                if (prop != 0 || BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    gatt.setCharacteristicNotification(mCH, true);

                    // This is specific to DUST SENSOR. D.C 20180112
                    if (UUID.fromString(SampleGattAttributes.DATA_DUST_SENSOR).equals(mCH.getUuid())) {
                        BluetoothGattDescriptor desc = mCH.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (gatt.writeDescriptor(desc))
                            System.out.println("Enabled notifications for Dust Sensor");
                        else
                            System.out.println("Can't enable notifications for Dust Sensor");
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("GATT - Service discovery status " + status);
            }
        }

        /* When a descriptor has been written - we use this to start data streaming from the sensor */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            System.out.println("Descriptor written - now we should enable streaming requests");
            updateStatus("Début de l'envoi des données");

            BluetoothGattService mSVC = gatt.getService(UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"));
            BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(SampleGattAttributes.DATA_DUST_SENSOR));

            mCH.setValue("c");
            if (gatt.writeCharacteristic(mCH)) {
                System.out.println("Write OK");
            } else System.out.println("Write NOT OK");

            updateStatus("Acquisition en cours.");
            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvState.setText("Données du capteur");
                }
            });*/

        }

        /* When a characteristic has been read - unused */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Char read " + characteristic.getStringValue(0));
            } else {
                System.out.println("Error during read of char, status " + status);
            }
        }

        public void updateStatus(String state)
        {
            Intent intent = new Intent("StatusUpdate");
            // You can also include some extra data
            intent.putExtra("status", state);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        }

        /* TODO: add handling for unparseable data
            Add handling for graph insertion failures
         */
        /* When a characteristic has changed - main function used to handle reception of new data from the sensor */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //super.onCharacteristicChanged(gatt, characteristic);
            System.out.println("Chara change, UUID " + characteristic.getUuid().toString());
            String curBuf = characteristic.getStringValue(0);
            buf += curBuf;
            System.out.println("buf now: " + buf);

            /* We read until the whole data 'line' has been read. Once it is done, parse the line */
            if (buf.contains("\n")) {
                SensorDataModel data = new SensorDataModel();
                if(data.StringToModel(buf))
                {
                    Intent intent = new Intent("SensorUpdate");
                    // You can also include some extra data
                    intent.putExtra("Data", data);
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

                    /* Sync with Influx */
                    if(syncToInflux == true) InfluxDBSync.influxSend(data);

                    /* Insert value into local DB */
                    try {
                        AppDatabase db = AppDatabaseSingleton.getInstance(ctx);
                        SensorPersistance sp = new SensorPersistance();
                        sp.fromDataModel(data);
                        db.sensorDao().insert(sp);
                    } catch (Exception e)
                    {
                        System.out.println("DB: can't persist: " + e.getMessage());
                    }
                }

                buf = "";
            }
        }
    };

    /* Notification channel */
    public static final String CHANNEL_ID = "science.apolline.bleu";

    /* GATT device */
    BluetoothGatt gattDevice;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ctx = this;
        if(intent.getAction().equals("BLE_STOP"))
        {
            requestedDisconnect = true;
            try {
                gattDevice.disconnect();
            } catch (Exception e)
            {

            }
        } else {
            /* Create a notification for the foreground service */
            requestedDisconnect = false;
            createNotificationChannel();
            Intent notificationIntent = new Intent(this, NotificationHandlerActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Capteur bleu")
                    .setContentText("Acquisition des mesures en cours...")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentIntent(pendingIntent)
                    .build();

            /* Starts the BLE service as a foreground service, so that Android _might_ not shut it down too soon */
            startForeground(1, notification);

            /* Initialize SimpleGattAttributes */
            SampleGattAttributes.init();

            /* Initialize InfluxDB Sync */
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
            syncToInflux = sharedPreferences.getBoolean("influx_sync", false);
            String url = sharedPreferences.getString("influx_url", "");
            String user = sharedPreferences.getString("influx_user", "");
            String pass = sharedPreferences.getString("influx_pass", "");
            if(syncToInflux == true) InfluxDBSync.influxSetup(url, user, pass);

            /* Start the GATT callback, then start the threads */
            BluetoothDevice dev = Utils.getSelectedDevice();
            if(gattDevice != null)
                gattDevice.connect();
            else
                gattDevice = dev.connectGatt(this, false, ble_callback);

            //stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensors channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
