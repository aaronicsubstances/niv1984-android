package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookmarkAdapter
        : FiniteListAdapter<BookmarkAdapterItem, BookmarkAdapter.ViewHolder>(null) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.bookmark_list_item,
            parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val descriptionTf = itemView.findViewById<TextView>(R.id.description)
        private val titleTf = itemView.findViewById<TextView>(R.id.title)
        private val dateTf = itemView.findViewById<TextView>(R.id.date)
        private val chapterTf = itemView.findViewById<TextView>(R.id.chapter)
        private val goToBtn = itemView.findViewById<Button>(R.id.goToBtn)

        fun bind(position: Int) {
            val item = getItem(position)
            titleTf.visibility = View.VISIBLE
            goToBtn.visibility = View.VISIBLE

            var titlePrefix = item.title
            var titleRemainder = ""
            val maxTitleLen = 20
            if (titlePrefix.length > maxTitleLen) {
                titleRemainder = titlePrefix.substring(maxTitleLen)
                titlePrefix = titlePrefix.substring(0, maxTitleLen)
            }
            titleTf.text = titlePrefix
            descriptionTf.text = titleRemainder

            val bv1 = AppConstants.bibleVersions.getValue(item.scrollPosPref.particularBibleVersions[0])
            val book = bv1.bookNames[item.scrollPosPref.bookNumber - 1]
            val bv = item.scrollPosPref.particularBibleVersions.map {
                AppConstants.bibleVersions.getValue(it).abbreviation
            }.joinToString("/")
            chapterTf.text = "$book ${item.scrollPosPref.chapterNumber} $bv"

            val d = AppUtils.formatTimeStamp(item.dateUpdated, "dd MMM yyyy")
            dateTf.text = "Last accessed on $d"
        }
    }
}