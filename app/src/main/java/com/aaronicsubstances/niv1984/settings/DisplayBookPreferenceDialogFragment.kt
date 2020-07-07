package com.aaronicsubstances.niv1984.settings

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
import com.aaronicsubstances.niv1984.utils.KjvBibleVersion
import com.aaronicsubstances.niv1984.utils.NivBibleVersion

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

    private var selectedRadioIndex = -1

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mRadioGroup = view.findViewById(R.id.radioGroup)

        val allBooks = ArrayList(AppConstants.bibleVersions.keys)

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

        // Ensure top books appear on top.
        for (i in topBooks.indices) {
            // find ith top book in allBooks and swap it with
            // ith position in allBooks
            val idx = allBooks.indexOf(topBooks[i])
            assert(idx != -1) {
                "Could not find book ${topBooks[i]}"
            }
            val temp = allBooks[i]
            allBooks[i] = topBooks[i]
            allBooks[idx] = temp
        }

        // dynamically add radio buttons
        for (i in allBooks.indices) {
            val r = RadioButton(context)
            r.tag = Pair(i, allBooks[i])
            r.text = AppConstants.bibleVersions[allBooks[i]]?.description
            r.id = View.generateViewId()
            val rprms= RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            mRadioGroup.addView(r, rprms)
            r.setOnClickListener(this)
        }

        moveUpBtn = view.findViewById(R.id.moveUp)
        moveUpBtn.setOnClickListener { processMoveUpAction() }
        moveDownBtn = view.findViewById(R.id.moveDown)
        moveDownBtn.setOnClickListener { processMoveDownAction() }

        moveUpBtn.isEnabled = false
        moveDownBtn.isEnabled = false
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

    override fun onDialogClosed(positiveResult: Boolean) {
        if (!positiveResult) {
            return
        }

        val topBooks = mutableListOf<String>()
        if (mRadioGroup.childCount > 0) {
            topBooks.add(getBibleVersionCode(mRadioGroup[0]))
        }
        if (mRadioGroup.childCount > 1) {
            topBooks.add(getBibleVersionCode(mRadioGroup[1]))
        }
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