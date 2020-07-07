package com.aaronicsubstances.niv1984.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookReadItem

class BookReadAdapter : ListAdapter<BookReadItem, BookReadViewHolder>(ITEM_COMPARATOR) {
    override fun onBindViewHolder(holder: BookReadViewHolder, position: Int) {
        val item = getItem(position);
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookReadViewHolder {
        return BookReadViewHolder.create(parent)
    }

    companion object {
        private val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<BookReadItem>() {
            override fun areItemsTheSame(oldItem: BookReadItem, newItem: BookReadItem): Boolean =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: BookReadItem, newItem: BookReadItem): Boolean =
                true
        }
    }
}

class BookReadViewHolder(item: View): RecyclerView.ViewHolder(item) {
    private val textView: TextView = itemView.findViewById(R.id.text)

    fun bind(readItem: BookReadItem?) {
        if (readItem != null) {
            textView.text = readItem.text
        }
        else {
            textView.text = null
        }
    }

    companion object {
        fun create(parent: ViewGroup): BookReadViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.book_read_item,
                parent, false)
            return BookReadViewHolder(itemView)
        }
    }
}