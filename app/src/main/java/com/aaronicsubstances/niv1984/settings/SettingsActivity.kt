package com.aaronicsubstances.niv1984.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceFragmentCompat
import com.aaronicsubstances.niv1984.R
import androidx.preference.Preference


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            // Try if the preference is one of our custom Preferences
            var dialogFragment: DialogFragment? = null
            if (preference is TimePreference) {
                // Create a new instance of TimePreferenceDialogFragment with the key of the related
                // Preference
                dialogFragment = TimePreferenceDialogFragmentCompat
                    .newInstance(preference.getKey())
            }
            else if (preference is DisplayBookPreference) {
                dialogFragment = DisplayBookPreferenceDialogFragment.newInstance(preference.key)
            }

            // If it was one of our custom Preferences, show its dialog
            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(
                    this.parentFragmentManager,
                    "androidx.preference" + ".PreferenceFragment.DIALOG"
                )
            }
            // Could not be handled here. Try with the super method.
            else {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }
}