package com.apolline.sensorviewer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SensorDAO {
    @Query("SELECT * FROM sensor_values")
    List<SensorPersistance> getAll();

    @Query("SELECT * FROM sensor_values WHERE date >= (:gpsDate)")
    List<SensorPersistance> loadAllFromDate(long gpsDate);

    @Query("SELECT * FROM sensor_values WHERE date LIKE :gpsDate LIMIT 1")
    SensorPersistance findByDate(long gpsDate);

    @Query("SELECT * FROM sensor_values WHERE dateLocal >= (:localDate)")
    List<SensorPersistance> loadAllFromLocalDate(long localDate);

    @Insert
    void insert(SensorPersistance value);

    @Insert
    void insertAll(SensorPersistance... values);

    @Delete
    void delete(SensorPersistance values);
}
