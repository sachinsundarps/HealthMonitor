package com.example.sachin.healthmonitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;
import static java.lang.Thread.sleep;

public class healthmonitor extends AppCompatActivity {

    float[] values = {};
    String[] xValues = new String[]{"2700", "2750", "2800", "2850", "2900", "2950", "3000"};
    String[] yValues = new String[]{"2000", "1500", "1000", "500"};
    //String[] xValues = new String[]{"0", "5", "10", "15"};
    //String[] yValues = new String[]{"15", "10", "5", "0"};
    GraphView heartrategraph;
    LinearLayout graphlayout;
    boolean stopped = false;
    float[] graphPoints = new float[20];
    Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healthmonitor);
        // Create a graph view using GraphView.java given and add it to a layout.
        heartrategraph = new GraphView(this, values, "Health Monitor", xValues, yValues, true);
        graphlayout = (LinearLayout) findViewById(R.id.heartrategraphlayout);
        graphlayout.addView(heartrategraph);
    }

    public void onClickRunbutton(View V) throws InterruptedException {
        String patientID = ((EditText)findViewById(R.id.patientidText)).getText().toString();
        String patientAge = ((EditText)findViewById(R.id.ageText)).getText().toString();
        String patientName = ((EditText)findViewById(R.id.patientnameText)).getText().toString();
        boolean male = ((RadioButton)findViewById(R.id.maleradio)).isChecked();
        boolean female = ((RadioButton)findViewById(R.id.femaleradio)).isChecked();
        stopped = false;

        // Data entry is checked.
        if (patientName.isEmpty() || patientAge.isEmpty() || patientID.isEmpty() || (male == false && female == false)) {
            Toast.makeText(this, "Enter all the required data!", Toast.LENGTH_SHORT).show();
            return;
        }
        // The points to be plotted on graph.
        for(int i = 1; i < graphPoints.length; i++) {
            graphPoints[i] = (float)(20 * Math.random());
        }

        // For the graph to be running till stop button is pressed, graph has to be run on a separate thread.
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stopped) {
                    // This is to plot and get continuous graph. Putting the for loop in line 46 here will
                    // paint the graph with new data each time and graph won't look continuous.
                    graphPoints[graphPoints.length-1] = graphPoints[0];
                    for (int j = 0; j < graphPoints.length - 1; j++) {
                        graphPoints[j] = graphPoints[j + 1];
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            heartrategraph.setValues(graphPoints);
                            graphlayout.removeView(heartrategraph);
                            graphlayout.addView(heartrategraph);
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
    }

    public void onClickStopbutton(View V) throws InterruptedException {
        stopped = true;
        // Graph is cleared.
        graphlayout.removeView(heartrategraph);
        heartrategraph.setValues(values);
        graphlayout.addView(heartrategraph);
    }
}
