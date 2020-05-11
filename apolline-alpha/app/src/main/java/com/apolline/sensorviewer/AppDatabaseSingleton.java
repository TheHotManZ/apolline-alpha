package com.apolline.sensorviewer;

import android.content.Context;

import androidx.room.Room;

public class AppDatabaseSingleton {
    private AppDatabaseSingleton()
    {}

    private static AppDatabase INSTANCE = null;

    public static AppDatabase getInstance(Context ctx)
    {
        if (INSTANCE == null)
        {
            INSTANCE = Room.databaseBuilder(ctx,
                AppDatabase.class, "database-name").build();
        }
        return INSTANCE;
    }
}


