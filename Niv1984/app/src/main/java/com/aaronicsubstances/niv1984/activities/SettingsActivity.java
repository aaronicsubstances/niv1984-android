package com.aaronicsubstances.niv1984.activities;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.fragments.SettingsFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

}