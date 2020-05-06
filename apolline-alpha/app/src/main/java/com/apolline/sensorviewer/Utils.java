package com.apolline.sensorviewer;

import android.bluetooth.BluetoothDevice;

class Utils {
    private static Boolean addressRegistered = false;
    private static String MAC = "";
    private static BluetoothDevice selectedDevice;

     static void registerAddress(String mac) {
        if(addressRegistered)
            return;

        addressRegistered = true;
        MAC = mac;
    }

    static void unregisterAddress() {
        if(!addressRegistered)
            return;

        addressRegistered = false;
        MAC = "";
    }

    static void setSelectedDevice(BluetoothDevice dev)
    {
        selectedDevice = dev;
    }

    static BluetoothDevice getSelectedDevice()
    {
        return selectedDevice;
    }

    static boolean hasRegistered() {
        return addressRegistered;
    }
}
