package com.apolline.sensorviewer;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import ch.hsr.geohash.GeoHash;

public class SensorDataModel implements Parcelable {
    /* Static configuration of sensor here */
    /* Indexes of values */
    public static final int SENSOR_DATE = 0;
    public static final int SENSOR_PM_1 = 1;
    public static final int SENSOR_PM_2_5 = 2;
    public static final int SENSOR_PM_10 = 3;
    public static final int SENSOR_PM_03_ABOVE   = 4;
    public static final int SENSOR_PM_05_ABOVE  = 5;
    public static final int SENSOR_PM_1_ABOVE  = 6;
    public static final int SENSOR_PM_25_ABOVE    = 7;
    public static final int SENSOR_PM5_ABOVE  = 8;
    public static final int SENSOR_PM10_ABOVE = 9;
    public static final int SENSOR_LATITUDE = 10;
    public static final int SENSOR_LONGITUDE = 11;
    public static final int SENSOR_ALTITUDE =  12;
    public static final int SENSOR_SPEED    = 13;
    public static final int SENSOR_SAT  =   14;
    public static final int SENSOR_TEMPC    = 15;
    public static final int SENSOR_PRESSURE = 16;
    public static final int SENSOR_TEMP = 17;
    public static final int SENSOR_HUMIDITY = 18;
    public static final int SENSOR_VOLT = 19;
    public static final int SENSOR_INTERNALTEMP = 20;
    public static final int SENSOR_INTERNALHUMIDITY = 21;

    public static final String[] SENSOR_MEASUREMENTS = {
            "",
            "pm.01.value",
            "pm.2.5.value",
            "pm.10.value",
            "pm.0.3.above",
            "pm.0.5.above",
            "pm.1.above",
            "pm.2.5.above",
            "pm.5.above",
            "pm.10.above",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "temperature.c",
            "humidity",
            "",
            "",
            "",
    };

    public static final String[] SENSOR_UNITS = {
            "",
            Unit.CONCENTRATION_UG_M3,
            Unit.CONCENTRATION_UG_M3,
            Unit.CONCENTRATION_UG_M3,
            Unit.CONCENTRATION_ABOVE,
            Unit.CONCENTRATION_ABOVE,
            Unit.CONCENTRATION_ABOVE,
            Unit.CONCENTRATION_ABOVE,
            Unit.CONCENTRATION_ABOVE,
            Unit.CONCENTRATION_ABOVE,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            Unit.TEMPERATURE_CELCIUS,
            Unit.PERCENTAGE,
            "",
            "",
            "",
    };


    public String getDeviceName() {
        return DeviceName;
    }

    public void setDeviceName(String deviceName) {
        DeviceName = deviceName;
    }

    public String getDeviceUUID() {
        return DeviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        DeviceUUID = deviceUUID;
    }

    private String DeviceName;
    private String DeviceUUID;

    private Date date, dateLocal;

    public String[] rawValues;

    public double getDouble(int value)
    {
        return Double.parseDouble(rawValues[value].trim());
    }

    public void setDouble(int value, double data)
    {
        rawValues[value] = String.valueOf(data);
    }

    public Date getDateLocal() {
        return dateLocal;
    }

    public void setDateLocal(Date dateLocal) {
        this.dateLocal = dateLocal;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getTempK()
    {
        return getDouble(SENSOR_TEMP) + 273.15f;
    }

    public String getGeohash()
    {
        String st = GeoHash.geoHashStringWithCharacterPrecision(getDouble(SENSOR_LATITUDE), getDouble(SENSOR_LONGITUDE), 10);
        //Log.i("GEOHASH", "Geohash is " + st);
        return st;
    }

    public SensorDataModel()
    {
        date = new Date();
        dateLocal = new Date();
    }


    public void fromPersistance(SensorPersistance s)
    {
        setDateLocal(new Date(s.dateLocal));
        setDate(new Date(s.date));
        rawValues = s.rawValues;
        setDeviceUUID(s.deviceUUID);
        setDeviceName(s.deviceName);
    }

    public boolean StringToModel(String input)
    {
        /* Check if this is a full string coming from the BLE device */
        if (input.contains("\n"))
        {
            final String[] splitted = input.split(";");
            rawValues = splitted;
            System.out.println(Arrays.toString(splitted));

            /* Update graph with PM1, PM2.5, PM10 */
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateformat = new SimpleDateFormat("y_M_d_H_m_s");
            Date d;
            try {
                date = dateformat.parse(splitted[SENSOR_DATE].trim());
            } catch (ParseException e) {
                e.printStackTrace();
                date = Calendar.getInstance().getTime();
            }

            dateLocal = Calendar.getInstance().getTime();

            return true;
        } else return false;
    }

    /* Parceling stuff */

    /* Data are stored in this order :
        - pm1
        - pm25
        - pm10
        - tempC
        - tempK
        - volt
        - date, stored as long for performance issues
     */
    public SensorDataModel(Parcel in){
        this.date = new Date(in.readLong());
        this.dateLocal = new Date(in.readLong());
        this.rawValues = (String[])in.readArray(String.class.getClassLoader());
        this.DeviceName = in.readString();
        this.DeviceUUID = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public SensorDataModel createFromParcel(Parcel in) {
            return new SensorDataModel(in);
        }

        public SensorDataModel[] newArray(int size) {
            return new SensorDataModel[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date.getTime());
        dest.writeLong(dateLocal.getTime());
        dest.writeArray(rawValues);
        dest.writeString(DeviceName);
        dest.writeString(DeviceUUID);
    }
}
