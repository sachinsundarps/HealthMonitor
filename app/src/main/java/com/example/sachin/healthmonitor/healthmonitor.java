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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
    File dbFile = null;

    String serverUrl = null;

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
        dbFile = new File(this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2");
        if (!dbFile.exists() && !dbFile.isDirectory()) {
            dbFile.mkdir();
        }
        db = SQLiteDatabase.openOrCreateDatabase(dbFile + "/patientDb", null);

        serverUrl = "http://10.218.110.136/CSE535Fall17Folder/UploadToServer.php";
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
                    int i = 0;
                    while(i < finalValuesx.length - 1) {
                        finalValuesx[i] = finalValuesx[i + 1];
                        i++;
                    }
                    db.beginTransaction();
                    String query = "select * from " + tableName + " ORDER BY timestamp DESC limit 1;";
                    Cursor cursor = db.rawQuery(query, null);
                    cursor.moveToFirst();
                    finalValuesx[i] = cursor.getFloat(cursor.getColumnIndex("xvalue"));
                    db.endTransaction();
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
        Toast.makeText(this, "Table created!", Toast.LENGTH_SHORT).show();
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

    public void onClickUploadDbbutton(View V) throws InterruptedException, JSONException {
        Toast.makeText(this, "Uploading database to server....", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uploadDB();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void uploadDB() throws IOException {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        int buffSize, bytesRead, available;
        byte[] buffer;
        int maxBuff = 1024 * 1024;
        int responseCode = 0;
        String filePath = this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2/patientDb";
        File file = new File(filePath);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            URL url = new URL(serverUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
            conn.setRequestProperty("uploaded_file", "patientDb");

            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes("--" + "*****" + "\r\n");
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                            + dbFile.getPath() + "/patientDb" + "\"\r\n");
            dos.writeBytes("\r\n");

            available = fis.available();
            buffSize = Math.min(available, maxBuff);
            buffer = new byte[buffSize];
            bytesRead = fis.read(buffer, 0, buffSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, buffSize);
                available = fis.available();
                buffSize = Math.min(available, maxBuff);
                bytesRead = fis.read(buffer, 0, buffSize);
            }

            dos.writeBytes("\r\n");
            dos.writeBytes("--*****--\r\n");
            responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();
            if(responseCode == 200){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(healthmonitor.this, "Database uploaded!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            fis.close();
            dos.flush();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(healthmonitor.this, "Db does not exist!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(healthmonitor.this, "URL does not exist!", Toast.LENGTH_SHORT).show();
                }
            });
            fis.close();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            fis.close();
            return;
        }

    }

    public void onClickDownloadDbbutton(View V) throws InterruptedException, JSONException {
        Toast.makeText(this, "Downloading database....", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadDB();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void downloadDB() throws IOException {

        // Referred http://www.coderzheaven.com/2012/04/29/download-file-android-device-remote-server-custom-progressbar-showing-progress/
        try {
            URL url = new URL("http://10.218.110.136/CSE535Fall17Folder/patientDb");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.connect();
            File loc = new File(this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2/patientDb");
            FileOutputStream fos = new FileOutputStream(loc);
            InputStream is = conn.getInputStream();
            int size = conn.getContentLength();
            int downloadedSize = 0;
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ( (bufferLength = is.read(buffer)) > 0 ) {
                fos.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(healthmonitor.this, "Database downloaded!", Toast.LENGTH_SHORT).show();
                }
            });
            is.close();
            fos.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(healthmonitor.this, "Error connecting to server!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

    }

}