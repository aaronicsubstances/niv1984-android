package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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
    var inSelectionMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var inSelectAllMode: Boolean = false
        set(value) {
            field = value
            if (value) {
                selectedIds.clear()
                notifyDataSetChanged()
            }
        }
    val selectedIds: MutableList<Int> = mutableListOf()
    val effectiveSelectionCount: Int
        get() {
            val selectionCount = if (inSelectAllMode) {
                currentList.size
            }
            else {
                selectedIds.size
            }
            return selectionCount
        }

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
        fun bookmarkDeleteRequested(item: BookmarkAdapterItem)
        fun onBookmarkSelectionChanged(item: BookmarkAdapterItem, itemNowSelected: Boolean)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val descriptionTf = itemView.findViewById<TextView>(R.id.description)
        private val chapterAndDateTf = itemView.findViewById<TextView>(R.id.chapterAndDate)
        private val selectedCheckBox = itemView.findViewById<CheckBox>(R.id.selectBox)

        fun bind(position: Int) {
            val item = getItem(position)

            selectedCheckBox.setOnClickListener {
                if (inSelectionMode) {
                    processSelection(item)
                }
            }
            itemView.setOnClickListener {
                if (inSelectionMode) {
                    processSelection(item)
                }
                else {
                    adapterEventListener?.bookmarkOpenRequested(item)
                }
            }
            itemView.setOnLongClickListener {
                if (inSelectionMode) {
                    false
                }
                else {
                    adapterEventListener?.bookmarkDeleteRequested(item)
                    true
                }
            }

            descriptionTf.text = item.title

            val d = AppUtils.formatTimeStamp(item.dateUpdated, "dd MMM yyyy")

            val bv1 = AppConstants.bibleVersions.getValue(item.scrollPosPref.particularBibleVersions[0])
            val book = bv1.bookNames[item.scrollPosPref.bookNumber - 1]
            val bv = item.scrollPosPref.particularBibleVersions.map {
                AppConstants.bibleVersions.getValue(it).abbreviation
            }.joinToString("/")
            chapterAndDateTf.text = "$book ${item.scrollPosPref.chapterNumber} $bv (accessed on $d)"

            if (inSelectionMode) {
                selectedCheckBox.visibility = View.VISIBLE
                selectedCheckBox.isChecked = inSelectAllMode || selectedIds.contains(item.id)
            }
            else {
                selectedCheckBox.visibility = View.INVISIBLE
            }
        }

        private fun processSelection(item: BookmarkAdapterItem) {
            var itemSelected = false
            if (inSelectAllMode || selectedIds.contains(item.id)) {
                if (inSelectAllMode) {
                    selectedIds.clear()
                    selectedIds.addAll(currentList.map { it.id })
                    inSelectAllMode = false
                }
                selectedIds.remove(item.id)
            }
            else {
                selectedIds.add(item.id)
                itemSelected = true
            }
            notifyItemChanged(adapterPosition)
            adapterEventListener?.onBookmarkSelectionChanged(item, itemSelected)
        }
    }
}