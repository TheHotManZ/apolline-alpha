package com.apolline.sensorviewer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sensor_values")
public class SensorPersistance {
    @PrimaryKey
    public long date;

    @ColumnInfo(name = "dateLocal")
    public long dateLocal;

    @ColumnInfo(name = "pm1")
    public Double pm1;

    @ColumnInfo(name = "pm25")
    public Double pm25;

    @ColumnInfo(name = "pm10")
    public Double pm10;

    @ColumnInfo(name = "temp")
    public Double temp;

    @ColumnInfo(name = "volt")
    public Double volt;

    public void fromDataModel(SensorDataModel model) {
        date = model.getDate().getTime();
        dateLocal = model.getDate().getTime();
        pm1 = model.getPm1();
        pm25 = model.getPm25();
        pm10 = model.getPm10();
        temp = model.getTempC();
        volt = model.getVolt();
    }
}
