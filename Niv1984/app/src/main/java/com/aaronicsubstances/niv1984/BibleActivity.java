package com.aaronicsubstances.niv1984;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibleActivity extends AppCompatActivity {
    private static final String JS_INTERFACE_NAME = "biblei";

    private static final Logger LOGGER = LoggerFactory.getLogger(BibleActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView browser = (WebView)findViewById(R.id.browser);

        WebSettings webSettings = browser.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        configureFileAccess(webSettings);

        browser.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                LOGGER.error("{}: {} (at {}:{})",
                        consoleMessage.messageLevel(), consoleMessage.message(),
                        consoleMessage.sourceId(), consoleMessage.lineNumber());
                return true;
            }
        });

        //browser.addJavascriptInterface(this, JS_INTERFACE_NAME);

        Uri entryPt = new Uri.Builder().scheme("file").authority("")
                .appendPath("android_asset").appendPath("index.html")
                .appendQueryParameter("e", getIntent().getData().toString())
                .build();
        browser.loadUrl(entryPt.toString());

        LOGGER.info("Launched browser at {}.", entryPt);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void configureFileAccess(WebSettings webSettings) {
        webSettings.setAllowFileAccessFromFileURLs(true);
    }
}
