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

    public static Point buildMeasurement(SensorDataModel data, int value)
    {
        /* Check we have a measurement for this */
        String tbName = SensorDataModel.SENSOR_MEASUREMENTS[value];
        if(tbName.equals("")) {
            Log.i("IDB", "Null measurement name for data index " + value);
            return null;
        }

        /* Build measurement */
        Point pm = Point.measurement(SensorDataModel.SENSOR_MEASUREMENTS[value])
                .time(data.getDateLocal().getTime(), TimeUnit.MILLISECONDS)
                .tag("device", data.getDeviceName())
                .tag("geohash", data.getGeohash())
                .tag("provider", "no")
                .tag("transport", "no")
                .tag("unit", SensorDataModel.SENSOR_UNITS[value])
                .tag("uuid", data.getDeviceUUID())
                .field("latitude", data.getDouble(SensorDataModel.SENSOR_LATITUDE))
                .field("longitude", data.getDouble(SensorDataModel.SENSOR_LONGITUDE))
                .field("value", data.getDouble(value))
                .field("gpstime", data.getDate().getTime())
                .build();

        return pm;
    }

    public static boolean influxSend(SensorDataModel data, Context ctx)
    {
        try {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(ctx /* Activity context */);

            /* Try to get the last sync time; sync everything if no time */
            String node = sharedPreferences.getString("influx_node", "QZ");
            String db = sharedPreferences.getString("influx_database", "apolline");

            /*influxDB.write(Point.measurement("sensor_data")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    //.tag("location", "santa_monica")
                    .addField("pm1", data.getPm1())
                    .addField("pm25", data.getPm25())
                    .addField("pm10", data.getPm10())
                    .addField("temp", data.getTempC())
                    .build());*/
            /*Point pm1 = Point.measurement("dust_pm1")
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

            influxDB.write(p);*/

            Point pm1 = buildMeasurement(data, SensorDataModel.SENSOR_PM_1);
            Point pm25 = buildMeasurement(data, SensorDataModel.SENSOR_PM_2_5);
            Point pm10 = buildMeasurement(data, SensorDataModel.SENSOR_PM_10);
            Point pm1above = buildMeasurement(data, SensorDataModel.SENSOR_PM_1_ABOVE);
            Point pm25above = buildMeasurement(data, SensorDataModel.SENSOR_PM_25_ABOVE);
            Point pm10above = buildMeasurement(data, SensorDataModel.SENSOR_PM10_ABOVE);
            Point pm03above = buildMeasurement(data, SensorDataModel.SENSOR_PM_03_ABOVE);
            Point pm05above = buildMeasurement(data, SensorDataModel.SENSOR_PM_05_ABOVE);
            Point pm5above = buildMeasurement(data, SensorDataModel.SENSOR_PM5_ABOVE);
            Point tempC = buildMeasurement(data, SensorDataModel.SENSOR_TEMP);
            Point hum = buildMeasurement(data, SensorDataModel.SENSOR_HUMIDITY);

            /* Leftovers requiring additional parsing : temperature °K and compensated humidity */
            Point tempK = Point.measurement("temperature.k")
                    .time(data.getDateLocal().getTime(), TimeUnit.MILLISECONDS)
                    .tag("device", data.getDeviceName())
                    .tag("geohash", data.getGeohash())
                    .tag("provider", "no")
                    .tag("transport", "no")
                    .tag("unit", Unit.TEMPERATURE_KELVIN)
                    .tag("uuid", data.getDeviceUUID())
                    .field("latitude", data.getDouble(SensorDataModel.SENSOR_LATITUDE))
                    .field("longitude", data.getDouble(SensorDataModel.SENSOR_LONGITUDE))
                    .field("value", data.getTempK())
                    .field("gpstime", data.getDate().getTime())
                    .build();

            Point rht = Point.measurement("humidity.compensated")
                    .time(data.getDateLocal().getTime(), TimeUnit.MILLISECONDS)
                    .tag("device", data.getDeviceName())
                    .tag("geohash", data.getGeohash())
                    .tag("provider", "no")
                    .tag("transport", "no")
                    .tag("unit", Unit.PERCENTAGE)
                    .tag("uuid", data.getDeviceUUID())
                    .field("latitude", data.getDouble(SensorDataModel.SENSOR_LATITUDE))
                    .field("longitude", data.getDouble(SensorDataModel.SENSOR_LONGITUDE))
                    .field("value", data.getDouble(SensorDataModel.SENSOR_HUMIDITY) / (1.0546 - 0.00216 * (data.getTempK() - 273.15)) * 10)
                    .field("gpstime", data.getDate().getTime())
                    .build();

            BatchPoints p = BatchPoints.database(db).points(pm1, pm25, pm10, pm1above, pm25above, pm10above, pm03above, pm05above, pm5above, tempC, tempK, hum, rht).build();

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

        boolean hadError = false;

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
                hadError = true;
            }
        }

        /* Only update last full sync time if there is no orphan values left */
        if(!hadError)
        {
            /* Update last sync time */
            sharedPreferences.edit().putLong("influx_lastsync", curTime).apply();
        }
    }
}
