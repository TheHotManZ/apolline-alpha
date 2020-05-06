package com.apolline.sensorviewer;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class SensorDataModel implements Parcelable {
    /* Indexes of values */
    public static final int SENSOR_DATE = 0;
    public static final int SENSOR_PM_1 = 1;
    public static final int SENSOR_PM_2_5 = 2;
    public static final int SENSOR_PM_10 = 3;
    public static final int SENSOR_TEMP = 17;
    public static final int SENSOR_VOLT = 19;

    private double pm1;
    private double pm25;
    private double pm10;
    private double tempC;
    private double tempK;
    private double volt;
    private Date date;

    public SensorDataModel()
    {
        pm1 = pm25 = pm10 = tempC = tempK = volt = 0.0f;
        date = new Date();
    }

    public double getPm1() {
        return pm1;
    }

    public void setPm1(double pm1) {
        this.pm1 = pm1;
    }

    public double getPm25() {
        return pm25;
    }

    public void setPm25(double pm25) {
        this.pm25 = pm25;
    }

    public double getPm10() {
        return pm10;
    }

    public void setPm10(double pm10) {
        this.pm10 = pm10;
    }

    public double getTempC() {
        return tempC;
    }

    public void setTempC(double tempC) {
        this.tempC = tempC;
    }

    public double getTempK() {
        return tempK;
    }

    public void setTempK(double tempK) {
        this.tempK = tempK;
    }

    public double getVolt() {
        return volt;
    }

    public void setVolt(double volt) {
        this.volt = volt;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean StringToModel(String input)
    {
        /* Check if this is a full string coming from the BLE device */
        if (input.contains("\n"))
        {
            final String[] splitted = input.split(";");
            System.out.println(Arrays.toString(splitted));

            /* Update graph with PM1, PM2.5, PM10 */
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateformat = new SimpleDateFormat("y_M_d_H_m_s");
            Date d;
            try {
                d = dateformat.parse(splitted[SENSOR_DATE]);
            } catch (ParseException e) {
                e.printStackTrace();
                d = new Date();
            }

            /* Parse input values */
            Double pm1 = Double.parseDouble(splitted[SENSOR_PM_1].trim());
            Double pm25 = Double.parseDouble(splitted[SENSOR_PM_2_5].trim());
            Double pm10 = Double.parseDouble(splitted[SENSOR_PM_10].trim());
            Double bat = Double.parseDouble(splitted[SENSOR_VOLT].trim());
            Double tmp = Double.parseDouble(splitted[SENSOR_TEMP].trim());

            /* Output values */
            setPm1(pm1);
            setPm25(pm25);
            setPm10(pm10);
            setVolt(bat);
            setTempC(tmp);
            setTempK(tmp + 273.15f);

            return true;
        } else return false;
    }

    public String toCSVLine()
    {
        String res = "";
        res += date.toString() + ",";
        res += pm1 + ",";
        res += pm25 + ",";
        res += pm10 + ",";
        res += volt + ",";
        res += tempC + ",";
        res += tempK;
        res += '\n';
        return res;
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
        this.pm1 = in.readDouble();
        this.pm25 = in.readDouble();
        this.pm10 =  in.readDouble();
        this.tempC = in.readDouble();
        this.tempK = in.readDouble();
        this.volt = in.readDouble();
        this.date = new Date(in.readLong());
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
        dest.writeDouble(pm1);
        dest.writeDouble(pm25);
        dest.writeDouble(pm10);
        dest.writeDouble(tempC);
        dest.writeDouble(tempK);
        dest.writeDouble(volt);
        dest.writeLong(date.getTime());
    }
}
