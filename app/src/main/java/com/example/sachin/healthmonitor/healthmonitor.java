package com.example.sachin.healthmonitor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.sip.SipAudioCall;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;

import static android.database.sqlite.SQLiteDatabase.OPEN_READWRITE;
import static java.lang.Thread.sleep;

public class healthmonitor extends AppCompatActivity {

    float[] values = {};
    String[] xValues = new String[]{"0", "1", "2", "3", "4", "5"};
    String[] yValues = new String[]{"10", "5", "0", "-5", "-10"};
    GraphView heartrategraphx;
    GraphView heartrategraphy;
    GraphView heartrategraphz;
    LinearLayout graphlayoutx;
    LinearLayout graphlayouty;
    LinearLayout graphlayoutz;
    boolean stopped = false;

    Thread thread;
    Intent startSenseService = null;
    IntentFilter filter = null;
    SQLiteDatabase db = null;

    String patientID;
    String patientAge;
    String patientName;
    String patientSex;
    String tableName = null;
    boolean male;
    boolean female;
    static boolean isRegistered = false;
    private View v;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float[] graphPoints = new float[3];
            graphPoints[0] = intent.getFloatExtra("xvalue", 0);
            graphPoints[1] = intent.getFloatExtra("yvalue", 0);
            graphPoints[2] = intent.getFloatExtra("zvalue", 0);
            uploadDatatoDb(graphPoints);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healthmonitor);
        // Create a graph view using GraphView.java given and add it to a layout.
        heartrategraphx = new GraphView(this, values, "Health Monitor", xValues, yValues, true);
        graphlayoutx = (LinearLayout) findViewById(R.id.heartrategraphlayout);
        graphlayoutx.addView(heartrategraphx);

        filter = new IntentFilter("com.example.sachin.healthmonitor");
        File dbFile = new File(this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2");
        if (!dbFile.exists() && !dbFile.isDirectory()) {
            dbFile.mkdir();
        }
        db = SQLiteDatabase.openOrCreateDatabase(dbFile + "/patientDb", null);
    }

    public void getEditTextData() {
        patientID = ((EditText)findViewById(R.id.patientidText)).getText().toString();
        patientAge = ((EditText)findViewById(R.id.ageText)).getText().toString();
        patientName = ((EditText)findViewById(R.id.patientnameText)).getText().toString();
        male = ((RadioButton)findViewById(R.id.maleradio)).isChecked();
        female = ((RadioButton)findViewById(R.id.femaleradio)).isChecked();
        if (male) {
            patientSex = "M";
        } else if (female) {
            patientSex = "F";
        }
    }

    protected void onResume() {
        if (!isRegistered) {
            this.registerReceiver(receiver, filter);
            isRegistered = true;
        }
        super.onResume();
    }

    protected void onPause() {
        if (isRegistered) {
            unregisterReceiver(receiver);
            isRegistered = false;
        }
        super.onPause();
    }

    public void onClickRunbutton(View V) throws InterruptedException {
        getEditTextData();
        float[] timestamp = null;
        float[] valuesx = null;
        float[] valuesy = null;
        float[] valuesz = null;
        stopped = false;

        // Data entry is checked.
        if (patientName.isEmpty() || patientAge.isEmpty() || patientID.isEmpty() || (male == false && female == false)) {
            Toast.makeText(this, "Enter all the required data!", Toast.LENGTH_SHORT).show();
            return;
        }
        int age = new Integer(patientAge);
        tableName = patientName + "_" + patientID + "_" + age + "_" + patientSex;

        // Select the latest 10 seconds data from the database.
        try {
            db.beginTransaction();
            String query = "select * from " + tableName + " ORDER BY timestamp DESC limit 10;";
            Cursor cursor = db.rawQuery(query, null);

            timestamp = new float[10];
            valuesx = new float[10];
            valuesy = new float[10];
            valuesz = new float[10];
            int i = 0;
            while (cursor.moveToNext()) {
                timestamp[i] = cursor.getFloat(cursor.getColumnIndex("timestamp"));
                valuesx[i] = cursor.getFloat(cursor.getColumnIndex("xvalue"));
                valuesy[i] = cursor.getFloat(cursor.getColumnIndex("yvalue"));
                System.out.println(valuesx[i]);
                valuesz[i] = cursor.getFloat(cursor.getColumnIndex("zvalue"));
                i++;
            }
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(this, "Database read failed!!", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }

        // For the graph to be running till stop button is pressed, graph has to be run on a separate thread.
        final float[] finalValuesx = valuesx;
        final float[] finalValuesy = valuesy;
        final float[] finalValuesz = valuesz;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stopped) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            heartrategraphx.setValues(finalValuesx);
                            graphlayoutx.removeView(heartrategraphx);
                            graphlayoutx.addView(heartrategraphx);
                        }
                    });
                    try {
                        sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        findViewById(R.id.runbutton).setEnabled(false);
    }

    public void onClickStopbutton(View V) throws InterruptedException {
        stopped = true;
        // Graph is cleared.
        graphlayoutx.removeView(heartrategraphx);
        heartrategraphx.setValues(values);
        graphlayoutx.addView(heartrategraphx);
        findViewById(R.id.runbutton).setEnabled(true);
    }

    public void onClickCreateDbbutton(View V) throws InterruptedException {
        getEditTextData();
        // Data entry is checked.
        if (patientName.isEmpty() || patientAge.isEmpty() || patientID.isEmpty() || (male == false && female == false)) {
            Toast.makeText(this, "Enter all the required data!", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = new Integer(patientAge);
        tableName = patientName + "_" + patientID + "_" + age + "_" + patientSex;
        try {
            db.beginTransaction();
            db.execSQL("create table if not exists " + tableName + " (timestamp timestamp, " +
                    "xvalue float, yvalue float, zvalue float);");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(this, "Database created failed!!", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }

        startSenseService = new Intent(healthmonitor.this, acclerometerUpdate.class);
        startSenseService.putExtra("tableName", tableName);
        startService(startSenseService);
        System.out.println("Service started");
    }

    public void uploadDatatoDb(float[] values) {

        // Create a Database if already not created and insert the accelerometer data.
        try {
            db.beginTransaction();
            String timestamp = String.valueOf(System.currentTimeMillis());
            if (tableName != null) {
                db.execSQL("insert into " + tableName + "(timestamp, xvalue, yvalue, zvalue) values " +
                        "(" + timestamp + ", " + values[0] + ", " + values[1] + ", " + values[2] + " );");
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(this, "Database insert failed!!", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }
}
