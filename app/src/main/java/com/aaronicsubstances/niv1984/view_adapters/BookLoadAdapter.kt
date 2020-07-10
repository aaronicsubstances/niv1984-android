package com.aaronicsubstances.niv1984.view_adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookLoadAdapter: FiniteListAdapter<BookDisplayItem, RecyclerView.ViewHolder>(null) {
    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            BookDisplayItemViewType.DIVIDER.ordinal -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_divider, parent, false)
                return DividerViewHolder(itemView)
            }
            BookDisplayItemViewType.TITLE.ordinal -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_title, parent, false)
                return TitleViewHolder(itemView)
            }
            else -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_verse, parent, false)
                return VerseViewHolder(itemView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.viewType) {
            BookDisplayItemViewType.DIVIDER -> {
                // do nothing
            }
            BookDisplayItemViewType.TITLE -> {
                (holder as TitleViewHolder).bind(item)
            }
            else -> {
                (holder as VerseViewHolder).bind(item)
            }
        }
    }

    class TitleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        init {
            //textView.textSize = AppUtils.spToPx(24f)
            textView.setTextColor(Color.parseColor("#000000"))
            //textView.setTextColor(Color.parseColor("white"))
        }

        fun bind(item: BookDisplayItem) {
            textView.text = item.text
        }
    }

    class DividerViewHolder(item: View): RecyclerView.ViewHolder(item)

    class VerseViewHolder(item: View): RecyclerView.ViewHolder(item) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        init {
            //textView.textSize = AppUtils.spToPx(14f)
            textView.setTextColor(Color.parseColor("#000000"))
            //textView.setTextColor(Color.parseColor("white"))
        }

        fun bind(item: BookDisplayItem) {
            // cache html parse result
            if (item.html == null) {
                item.html = AppUtils.parseHtml(item.text)
                item.text = "" // reduce memory load
            }
            textView.text = item.html
        }
    }
}