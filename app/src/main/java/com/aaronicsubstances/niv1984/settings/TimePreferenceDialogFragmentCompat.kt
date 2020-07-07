package com.aaronicsubstances.niv1984.settings

import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import androidx.preference.PreferenceDialogFragmentCompat
import android.text.format.DateFormat
import com.aaronicsubstances.niv1984.R

class TimePreferenceDialogFragmentCompat: PreferenceDialogFragmentCompat() {
    companion object {
        fun newInstance(key: String): TimePreferenceDialogFragmentCompat {
            val fragment = TimePreferenceDialogFragmentCompat()
            val b = Bundle()
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    private lateinit var mTimePicker: TimePicker

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        mTimePicker = view.findViewById(R.id.edit)

        // Exception when there is no TimePicker
        checkNotNull(mTimePicker) { "Dialog view must contain" + " a TimePicker with id 'edit'" }

        // Get the time from the related Preference
        var minutesAfterMidnight: Int? = null
        val preference = preference
        if (preference is TimePreference) {
            minutesAfterMidnight = preference.time
        }

        // Set the time to the TimePicker
        if (minutesAfterMidnight != null) {
            val hours = minutesAfterMidnight / 60
            val minutes = minutesAfterMidnight % 60
            val is24hour = DateFormat.is24HourFormat(context)

            mTimePicker.setIs24HourView(is24hour)
            mTimePicker.currentHour = hours
            mTimePicker.currentMinute = minutes
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            // generate value to save
            val hours = mTimePicker.currentHour
            val minutes = mTimePicker.currentMinute
            val minutesAfterMidnight = hours * 60 + minutes

            // Get the related Preference and save the value
            val preference = preference
            if (preference is TimePreference) {
                // This allows the client to ignore the user value.
                if (preference.callChangeListener(minutesAfterMidnight)) {
                    // Save the value
                    preference.time = minutesAfterMidnight
                }
            }
        }
    }
}