package com.apolline.sensorviewer;

import com.squareup.okhttp.OkHttpClient;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.util.concurrent.TimeUnit;

public class InfluxDBSync {
    //final static String serverURL = "http://192.168.1.33:8086", username = "android", password = "android";
    private static InfluxDB influxDB;

    public static void influxSetup(String url, String user, String pass)
    {
        try {
            influxDB = InfluxDBFactory.connect(url, user, pass);
            System.out.println("InfluxDB: Connected");
        } catch (Exception e)
        {
            System.out.println("InfluxDB: Couldn't connect to InfluxDB (" + e.getMessage() + ")");
        }
    }

    public static void influxSend(SensorDataModel data)
    {
        try {
            /*influxDB.write(Point.measurement("sensor_data")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    //.tag("location", "santa_monica")
                    .addField("pm1", data.getPm1())
                    .addField("pm25", data.getPm25())
                    .addField("pm10", data.getPm10())
                    .addField("temp", data.getTempC())
                    .build());*/
            influxDB.write(BatchPoints.database("sensor_data")
                    .points(Point.measurement("sensor_values")
                            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            //.tag("location", "santa_monica")
                            .field("pm1", data.getPm1())
                            .field("pm25", data.getPm25())
                            .field("pm10", data.getPm10())
                            .field("temp", data.getTempC())
                            .build())
                    .build());
        } catch (Exception e)
        {
            System.out.println("InfluxDB: Couldn't send data - " + e.getMessage());
        }
    }
}
