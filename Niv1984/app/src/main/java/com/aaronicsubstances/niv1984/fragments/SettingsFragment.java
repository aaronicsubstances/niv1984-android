package com.aaronicsubstances.niv1984.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsFragment.class);

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(SharedPrefsManager.SHARED_PREF_NAME);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the listener whenever a key changes
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        LOGGER.debug("shared preferences edited with: {}", key);
        if (SharedPrefsManager.SHARED_PREF_KEY_NIGHT_MODE.equals(key)) {
            AppCompatDelegate.setDefaultNightMode(sharedPreferences.getBoolean(key, false) ?
                    AppCompatDelegate.MODE_NIGHT_YES :
                    AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
