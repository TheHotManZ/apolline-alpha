package com.apolline.sensorviewer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;

public class Sensors extends AppCompatActivity {
    private GraphView graphTemp;
    private TextView txtBatterie, txtTemp, tvState;
    private TextView tvPm1, tvPm25, tvPm10;

    private Boolean stop;
    private Boolean sensorsFill;
    private Context ctx;

    private String buf = "";

    /* GATT device and characteristics */
    private BluetoothGatt gattDevice;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;

    /* Data series */
    DataPoint[] pm1points, pm25points, pm10points;
    LineGraphSeries<DataPoint> pm1series, pm25series, pm10series;

    /* File sync */
    private final String CSV_FILENAME = "sensor_data.csv";

    /* Backported from the original app - might not be useful right now, keeping it for "what-if" purposes */
     @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void setupGattCharacteristics(List<BluetoothGattService> gattServices)
    {
        if (gattServices == null) return;
        String uuid;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            System.out.println("GATT Service UUID " + uuid);
            String LIST_NAME = "NAME";
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            String LIST_UUID = "UUID";
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);
            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                System.out.println("GATT Service Characteristic " + uuid);
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        System.out.println("GATT characteristics have been set up");
    }

    private DataPoint[] listToArrayDP(List<DataPoint> list) {
        DataPoint[] array = new DataPoint[list.size()];
        for(int i = 0; i < list.size(); i++)
            array[i] = list.get(i);

        return array;
    }

    private Date stringToDate(String str) {
        // 11/03/2020, 18:54
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy, HH:mm");
        try {
            return format.parse(str);
        } catch(ParseException ex) {
            return null;
        }
    }

    public void goBackButton(View view) {
        finish();
    }

    /* TODO : might be doing this better by just opening the file on startup, and just appending on the fly */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void commitCSV(SensorDataModel data)
    {
        File file = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if(!file.exists())
            file.mkdir();



        try {
           File outFile = new File(file, CSV_FILENAME);
           FileWriter wr = new FileWriter(outFile, true);
            wr.append(data.toCSVLine());
            wr.flush();
            wr.close();
            System.out.println("Saving to " + outFile.toString());
        } catch (Exception e) {
            System.out.println("Couldn't save data: " + e.getMessage());
        }
    }

    /* Incoming data from BLE device */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            /* Get broadcasted data */
            SensorDataModel data = (SensorDataModel) intent.getParcelableExtra("Data");

            /* Update graph */
            try {
                pm1series.appendData(new DataPoint(data.getDate(), data.getPm1()), true, 100, false);
                pm25series.appendData(new DataPoint(data.getDate(), data.getPm25()), true, 100, false);
                pm10series.appendData(new DataPoint(data.getDate(), data.getPm10()), true, 100, false);
            } catch (Exception e) {
                Log.i("BLU", "There was an error appending data to the graph. " + e.getMessage());
            }
            /* Update battery power */
            Double bat = data.getVolt();
            if (bat >= 3.97){
                txtBatterie.setText("Batterie: 80-100%");
                txtBatterie.setTextColor(Color.BLACK);
            } else if (bat >= 3.87) {
                txtBatterie.setText("Batterie: 60-80%");
                txtBatterie.setTextColor(Color.BLACK);
            } else if (bat >= 3.79) {
                txtBatterie.setText("Batterie: 40-60%");
                txtBatterie.setTextColor(Color.BLACK);
            } else if (bat >= 3.70) {
                txtBatterie.setText("Batterie: 20-40%");
                txtBatterie.setTextColor(Color.BLACK);
            } else {
                txtBatterie.setText("Batterie: 0-20%");
                txtBatterie.setTextColor(Color.RED);
            }

            /* Update temperature */
            txtTemp.setText("Température: " + data.getTempC() + "°C / " + data.getTempK() + "K");

            /* Set displayed values of sensors */
            tvPm1.setText(String.format("%.1f", data.getPm1()));
            tvPm25.setText(String.format("%.1f", data.getPm25()));
            tvPm10.setText(String.format("%.1f", data.getPm10()));

            /* Save data */
            /* commitCSV(data); */
        }
    };

    /* Status update from BLE device */
    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            /* Get broadcasted data */
            String data = (String)intent.getStringExtra("status");
            tvState.setText(data);
        }
    };

    private boolean checkBLEService()
    {
        ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BLEService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);

        /* Save context for threads */
        ctx = this;

        /* Get references to view objects */
        graphTemp = findViewById(R.id.graphTemp);
        txtBatterie = findViewById(R.id.txtBatterie);
        txtTemp = findViewById(R.id.txtTemp);
        tvState = findViewById(R.id.tvState);
        tvPm1 = findViewById(R.id.tvPm1);
        tvPm25 = findViewById(R.id.tvPm25);
        tvPm10 = findViewById(R.id.tvPm10);

        /* Init graph series */
        pm1series = new LineGraphSeries<DataPoint>();
        pm25series = new LineGraphSeries<DataPoint>();
        pm10series = new LineGraphSeries<DataPoint>();

        pm1series.setTitle("PM1");
        pm1series.setColor(Color.rgb(255,0,0));
        pm25series.setTitle("PM2.5");
        pm25series.setColor(Color.rgb(0,255,0));
        pm10series.setTitle("PM10");
        pm10series.setColor(Color.rgb(0,0,255));

        graphTemp.addSeries(pm1series);
        graphTemp.addSeries(pm25series);
        graphTemp.addSeries(pm10series);

        graphTemp.getLegendRenderer().setVisible(true);
        graphTemp.getGridLabelRenderer().setHorizontalLabelsVisible(true);
        graphTemp.getGridLabelRenderer().setHorizontalLabelsAngle(90);
        graphTemp.getViewport().setScalable(true);
        graphTemp.getViewport().setScrollable(true);
        graphTemp.getViewport().setScalableY(true);
        graphTemp.getViewport().setScrollableY(true);
        graphTemp.getViewport().setYAxisBoundsManual(true);
        graphTemp.getViewport().setXAxisBoundsManual(true);
        graphTemp.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(ctx, new SimpleDateFormat("HH:mm:ss")));
        graphTemp.getGridLabelRenderer().setNumHorizontalLabels(10);
        // set manual x bounds to have nice steps
        graphTemp.getGridLabelRenderer().setHumanRounding(false);

        stop = false;
        sensorsFill = false;

        /* Listen to data coming from BLE service */
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("SensorUpdate"));
        /* Listen to status coming from BLE service */
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mStatusReceiver, new IntentFilter("StatusUpdate"));
    }

    @Override
    protected void onDestroy() {
        stop = true;
        sensorsFill = false;

        /* Unregister message receivers */
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusReceiver);

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        stop = true;
        sensorsFill = false;
        super.onPause();
    }
}
