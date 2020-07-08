package com.aaronicsubstances.niv1984.scrollexperiment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.endlesspaginglib.EndlessListRepositoryForPositionalDS
import com.aaronicsubstances.endlesspaginglib.ListAdapterForPositionalDS
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookReadItem

class ExPosAdapter(repo: EndlessListRepositoryForPositionalDS<BookReadItem>):
    ListAdapterForPositionalDS<BookReadItem, ExPosAdapterViewHolder>(repo) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ExPosAdapterViewHolder.create(parent)

    override fun onBindViewHolder(holder: ExPosAdapterViewHolder, position: Int) {
        val model = getItem(position)
        holder.bind(model)
    }
}

class ExPosAdapterViewHolder(item: View): RecyclerView.ViewHolder(item) {
    private val textView: TextView = item.findViewById(R.id.text)

    fun bind(model: BookReadItem?) {
        if (model == null) {
            textView.text = "Loading..."
        }
        else {
            textView.text = model.text
        }
    }

    companion object {
        fun create(parent: ViewGroup): ExPosAdapterViewHolder {
            val holderView = LayoutInflater.from(parent.context).inflate(
                R.layout.book_read_item, parent, false)
            return ExPosAdapterViewHolder(holderView)
        }
    }
}