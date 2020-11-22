package com.aaronicsubstances.niv1984.ui.settings

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.LayoutRes
import androidx.preference.DialogPreference
import com.aaronicsubstances.niv1984.R

class VersionCheckPreference: DialogPreference {
    constructor(context: Context) :
            this(context, null) {
    }

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, R.attr.preferenceStyle)

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int
    ) :
            super(context, attrs, defStyleAttr)

    @LayoutRes
    var dialogLayoutResId = R.layout.pref_version_check

    override fun getDialogLayoutResource(): Int {
        return dialogLayoutResId
    }
}