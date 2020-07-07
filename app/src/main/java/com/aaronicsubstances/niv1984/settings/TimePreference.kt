package com.aaronicsubstances.niv1984.settings

import android.content.Context
import androidx.preference.DialogPreference
import android.util.AttributeSet
import android.content.res.TypedArray
import android.util.Log
import androidx.annotation.LayoutRes
import androidx.preference.Preference
import com.aaronicsubstances.niv1984.R


class TimePreference: DialogPreference, Preference.OnPreferenceChangeListener {
    constructor(context: Context) :
            this(context, null) {
    }

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, R.attr.preferenceStyle) {
    }

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int
    ) :
            super(context, attrs, defStyleAttr) {
        setOnPreferenceChangeListener(this)
    }

    var time: Int = 0
        set(value) {
            field = value
            persistInt(value)
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        // Default value from attribute. Fallback value is set to -1.
        return a.getInt(index, -1)
    }

    override fun onSetInitialValue(
        //restorePersistedValue: Boolean,
        defaultValue: Any?
    ) {
        // Read the value. Use the default value if it is not possible.
        var t = getPersistedInt(-1)
        if (t == -1 && defaultValue != null) {
            t = defaultValue as Int
        }
        time = t
        updateSummary(t)
    }

    @LayoutRes
    var dialogLayoutResId = R.layout.pref_dialog_time

    override fun getDialogLayoutResource(): Int {
        return dialogLayoutResId
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        updateSummary(if (newValue != null) {
                newValue as Int
            }
            else {
            -1
        })
        return true
    }

    private fun updateSummary(t: Int) {
        if (t == -1) {
            summary = "Not set"
        }
        else {
            summary = "${t / 60}:${t % 60}"
        }
    }
}
