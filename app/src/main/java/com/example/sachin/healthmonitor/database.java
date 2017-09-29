package com.example.sachin.healthmonitor;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Sachin on 09/24/2017.
 */

public class database {

    public void createTable(Activity act, String tableName) {
        SQLiteDatabase db = null;
        // Create a Database if already not created.
        try {
            File dbFile = new File(Environment.getExternalStorageDirectory() + "/CSE535_ASSIGNMENT2");
            if (!dbFile.exists() && !dbFile.isDirectory()) {
                dbFile.mkdir();
            }
            db = SQLiteDatabase.openOrCreateDatabase(dbFile + "/patientDb", null);
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(act, "Database open/create failed!!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a table for the patient in database
        try {
            db.beginTransaction();

        } catch (Exception e) {
            Toast.makeText(act, "Table creation failed!!", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

}
