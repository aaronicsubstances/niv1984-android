package com.aaronicsubstances.niv1984.ui.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.get
import androidx.preference.PreferenceDialogFragmentCompat
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class DisplayBookPreferenceDialogFragment: PreferenceDialogFragmentCompat(), View.OnClickListener {

    companion object {
        fun newInstance(key: String): DisplayBookPreferenceDialogFragment {
            val fragment = DisplayBookPreferenceDialogFragment()
            val b = Bundle()
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    private lateinit var mRadioGroup: RadioGroup
    private lateinit var moveUpBtn: Button
    private lateinit var moveDownBtn: Button
    private lateinit var resetToDefault: Button

    private var selectedRadioIndex = 0

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mRadioGroup = view.findViewById(R.id.radioGroup)

        // Get the top books from the related Preference
        var topBooks = listOf<String>()
        val preference = preference
        if (preference is DisplayBookPreference) {
            topBooks = preference.preferredSequence.splitToSequence(" ").filter{
                it.isNotEmpty()
            }.toList()
        }
        if (topBooks.isEmpty()) {
            topBooks = AppConstants.DEFAULT_BIBLE_VERSIONS
        }

        val allBooks = AppUtils.getAllBooks(topBooks)

        // dynamically add radio buttons
        for (i in allBooks.indices) {
            val r = RadioButton(context)
            r.tag = Pair(i, allBooks[i])
            r.text = AppConstants.bibleVersions.getValue(allBooks[i]).description
            r.id = View.generateViewId()
            val rprms = RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            mRadioGroup.addView(r, rprms)
            r.setOnClickListener(this)
        }

        moveUpBtn = view.findViewById(R.id.moveUp)
        moveUpBtn.setOnClickListener { processMoveUpAction() }
        moveDownBtn = view.findViewById(R.id.moveDown)
        moveDownBtn.setOnClickListener { processMoveDownAction() }
        resetToDefault = view.findViewById(R.id.reset)
        resetToDefault.setOnClickListener { resetToDefault() }

        // select/check first.
        (mRadioGroup[0] as RadioButton).isChecked = true
        selectedRadioIndex = 0
        moveUpBtn.isEnabled = false
    }

    override fun onClick(v: View?) {
        val radio = v as RadioButton
        if (radio.isChecked) {
            val vTag = radio.tag as Pair<Int, String>
            setButtonStates(vTag.first)
        }
    }

    private fun setButtonStates(selectedIndex: Int) {
        selectedRadioIndex = selectedIndex
        // enable/disable buttons according to selection.
        moveUpBtn.isEnabled = selectedRadioIndex > 0;
        moveDownBtn.isEnabled = selectedRadioIndex < mRadioGroup.childCount - 1;
    }

    private fun processMoveUpAction() {
        swapTextsAndTags(selectedRadioIndex, selectedRadioIndex - 1)
        setButtonStates(selectedRadioIndex - 1)
        (mRadioGroup[selectedRadioIndex] as RadioButton).isChecked = true
    }

    private fun processMoveDownAction() {
        swapTextsAndTags(selectedRadioIndex, selectedRadioIndex + 1)
        setButtonStates(selectedRadioIndex + 1)
        (mRadioGroup[selectedRadioIndex] as RadioButton).isChecked = true
    }

    private fun swapTextsAndTags(idx1: Int, idx2: Int) {
        val radio1 = mRadioGroup[idx1] as RadioButton
        val radio2 = mRadioGroup[idx2] as RadioButton
        val tempText = radio1.text
        val tempTag = (radio1.tag as Pair<Int, String>).second
        radio1.text = radio2.text
        radio1.tag = Pair(idx1, (radio2.tag as Pair<Int, String>).second)
        radio2.text = tempText
        radio2.tag = Pair(idx2, tempTag)
    }

    private fun resetToDefault() {
        dialog?.dismiss()
        val preferredSequence = AppConstants.DEFAULT_BIBLE_VERSIONS.joinToString(" ")
        val preference = preference
        if (preference is DisplayBookPreference) {
            // This allows the client to ignore the user value.
            if (preference.callChangeListener(preferredSequence)) {
                // Save the value
                preference.preferredSequence = preferredSequence
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (!positiveResult) {
            return
        }

        val topBooks = listOf(getBibleVersionCode(mRadioGroup[0]),
            getBibleVersionCode(mRadioGroup[1]))
        val preferredSequence = topBooks.joinToString(" ")
        val preference = preference
        if (preference is DisplayBookPreference) {
            // This allows the client to ignore the user value.
            if (preference.callChangeListener(preferredSequence)) {
                // Save the value
                preference.preferredSequence = preferredSequence
            }
        }
    }

    private fun getBibleVersionCode(v: View): String {
        return (v.tag as Pair<Int, String>).second
    }
}