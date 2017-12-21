package com.aaronicsubstances.niv1984.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class AboutActivity extends BaseActivity {
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

        updateLatestVersionView();
    }

    private void updateLatestVersionView() {
        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(this);
        String[] temp = new String[3];
        sharedPrefsManager.getCachedLatestVersion(temp);
        String latestVersion = temp[0];
        if (latestVersion != null) {
            mCurrentVersionView.setText(getString(R.string.current_version, mCurrentVersion) + ' ' +
                getString(R.string.latest_version, latestVersion));
        } else {
            LOGGER.warn("Latest version not known.");
        }
    }
}
