package com.aaronicsubstances.niv1984.activities;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.Utils;
import com.aaronicsubstances.niv1984.etc.VersionCheckResponse;

import java.util.Date;

public class AboutActivity extends BaseActivity {
    private String mCurrentVersion;
    private TextView mCurrentVersionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentVersionView = findViewById(R.id.current_version);
        mCurrentVersion = Utils.getAppVersion(this);
        mCurrentVersionView.setText(getString(R.string.current_version, mCurrentVersion));

        TextView copyrightView = findViewById(R.id.copy_right);
        String appCompany = getString(R.string.app_company);
        String currentYear = Utils.formatTimeStamp(new Date(), "yyyy");
        String copyrightText = getString(R.string.copyright_text, currentYear, appCompany);
        copyrightView.setText(copyrightText);

        TextView t2 = (TextView) findViewById(R.id.credits);
        t2.setMovementMethod(LinkMovementMethod.getInstance());

        updateLatestVersionView();
    }

    private void updateLatestVersionView() {
        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(this);
        VersionCheckResponse latestVersionCheck = sharedPrefsManager.getCachedLatestVersionInfo();
        int currentVersionCode = Utils.getAppVersionCode(this);
        if (latestVersionCheck.getVersionName() != null && currentVersionCode < latestVersionCheck.getVersionCode()) {
            mCurrentVersionView.setText(getString(R.string.current_version, mCurrentVersion) + ' ' +
                getString(R.string.latest_version, latestVersionCheck.getVersionName()));
        }
    }
}
