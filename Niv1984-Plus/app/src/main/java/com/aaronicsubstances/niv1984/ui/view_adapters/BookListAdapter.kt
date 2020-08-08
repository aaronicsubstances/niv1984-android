package com.aaronicsubstances.niv1984.ui.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppConstants
import androidx.core.content.ContextCompat
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.github.ivbaranov.mli.MaterialLetterIcon
import org.slf4j.LoggerFactory

class BookListAdapter(bibleVersionCode: String,
                      private val onItemClickListenerFactory: LargeListEventListenerFactory)
        : FiniteListAdapter<Any, BookListAdapterViewHolder>(null) {

    var bibleVersionCode: String = bibleVersionCode
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookListAdapterViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.book_list_item,
            parent, false)
        return BookListAdapterViewHolder(
            itemView,
            onItemClickListenerFactory
        )
    }

    override fun getItemCount(): Int {
        return AppConstants.BIBLE_BOOK_COUNT
    }

    override fun onBindViewHolder(holder: BookListAdapterViewHolder, position: Int) {
        holder.bind(position, bibleVersionCode)
    }
}

class BookListAdapterViewHolder(item: View,
                                itemClickListenerFactory: LargeListEventListenerFactory)
        : RecyclerView.ViewHolder(item) {

    private val bookTitleView: TextView = item.findViewById(R.id.bookTitle)
    private val bookIconView: MaterialLetterIcon = item.findViewById(R.id.bookIcon)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BookListAdapter::class.java.simpleName)
    }

    init {
        itemView.setOnClickListener(itemClickListenerFactory.create(this,
            View.OnClickListener::class.java, null))
    }

    fun bind(position: Int, bibleVersionCode: String) {
        val bibleVersion = AppConstants.bibleVersions.getValue(bibleVersionCode)

        val description = bibleVersion.bookNames[position]
        bookTitleView.text = description

        bookIconView.letterColor = ContextCompat.getColor(itemView.context,
            R.color.letterTextColor)

        // use first letter if available.
        var colorIndex = 0
        var iconTextSource = description
        for (i in description.indices) {
            if (Character.isLetter(description[i])) {
                iconTextSource = description.substring(i)
                // there are only 26 available colours aside default of 0,
                // so map to one of them.
                // let mapping resemble previous one in which English letters were used.
                // NB: capital A has unicode value of 65
                colorIndex = ((description[i].toInt() - 65 + 26*3) % 26) + 1
                break
            }
        }
        bookIconView.letter = iconTextSource
        val colorRes = try {
            val f = R.color::class.java.getField(String.format("letter_%02d", colorIndex))
            f.get(null) as Int
        }
        catch (ex: Exception) {
            LOGGER.error("Could not find icon for {} book: ", description, ex)
            R.color.letter_00
        }
        bookIconView.shapeColor = ContextCompat.getColor(itemView.context, colorRes)
    }
}