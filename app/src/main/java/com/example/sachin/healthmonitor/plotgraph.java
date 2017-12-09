package com.example.sachin.healthmonitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Date;

public class plotgraph extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plotgraph);
        WebView webView = (WebView) findViewById(R.id.webview);
        final long[] start = new long[1];
        final long[] end = new long[1];
        webView.setWebViewClient(new WebViewClient(){
            public void onPageStarted(WebView view, String url) {
                start[0] = (new Date()).getTime();
            }

            public void onPageFinished(WebView view, String url) {
                end[0] = (new Date()).getTime();
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        webView.getSettings().setSupportZoom(true);
        webView.loadUrl("https://sachin-3d.herokuapp.com/training_set");
        float time = end[0] - start[0];
        Toast.makeText(this, "Load time:" + time, Toast.LENGTH_SHORT).show();
    }
}
