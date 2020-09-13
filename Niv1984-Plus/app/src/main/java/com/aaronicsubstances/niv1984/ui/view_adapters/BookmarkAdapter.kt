package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookmarkAdapter
        : FiniteListAdapter<BookmarkAdapterItem, BookmarkAdapter.ViewHolder>(null) {
    var adapterEventListener: AdapterEventListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.bookmark_list_item,
            parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    interface AdapterEventListener {
        fun bookmarkOpenRequested(item: BookmarkAdapterItem)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val descriptionTf = itemView.findViewById<TextView>(R.id.description)
        private val chapterAndDateTf = itemView.findViewById<TextView>(R.id.chapterAndDate)

        fun bind(position: Int) {
            val item = getItem(position)

            itemView.setOnClickListener {
                adapterEventListener?.bookmarkOpenRequested(item)
            }

            descriptionTf.text = item.title

            val d = AppUtils.formatTimeStamp(item.dateUpdated, "dd MMM yyyy")

            val bv1 = AppConstants.bibleVersions.getValue(item.scrollPosPref.particularBibleVersions[0])
            val book = bv1.bookNames[item.scrollPosPref.bookNumber - 1]
            val bv = item.scrollPosPref.particularBibleVersions.map {
                AppConstants.bibleVersions.getValue(it).abbreviation
            }.joinToString("/")
            chapterAndDateTf.text = "$book ${item.scrollPosPref.chapterNumber} $bv (accessed on $d)"
        }
    }
}