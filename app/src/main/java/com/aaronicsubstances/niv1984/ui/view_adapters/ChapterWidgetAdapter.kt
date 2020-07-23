package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewClickListener
import com.aaronicsubstances.niv1984.R

class ChapterWidgetAdapter(private val chapterCount: Int,
                           private val clickListenerFactory: LargeListViewClickListener.Factory<Int>):
        RecyclerView.Adapter<ChapterWidgetAdapter.ViewHolder>() {

    var selectedIndex: Int = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int {
        return chapterCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.book_load_chapter_widget_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView as TextView
        private val defaultTextColour = textView.textColors

        init {
            itemView.setOnClickListener(clickListenerFactory.create(this))
        }

        fun bind(position: Int) {
            textView.text = "${position + 1}"
            if (position == selectedIndex) {
                textView.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorAccent))
            }
            else {
                textView.setTextColor(defaultTextColour)
            }
        }
    }
}