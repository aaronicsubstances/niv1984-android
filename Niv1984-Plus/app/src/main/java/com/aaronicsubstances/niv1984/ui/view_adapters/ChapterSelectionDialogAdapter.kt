package com.aaronicsubstances.niv1984.ui.view_adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aaronicsubstances.niv1984.R

class ChapterSelectionDialogAdapter(
    private val totalCount: Int,
    private val selectedChapterNumber: Int
): BaseAdapter() {

    override fun getItem(position: Int) = position + 1

    override fun getItemId(position: Int) = 0L

    override fun getCount() = totalCount

    private var defaultTextViewColour: ColorStateList? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewToUse = if (convertView != null) convertView as TextView else {
            val newTf = (parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                R.layout.dialog_chapter_sel_item, parent, false) as TextView
            defaultTextViewColour = newTf.textColors
            newTf
        }
        viewToUse.text = "${getItem(position)}"
        viewToUse.setTextColor(defaultTextViewColour)
        if (position + 1 == selectedChapterNumber) {
            viewToUse.setTextColor(ContextCompat.getColor(parent.context, R.color.colorAccent))
        }
        return viewToUse
    }
}