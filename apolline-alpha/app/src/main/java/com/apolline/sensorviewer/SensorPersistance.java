package com.apolline.sensorviewer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;



@Entity(tableName = "sensor_values")
public class SensorPersistance {
    @PrimaryKey
    public long date;

    @ColumnInfo(name = "dateLocal")
    public long dateLocal;

    @ColumnInfo(name = "deviceName")
    public String deviceName;

    @ColumnInfo(name = "deviceUUID")
    public String deviceUUID;

    @ColumnInfo(name = "rawValues")
    @TypeConverters({DAOStringArrayConverter.class})
    public String[] rawValues;

    public void fromDataModel(SensorDataModel model) {
        date = model.getDate().getTime();
        dateLocal = model.getDateLocal().getTime();
        rawValues = model.rawValues;
        deviceName = model.getDeviceName();
        deviceUUID = model.getDeviceUUID();
    }
}
