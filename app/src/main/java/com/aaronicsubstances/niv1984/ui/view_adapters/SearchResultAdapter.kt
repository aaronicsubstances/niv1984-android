package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewClickListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.SearchResult
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class SearchResultAdapter(
    private val onItemClickListenerFactory: LargeListViewClickListener.Factory<SearchResult>
) : ListAdapter<SearchResult, SearchResultAdapter.ViewHolder>(ITEM_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.search_result_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    companion object {
        private val ITEM_COMPARATOR = object: DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return oldItem.docId == newItem.docId
            }

            override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return true
            }
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.textView)
        private val descriptionView = itemView.findViewById<TextView>(R.id.description)

        init {
            itemView.setOnClickListener(onItemClickListenerFactory.create(this))
        }

        fun bind(position: Int) {
            val searchResult = getItem(position)

            val bibleVersion = AppConstants.bibleVersions.getValue(searchResult.bibleVersion)
            var description = bibleVersion.bookNames[searchResult.bookNumber - 1]
            if (searchResult.verseNumber < 1) {
                description += " ${searchResult.chapterNumber} footnote"

            } else {
                description += " ${searchResult.chapterNumber}:${searchResult.verseNumber} "
            }
            description += " (${bibleVersion.abbreviation})"
            descriptionView.text = description

            val styledContent = AppUtils.parseHtml(searchResult.text)
            textView.text = styledContent
        }
    }
}