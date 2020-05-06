package com.apolline.sensorviewer;

import java.util.HashMap;

public class SampleGattAttributes {
    private static HashMap<String,String> attributes = new HashMap<String,String>();
    public static String DATA_DUST_SENSOR = "49535343-1E4D-4BD9-BA61-23C647249616";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static void init() {
        // Sample Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    static String lookup(String uuid, String defaultName)  {
        String name = attributes.get(uuid);
        if (name == null) return defaultName;
        else return name;
    }
}
