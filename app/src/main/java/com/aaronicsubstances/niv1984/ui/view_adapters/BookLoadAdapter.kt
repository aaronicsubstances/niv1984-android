package com.aaronicsubstances.niv1984.ui.view_adapters

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.BookParser
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookLoadAdapter: FiniteListAdapter<BookDisplayItem, RecyclerView.ViewHolder>(null) {

    var bibleVersions = listOf<String>()
    var multipleDisplay: Boolean = false
    var displayMultipleSidebySide: Boolean = false
    var zoomLevel: Int = 100
    var isNightMode: Boolean = false

    override fun getItemViewType(position: Int): Int {
        var viewType = getItem(position).viewType.ordinal
        // split viewType to positive or negative depending on displaySideBySide.
        // since zero cannot have different signs, increment by 1.
        viewType += 1
        if (multipleDisplay && displayMultipleSidebySide) {
            viewType *= -1
        }
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (Math.abs(viewType) - 1) {
            BookDisplayItemViewType.TITLE.ordinal -> {
                if (viewType < 0) {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.book_load_item_title_split, parent, false)
                    return SplitTitleViewHolder(itemView)
                }
                else {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.book_load_item_title, parent, false)
                    return TitleViewHolder(itemView)
                }
            }
            BookDisplayItemViewType.VERSE.ordinal -> {
                if (viewType < 0) {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.book_load_item_verse_split, parent, false
                    )
                    return SplitVerseViewHolder(itemView)
                }
                else {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.book_load_item_verse, parent, false
                    )
                    return VerseViewHolder(itemView)
                }
            }
            BookDisplayItemViewType.DIVIDER.ordinal -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_divider, parent, false)
                return DividerViewHolder(itemView)
            }
            else -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_default, parent, false)
                return DefaultViewHolder(itemView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.viewType) {
            BookDisplayItemViewType.TITLE -> {
                if (holder is SplitTitleViewHolder) {
                    holder.bind(item)
                }
                else {
                    (holder as TitleViewHolder).bind(item)
                }
            }
            BookDisplayItemViewType.VERSE -> {
                if (holder is SplitVerseViewHolder) {
                    holder.bind(item)
                }
                else {
                    (holder as VerseViewHolder).bind(item)
                }
            }
            BookDisplayItemViewType.DIVIDER -> {
                (holder as DividerViewHolder).bind(item)
            }
            else -> {
                (holder as DefaultViewHolder).bind(item)
            }
        }
    }

    fun initDefault(item: BookDisplayItem, textView: TextView) {
        val textColor = if (isNightMode) "white" else "black"
        textView.setTextColor(Color.parseColor(textColor))

        val textSize = (if (item.viewType == BookDisplayItemViewType.TITLE) 21f else 18f) * zoomLevel / 100
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

        textView.typeface = null
        if (item.viewType == BookDisplayItemViewType.HEADER) {
            textView.setTypeface(textView.typeface, Typeface.BOLD)
        }
    }

    fun bindDefault(item: BookDisplayItem, itemContent: BookDisplayItemContent, textView: TextView) {
        initDefault(item, textView)

        // cache html parse result
        if (itemContent.html == null) {
            itemContent.html = AppUtils.parseHtml(itemContent.text)
            itemContent.text = "" // reduce memory load
        }
        textView.text = itemContent.html

        textView.textAlignment = TextView.TEXT_ALIGNMENT_INHERIT
        textView.setPadding(0)
        if (itemContent.blockQuoteKind != null) {
            when (itemContent.blockQuoteKind) {
                BookParser.BlockQuoteKind.CENTER -> {
                    textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                }
                BookParser.BlockQuoteKind.RIGHT -> {
                    textView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                }
                BookParser.BlockQuoteKind.LEFT -> {
                    textView.setPadding(
                        AppUtils.dimenResToPx(
                            R.dimen.block_quote_left, textView.context
                        ), 0, 0, 0
                    )
                }
                BookParser.BlockQuoteKind.LEFT_INDENTED -> {
                    textView.setPadding(
                        AppUtils.dimenResToPx(
                            R.dimen.block_quote_left_indent, textView.context
                        ), 0, 0, 0
                    )
                }
            }
        }
    }

    inner class DefaultViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        fun bind(item: BookDisplayItem) {
            bindDefault(item, item.fullContent, textView)
        }
    }

    inner class DividerViewHolder(itemView: View):
            RecyclerView.ViewHolder(itemView) {
        private val topRule = itemView.findViewById<View>(R.id.topRule)
        private val bottomRule = itemView.findViewById<View>(R.id.bottomRule)
        private val textView = topRule.findViewById<TextView>(R.id.text)

        fun bind(item: BookDisplayItem) {
            initDefault(item, textView)
            if (!item.fullContent.isFirstDivider) {
                bottomRule.visibility = View.VISIBLE
                topRule.visibility = View.GONE
            }
            else {
                if (multipleDisplay) {
                    bottomRule.visibility = View.GONE
                    topRule.visibility = View.VISIBLE
                    val bibleVersionCode = bibleVersions[item.fullContent.bibleVersionIndex]
                    val abbr = AppConstants.bibleVersions.getValue(bibleVersionCode).abbreviation
                    textView.text = abbr
                }
                else {
                    bottomRule.visibility = View.VISIBLE
                    topRule.visibility = View.GONE
                }
            }
        }
    }

    inner class TitleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        fun bind(item: BookDisplayItem) {
            bindDefault(item, item.fullContent, textView)
        }
    }

    inner class VerseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = this.itemView.findViewById<TextView>(R.id.text)

        fun bind(item: BookDisplayItem) {
            bindDefault(item, item.fullContent, textView)
        }
    }

    inner class SplitTitleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)
        private val textView2 = itemView.findViewById<TextView>(R.id.text2)

        fun bind(item: BookDisplayItem) {
            bindDefault(item, item.firstPartialContent!![0], textView)
            bindDefault(item, item.secondPartialContent!![0], textView2)
        }
    }

    inner class SplitVerseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val firstSideVerse = itemView.findViewById<ViewGroup>(R.id.firstSideVerse)
        private val secondSideVerse = itemView.findViewById<ViewGroup>(R.id.secondSideVerse)

        fun bind(item: BookDisplayItem) {
            bindSpecific(item, item.firstPartialContent!!, firstSideVerse)
            bindSpecific(item, item.secondPartialContent!!, secondSideVerse)
        }

        private fun bindSpecific(item: BookDisplayItem, items: List<BookDisplayItemContent>,
                                 textViewGroup: ViewGroup) {
            // ensure enough text views.
            while (textViewGroup.childCount < items.size) {
                val tv = TextView(textViewGroup.context)

                // Create a LayoutParams for TextView
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                tv.layoutParams = lp

                textViewGroup.addView(tv)
            }

            // reset all views
            (0 until textViewGroup.childCount).forEach {
                textViewGroup.getChildAt(it).visibility = View.GONE
            }

            items.forEachIndexed { i, itemContent ->
                val textView = textViewGroup.getChildAt(i) as TextView
                textView.visibility = View.VISIBLE
                bindDefault(item, itemContent, textView)
            }
        }
    }
}