package com.example.sachin.healthmonitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

/**
 * Created by Sachin on 09/25/2017.
 */

public class acclerometerUpdate extends Service implements SensorEventListener {
    private SensorManager accelManage;
    private Sensor senseAccel;
    private String tableName;
    int i = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        System.out.println("sensor changed");
        Sensor accelerometer = event.sensor;
        float[] values = new float[3];
        if (accelerometer.getType() == Sensor.TYPE_ACCELEROMETER) {
            i++;
            values[0] = event.values[0];
            values[1] = event.values[1];
            values[2] = event.values[2];
            Intent broadcast = new Intent();
            broadcast.setAction("com.example.sachin.healthmonitor");
            broadcast.putExtra("xvalue", values[0]);
            broadcast.putExtra("yvalue", values[1]);
            broadcast.putExtra("zvalue", values[2]);
            sendBroadcast(broadcast);
            if (i >= 127) {
                i = 0;
                accelManage.unregisterListener(this);
                accelManage.registerListener(this, senseAccel, 1000);
            }
        }
    }

    //Register the sensorManager to listen to accelerometer sensor
    @Override
    public void onCreate(){
        System.out.println("service create");
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelManage.registerListener(this, senseAccel, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        tableName = intent.getStringExtra("tableName");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
