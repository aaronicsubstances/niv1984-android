package com.aaronicsubstances.niv1984;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class AboutActivity extends AppCompatActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(AboutActivity.class);

    private String mCurrentVersion;
    private TextView mCurrentVersionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentVersionView = (TextView)findViewById(R.id.current_version);
        mCurrentVersion = Utils.getAppVersion(this);
        mCurrentVersionView.setText(getString(R.string.current_version, mCurrentVersion));

        TextView copyrightView = (TextView)findViewById(R.id.copy_right);
        String appCompany = getString(R.string.app_company);
        String currentYear = Utils.formatTimeStamp(new Date(), "yyyy");
        String copyrightText = getString(R.string.copyright_text, currentYear, appCompany);
        copyrightView.setText(copyrightText);

        /*TextView t2 = (TextView)findViewById(R.id.external_link);
        String appSite = getString(R.string.app_site);
        t2.setText(Html.fromHtml(String.format("<a href='http://%1$s'>%1$s</a>", appSite)));
        t2.setMovementMethod(LinkMovementMethod.getInstance());*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForLatestVersion();
    }

    private void updateLatestVersionView() {
        String latestVersion = Utils.getCachedLatestVersion(this);
        if (!mCurrentVersion.equals(latestVersion)) {
            if (latestVersion != null) {
                mCurrentVersionView.setText(getString(R.string.current_version, mCurrentVersion) + ' ' +
                    getString(R.string.latest_version, latestVersion));
            } else {
                LOGGER.warn("Latest version not known.");
            }
        }
    }

    private void checkForLatestVersion() {
        new AsyncTask() {
            @Override
            protected void onPostExecute(Object o) {
                if (isFinishing()) return;
                if (o instanceof Exception) {
                    LOGGER.error("Error occurred while retrieving latest version.", (Exception)o);
                }
                updateLatestVersionView();
            }

            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    String uid = Utils.getUserUid(AboutActivity.this);
                    String currentVersion = Utils.getAppVersion(AboutActivity.this);
                    String url = Utils.getApiUrl(uid, Utils.API_CURRENT_VERSION_PATH +
                            "?v=%s", currentVersion);
                    LOGGER.debug("Checking for latest version at {}", url);
                    String versionCheckResponse = Utils.httpGet(url);
                    LOGGER.debug("Latest version check returned: {}", versionCheckResponse);
                    if (!versionCheckResponse.matches("^\\*?(\\w|\\.)+$")) {
                        throw new RuntimeException("Unexpected response from version check: " +
                            versionCheckResponse);
                    }
                    boolean forceUpdate = false;
                    if (versionCheckResponse.startsWith("*")) {
                        versionCheckResponse = versionCheckResponse.substring(1);
                        forceUpdate = true;
                    }
                    Utils.cacheLatestVersion(AboutActivity.this, versionCheckResponse, forceUpdate);
                    return null;
                }
                catch (Exception ex) {
                    return ex;
                }
            }
        }.execute();
    }
}
