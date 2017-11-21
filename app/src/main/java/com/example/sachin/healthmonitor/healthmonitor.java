package com.example.sachin.healthmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.opengl.GLES20;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;
import org.json.JSONException;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import static java.lang.Thread.sleep;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;


public class healthmonitor extends AppCompatActivity {

    float[] values = {};
    String[] xValues = new String[]{"0", "1", "2", "3", "4", "5"};
    String[] yValues = new String[]{"10", "5", "0", "-5", "-10"};
    GraphView heartrategraphx;
    LinearLayout graphlayoutx;
    boolean stopped = false;
    boolean running = false;

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
    String activityLabel;
    boolean male;
    boolean female;
    boolean run;
    boolean walk;
    boolean jump;
    static boolean isRegistered = false;
    private View v;
    final float[] valuesx = new float[10];
    final float[] valuesy = new float[10];
    final float[] valuesz = new float[10];
    int i = 0;
    float[][] graphPoints = new float[50][3];

    private String[] classes = {"Walking", "Running", "Jumping"};
    private native int trainClassifierNative(String trainingFile, int kernelType,
                                             int cost, float gamma, int isProb, String modelFile);
    private native int doClassificationNative(float values[][], int indices[][],
                                              int isProb, String modelFile, int labels[], double probs[]);
    static {
        System.loadLibrary("signal");
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (i < 50) {
                graphPoints[i][0] = intent.getFloatExtra("xvalue", 0);
                graphPoints[i][1] = intent.getFloatExtra("yvalue", 0);
                graphPoints[i][2] = intent.getFloatExtra("zvalue", 0);
            }
            if (i == 50) {
                uploadDatatoDb(graphPoints);
                stopService(startSenseService);
                Toast.makeText(healthmonitor.this, "Data uploaded.", Toast.LENGTH_SHORT).show();
            }
            System.out.println(i);
            i++;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healthmonitor);
        // Create a graph view using GraphView.java given and add it to a layout.
        heartrategraphx = new GraphView(this, values, values, values, "Health Monitor", xValues, yValues, true);
        graphlayoutx = (LinearLayout) findViewById(R.id.heartrategraphlayout);
        graphlayoutx.addView(heartrategraphx);

        System.out.println("App create");
        filter = new IntentFilter("com.example.sachin.healthmonitor");
        dbFile = new File(this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2");
        if (!dbFile.exists() && !dbFile.isDirectory()) {
            dbFile.mkdir();
        }
        serverUrl = "http://10.218.110.136/CSE535Fall17Folder/UploadToServer.php";
        db = SQLiteDatabase.openOrCreateDatabase(dbFile + "/patientDb", null);
    }

    public void getEditTextData() {
        /*
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
         */
        walk = ((RadioButton)findViewById(R.id.walk)).isChecked();
        run = ((RadioButton)findViewById(R.id.run)).isChecked();
        jump = ((RadioButton)findViewById(R.id.jump)).isChecked();
        if (walk) {
            activityLabel = "walk";
        } else if (run) {
            activityLabel = "run";
        } else {
            activityLabel = "jump";
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

    public void onClickRadio(View V) {
        i = 0;
        getEditTextData();
    }

    public void onClickRunbutton(View V) throws InterruptedException {
        stopped = false;

        // Data entry is checked.
        //if (patientName.isEmpty() || patientAge.isEmpty() || patientID.isEmpty() || (male == false && female == false)) {
        if (activityLabel.isEmpty()) {
            Toast.makeText(this, "Enter all the required data!", Toast.LENGTH_SHORT).show();
            return;
        }
        getDatafromDb(valuesx, valuesy, valuesz);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (!stopped) {
                    getDatafromDb(valuesx, valuesy, valuesz);

                    float tmp = valuesx[0];
                    valuesx[valuesx.length - 1] = tmp;
                    tmp = valuesy[0];
                    valuesy[valuesy.length - 1] = tmp;
                    tmp = valuesy[0];
                    valuesy[valuesy.length - 1] = tmp;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("sthread ui");
                            heartrategraphx.setxValues(valuesx);
                            heartrategraphx.setyValues(valuesy);
                            heartrategraphx.setzValues(valuesz);
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
        heartrategraphx.setxValues(values);
        heartrategraphx.setyValues(values);
        heartrategraphx.setzValues(values);
        graphlayoutx.addView(heartrategraphx);
        findViewById(R.id.runbutton).setEnabled(true);
    }

    public void onClickCreateDbbutton(View V) throws InterruptedException {
        getEditTextData();
        // Data entry is checked.
        //if (patientName.isEmpty() || patientAge.isEmpty() || patientID.isEmpty() || (male == false && female == false)) {
        if (activityLabel.isEmpty()) {
            Toast.makeText(this, "Enter all the required data!", Toast.LENGTH_SHORT).show();
            return;
        }

        //int age = new Integer(patientAge);
        tableName = "activities";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    db.beginTransaction();
                    db.execSQL("create table if not exists " + tableName + " (timestamp timestamp, " +
                            "xvalue float, yvalue float, zvalue float, activity_label varchar(20));");
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    System.out.println(e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(healthmonitor.this, "Database created failed!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    db.endTransaction();
                }
            }
        }).start();

        startSenseService = new Intent(healthmonitor.this, acclerometerUpdate.class);
        startSenseService.putExtra("tableName", tableName);
        startService(startSenseService);
        Toast.makeText(this, "Table created!", Toast.LENGTH_SHORT).show();
    }

    public void uploadDatatoDb(float[][] values) {
        // Create a Database if already not created and insert the accelerometer data.
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            if (tableName != null) {
                for (int i = 0; i < 50; i++) {
                    db.execSQL("insert into " + tableName + "(timestamp, xvalue, yvalue, zvalue, activity_label) values " +
                            "(" + timestamp + ", " + values[i][0] + ", " + values[i][1] + ", " +
                            values[i][2] + ", '" + activityLabel + "' );");
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(this, "Database insert failed!!", Toast.LENGTH_SHORT).show();
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
            dbFile = new File(this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2_Extra");
            if (!dbFile.exists() && !dbFile.isDirectory()) {
                dbFile.mkdir();
            }
            File loc = new File(dbFile + "/patientDb");
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

    public void getDatafromDb(float[] valuesx, float[] valuesy, float[] valuesz) {
        getEditTextData();
        float[] timestamp = null;
        //int age = new Integer(patientAge);
        //tableName = patientName + "_" + patientID + "_" + age + "_" + patientSex;
        tableName = "activities";

        // Select the latest 10 seconds data from the database.
        try {
            String query = "select * from " + tableName + " ORDER BY timestamp DESC limit 10;";
            Cursor cursor = db.rawQuery(query, null);
            int i = 0;
            while (cursor.moveToNext()) {
                valuesx[i] = cursor.getFloat(cursor.getColumnIndex("xvalue"));
                valuesy[i] = cursor.getFloat(cursor.getColumnIndex("yvalue"));
                valuesz[i] = cursor.getFloat(cursor.getColumnIndex("zvalue"));
                i++;
            }
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(this, "Database read failed!!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public void onClickTrainbutton(View V) throws InterruptedException, JSONException {
        tableName = "activities";
        float valuex;
        float valuey;
        float valuez;
        String label;
        try {
            String query = "select * from " + tableName + ";";
            Cursor cursor = db.rawQuery(query, null);
            String trainFilePath = this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2/training_set";
            /*File trainSet = new File(trainFilePath);
            FileOutputStream fos = new FileOutputStream(trainSet);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            while (cursor.moveToNext()) {
                valuex = cursor.getFloat(cursor.getColumnIndex("xvalue"));
                valuey = cursor.getFloat(cursor.getColumnIndex("yvalue"));
                valuez = cursor.getFloat(cursor.getColumnIndex("zvalue"));
                label = cursor.getString(cursor.getColumnIndex("activity_label"));
                if (label.equals("walk")) {
                    bw.write("0 " + "1:" + valuex + " 2:" + valuey + " 3:" + valuez);
                    bw.newLine();
                } else if (label.equals("run")) {
                    bw.write("1 " + "1:" + valuex + " 2:" + valuey + " 3:" + valuez);
                    bw.newLine();
                } else {
                    bw.write("2 " + "1:" + valuex + " 2:" + valuey + " 3:" + valuez);
                    bw.newLine();
                }
            }
            bw.close();
            fos.close();*/

            // SVM training
            String trainedFilePath = this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2/trained_set";
            int kernelType = 2;
            int cost = 4;
            int isProb = 0;
            float gamma = 0.25f;
            if (trainClassifierNative(trainFilePath, kernelType, cost, gamma, isProb,
                    trainedFilePath) == -1) {
                finish();
            }
            Toast.makeText(this, "Training is done", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(this, "Database read failed!!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /**
     * classify generate labels for features.
     * Return:
     * 	-1: Error
     * 	0: Correct
     */
    public int callSVM(float values[][], int indices[][], int groundTruth[], int isProb, String modelFile,
                       int labels[], double probs[]) {
        // SVM type
        final int C_SVC = 0;
        final int NU_SVC = 1;
        final int ONE_CLASS_SVM = 2;
        final int EPSILON_SVR = 3;
        final int NU_SVR = 4;

        // For accuracy calculation
        int correct = 0;
        int total = 0;
        float error = 0;
        float sump = 0, sumt = 0, sumpp = 0, sumtt = 0, sumpt = 0;
        float MSE, SCC, accuracy;

        int num = values.length;
        int svm_type = C_SVC;
        if (num != indices.length)
            return -1;
        // If isProb is true, you need to pass in a real double array for probability array
        int r = doClassificationNative(values, indices, isProb, modelFile, labels, probs);

        // Calculate accuracy
        if (groundTruth != null) {
            if (groundTruth.length != indices.length) {
                return -1;
            }
            for (int i = 0; i < num; i++) {
                int predict_label = labels[i];
                int target_label = groundTruth[i];
                if(predict_label == target_label)
                    ++correct;
                error += (predict_label-target_label)*(predict_label-target_label);
                sump += predict_label;
                sumt += target_label;
                sumpp += predict_label*predict_label;
                sumtt += target_label*target_label;
                sumpt += predict_label*target_label;
                ++total;
            }

            if (svm_type==NU_SVR || svm_type==EPSILON_SVR)
            {
                MSE = error/total; // Mean square error
                SCC = ((total*sumpt-sump*sumt)*(total*sumpt-sump*sumt)) / ((total*sumpp-sump*sump)*(total*sumtt-sumt*sumt)); // Squared correlation coefficient
            }
            accuracy = (float)correct/total*100;
            System.out.println("Classification accuracy is " + accuracy);
            Toast.makeText(this, "Classification accuracy is " + accuracy, Toast.LENGTH_SHORT).show();
        }

        return r;
    }

    // SVM classification
    public void onClickClassifybutton(View V) {
        float[][] values = {
                {-1, 5, 8},
        };
        int[][] indices = {
                {1,2,3}
        };
        int[] groundTruth = {2};
        int[] labels = new int[1];
        double[] probs = new double[1];
        int isProb = 0; // Not probability prediction
        String modelFileLoc = this.getExternalFilesDir(null) + "/CSE535_ASSIGNMENT2/trained_set";

        if (callSVM(values, indices, groundTruth, isProb, modelFileLoc, labels, probs) != 0) {
            Toast.makeText(this, "Classification is incorrect", Toast.LENGTH_SHORT).show();
        }
        else {
            String m = "";
            for (int l : labels)
                m += classes[l] + ", ";
            System.out.println("Classification is done, the result is " + m);
            Toast.makeText(this, "Classification is done, the result is " + m, Toast.LENGTH_SHORT).show();
        }
    }

    public void draw3d() {
        ;
    }
}