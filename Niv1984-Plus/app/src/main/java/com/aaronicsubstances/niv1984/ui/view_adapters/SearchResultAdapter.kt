package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.SearchResultAdapterItem
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class SearchResultAdapter: ListAdapter<SearchResultAdapterItem, RecyclerView.ViewHolder>(ITEM_COMPARATOR) {

    var onItemClickListenerFactory: LargeListEventListenerFactory? = null

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == AppUtils.VIEW_TYPE_LOADING) {
            val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.loading_spinner_item, parent, false
            )
            return LoadingViewHolder(itemView)
        }
        else if (viewType == AppUtils.VIEW_TYPE_ERROR) {
            val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.search_result_item, parent, false
            )
            return ErrorViewHolder(itemView)
        }
        else {
            val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.search_result_item, parent, false
            )
            return ViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(position)
        }
    }

    companion object {
        private val ITEM_COMPARATOR = object: DiffUtil.ItemCallback<SearchResultAdapterItem>() {
            override fun areItemsTheSame(oldItem: SearchResultAdapterItem, newItem: SearchResultAdapterItem): Boolean {
                return oldItem.item.docId == newItem.item.docId
            }

            override fun areContentsTheSame(oldItem: SearchResultAdapterItem, newItem: SearchResultAdapterItem): Boolean {
                return true
            }
        }
    }

    class LoadingViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    inner class ErrorViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.textView)
        private val descriptionView = itemView.findViewById<TextView>(R.id.description)

        init {
            descriptionView.text = "?"
            textView.text = itemView.context.getString(R.string.too_many_search_items_error)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.textView)
        private val descriptionView = itemView.findViewById<TextView>(R.id.description)

        init {
            onItemClickListenerFactory?.let {
                itemView.setOnClickListener(it.create(this, View.OnClickListener::class.java, null))
            }
        }

        fun bind(position: Int) {
            val searchResult = getItem(position).item

            val bibleVersion = AppConstants.bibleVersions.getValue(searchResult.bibleVersion)
            var description = bibleVersion.bookNames[searchResult.bookNumber - 1]
            if (searchResult.verseNumber < 1) {
                description += " ${searchResult.chapterNumber} ${bibleVersion.strFootnote}"

            } else {
                description += " ${searchResult.chapterNumber}:${searchResult.verseNumber}"
            }
            description += " (${bibleVersion.abbreviation})"
            descriptionView.text = description

            val styledContent = AppUtils.parseHtml(searchResult.text)
            textView.text = styledContent
        }
    }
}