package com.aaronicsubstances.niv1984.ui.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import androidx.preference.Preference
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppConstants

class DisplayBookPreference: DialogPreference, Preference.OnPreferenceChangeListener {

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

    var preferredSequence = ""
        set(value) {
            field = value
            persistString(value)
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        // Default value from attribute. Fallback value is set to empty string.
        return a.getString(index) ?: ""
    }

    override fun onSetInitialValue(
        defaultValue: Any?
    ) {
        // Read the value. Use the default value if it is not possible.
        var t = getPersistedString("")
        if (t.isEmpty() && defaultValue != null) {
            t = defaultValue as String
        }
        preferredSequence = t
        updateSummary(t)
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_display_book_list
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        updateSummary(if (newValue != null) newValue as String else "")
        return true
    }

    private fun updateSummary(t: String) {
        if (t == "") {
            summary = "Not set"
        }
        else {
            // use the topmost two for summary display
            val codes = t.splitToSequence(" ").take(2).toList()
            summary = codes.map {
                AppConstants.bibleVersions[it]?.description
            }.joinToString(", ")
            if (codes == AppConstants.DEFAULT_BIBLE_VERSIONS) {
                summary = "Default: $summary"
            }
        }
    }
}