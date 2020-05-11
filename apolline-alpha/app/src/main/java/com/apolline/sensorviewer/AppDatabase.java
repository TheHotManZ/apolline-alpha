package com.apolline.sensorviewer;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {SensorPersistance.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SensorDAO sensorDao();
}
