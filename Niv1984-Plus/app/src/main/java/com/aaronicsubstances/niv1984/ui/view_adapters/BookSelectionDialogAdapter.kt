package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppConstants

class BookSelectionDialogAdapter(
    private val bibleVersionCode: String,
    private val selectedBookNumber: Int,
    private val clickListenerFactory: LargeListEventListenerFactory
): FiniteListAdapter<Any, BookSelectionDialogAdapter.ViewHolder>(null) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.dialog_book_sel_item,
            parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return AppConstants.BIBLE_BOOK_COUNT
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookRadioView: RadioButton = itemView.findViewById(R.id.radioText)

        init {
            val clickListener = clickListenerFactory.create(this,
                View.OnClickListener::class.java,null)
            bookRadioView.setOnClickListener(clickListener)
        }

        fun bind(position: Int) {
            val bibleVersion = AppConstants.bibleVersions.getValue(bibleVersionCode)

            val description = bibleVersion.bookNames[position]
            bookRadioView.text = description

            bookRadioView.isChecked = selectedBookNumber == position + 1
        }
    }
}