package com.apolline.sensorviewer;

import android.app.Activity;
import android.os.Bundle;

/* This is a dummy activity used to resume the app from the notification displayed by the BLE service */
public class NotificationHandlerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack
        finish();
    }
}
