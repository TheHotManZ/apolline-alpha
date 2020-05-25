package com.apolline.sensorviewer;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class DAOStringArrayConverter {
    @TypeConverter
    public static String[] fromString(String value) {
        Type listType = new TypeToken<String[]>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromStringArray(String[] list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }
}
