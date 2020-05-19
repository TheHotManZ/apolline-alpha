package com.apolline.sensorviewer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.squareup.okhttp.OkHttpClient;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InfluxDBSync {
    //final static String serverURL = "http://192.168.1.33:8086", username = "android", password = "android";
    private static InfluxDB influxDB;

    public static boolean influxSetup(String url, String user, String pass)
    {
        try {
            influxDB = InfluxDBFactory.connect(url, user, pass);
            Log.i("IDB", "InfluxDB: Connected");
            return true;
        } catch (Exception e)
        {
            Log.i("IDB", "InfluxDB: Couldn't connect to InfluxDB (" + e.getMessage() + ")");
            return false;
        }
    }

    public static boolean influxSend(SensorDataModel data, Context ctx)
    {
        try {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(ctx /* Activity context */);

            /* Try to get the last sync time; sync everything if no time */
            String node = sharedPreferences.getString("influx_node", "QZ");

            /*influxDB.write(Point.measurement("sensor_data")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    //.tag("location", "santa_monica")
                    .addField("pm1", data.getPm1())
                    .addField("pm25", data.getPm25())
                    .addField("pm10", data.getPm10())
                    .addField("temp", data.getTempC())
                    .build());*/
            Point pm1 = Point.measurement("dust_pm1")
                    .time(data.getDate().getTime(), TimeUnit.MILLISECONDS)
                    .tag("device", node)
                    .field("value", data.getPm1())
                    .field("localtime", data.getDateLocal().getTime()).build();
            Point pm25 = Point.measurement("dust_pm2.5")
                    .time(data.getDate().getTime(), TimeUnit.MILLISECONDS)
                    .tag("device", node)
                    .field("value", data.getPm25())
                    .field("localtime", data.getDateLocal().getTime()).build();
            Point pm10 = Point.measurement("dust_pm10")
                    .time(data.getDate().getTime(), TimeUnit.MILLISECONDS)
                    .tag("device", node)
                    .field("value", data.getPm10())
                    .field("localtime", data.getDateLocal().getTime()).build();

            BatchPoints p = BatchPoints.database("qarpediem").points(pm1, pm25, pm10).build();

            influxDB.write(p);
            return true;
        } catch (Exception e)
        {
            Log.i("IDB", "InfluxDB: Couldn't send data - " + e.getMessage());
            return false;
        }
    }

    public static boolean influxRegisterNode(Context ctx)
    {
        try {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(ctx /* Activity context */);

            /* Try to get the last sync time; sync everything if no time */
            String node = sharedPreferences.getString("influx_node", "QZ");


            influxDB.write(BatchPoints.database("qarpediem")
                    .points(Point.measurement("address")
                            .tag("device", node)
                            .field("dummy", "")
                            .build())
                    .build());
            return true;
        } catch (Exception e)
        {
            Log.i("IDB", "InfluxDB: Couldn't send data - " + e.getMessage());
            return false;
        }
    }

    public static void syncSinceLastSync(Context ctx)
    {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(ctx /* Activity context */);

        Long curTime = Calendar.getInstance().getTimeInMillis();

        /* Try to get the last sync time; sync everything if no time */
        Long lastSync = sharedPreferences.getLong("influx_lastsync", Long.parseLong("0"));
        Log.i("IDB", "SYNC: Last sync on " + new Date(lastSync).toString());

        /* Fetch all values since last sync */
        AppDatabase db = AppDatabaseSingleton.getInstance(ctx);
        List<SensorPersistance> values = db.sensorDao().loadAllFromLocalDate(lastSync);

        /* Some debug output */
        Log.i("IDB", "SYNC: we have " + values.size() + " values to sync");

        /* Register node */
        // influxRegisterNode(ctx);

        /* Sync each value and remove from DB */
        for(SensorPersistance s : values)
        {
            /* Broadcast sync status */
            Intent intent = new Intent("SyncUpdate");
            // You can also include some extra data
            intent.putExtra("Current", values.indexOf(s));
            intent.putExtra("Total", values.size());
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

            /* Create SensorDataModel and sync */
            SensorDataModel m = new SensorDataModel();
            m.fromPersistance(s);
            if(influxSend(m, ctx))
            {
                /* Remove from local DB if sync succeeded */
                db.sensorDao().delete(s);

                Log.i("IDB", "Synced value taken at " + m.getDate().toString() + " GPS time");
            } else {
                Log.i("IDB", "Couldn't sync value taken at " + m.getDate().toString() + " GPS time");
            }
        }

        /* Update last sync time */
        sharedPreferences.edit().putLong("influx_lastsync", curTime).apply();
    }
}
