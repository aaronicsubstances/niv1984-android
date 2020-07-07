package com.aaronicsubstances.niv1984.scrollexperiment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R

class ExAdapter(var text: String, var size: Int): RecyclerView.Adapter<ExViewHolder>() {

    override fun getItemCount(): Int {
        return size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExViewHolder {
        return ExViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ExViewHolder, position: Int) {
        holder.bind(position, text)
    }
}

class ExViewHolder(item: View): RecyclerView.ViewHolder(item) {
    private val textView: TextView = item.findViewById(R.id.text)

    fun bind(position: Int, text: String) {
        textView.text = "${position+1}: $text"
    }

    companion object {
        fun create(parent: ViewGroup): ExViewHolder {
            val holderView = LayoutInflater.from(parent.context).inflate(
                R.layout.book_read_item, parent, false)
            return ExViewHolder(holderView)
        }
    }
}