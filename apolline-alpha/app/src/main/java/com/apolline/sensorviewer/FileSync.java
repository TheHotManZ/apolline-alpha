package com.apolline.sensorviewer;

import java.io.FileNotFoundException;

public class FileSync {
    public static final String SYNC_FILENAME = "sensor_out.csv";

    public static void AddCSVEntry(SensorDataModel data) throws FileNotFoundException {
        String csv = data.toCSVLine();


    }
}
