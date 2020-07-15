package com.aaronicsubstances.niv1984.view_adapters

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
import com.aaronicsubstances.niv1984.parsing.BookParser
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookLoadAdapter(private val bibleVersions: List<String>):
        FiniteListAdapter<BookDisplayItem, RecyclerView.ViewHolder>(null) {

    var multipleDisplay: Boolean = false
    var displaySidebySide: Boolean = false

    override fun getItemViewType(position: Int): Int {
        var viewType = getItem(position).viewType.ordinal
        // split viewType to positive or negative depending on displaySideBySide.
        // since zero cannot have different signs, increment by 1.
        viewType += 1
        if (displaySidebySide) {
            viewType *= -1
        }
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (Math.abs(viewType) - 1) {
            BookDisplayItemViewType.TITLE.ordinal -> {
                if (displaySidebySide) {
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
                if (displaySidebySide) {
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
                return DividerViewHolder(itemView, bibleVersions)
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
                if (displaySidebySide) {
                    (holder as SplitTitleViewHolder).bind(item)
                }
                else {
                    (holder as TitleViewHolder).bind(item)
                }
            }
            BookDisplayItemViewType.VERSE -> {
                if (displaySidebySide) {
                    (holder as SplitVerseViewHolder).bind(item)
                }
                else {
                    (holder as VerseViewHolder).bind(item)
                }
            }
            BookDisplayItemViewType.DIVIDER -> {
                (holder as DividerViewHolder).bind(item, multipleDisplay)
            }
            else -> {
                (holder as DefaultViewHolder).bind(item)
            }
        }
    }

    class DefaultViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        init {
            //textView.textSize = AppUtils.spToPx(14f)
            textView.setTextColor(Color.parseColor("#000000"))
            //textView.setTextColor(Color.parseColor("white"))
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }

        fun bind(item: BookDisplayItem) {
            if (item.viewType == BookDisplayItemViewType.HEADER) {
                textView.setTypeface(textView.typeface, Typeface.BOLD)
            } else {
                textView.typeface = null
            }
            // cache html parse result
            if (item.fullContent.html == null) {
                item.fullContent.html = AppUtils.parseHtml(item.fullContent.text)
                item.fullContent.text = "" // reduce memory load
            }
            textView.text = item.fullContent.html
        }
    }

    class DividerViewHolder(itemView: View, private val bibleVersions: List<String>):
            RecyclerView.ViewHolder(itemView) {
        private val topRule = itemView.findViewById<View>(R.id.topRule)
        private val bottomRule = itemView.findViewById<View>(R.id.bottomRule)
        private val textView = topRule.findViewById<TextView>(R.id.text)

        init {
            VerseViewHolder.initSpecific(textView)
        }

        fun bind(item: BookDisplayItem, multipleDisplay: Boolean) {
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

    class TitleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        init {
            initSpecific(textView)
        }

        fun bind(item: BookDisplayItem) {
            bindSpecific(item.fullContent, textView)
        }

        companion object {
            fun initSpecific(textView: TextView) {
                //textView.textSize = AppUtils.spToPx(24f)
                textView.setTextColor(Color.parseColor("#000000"))
                //textView.setTextColor(Color.parseColor("white"))
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            }

            fun bindSpecific(item: BookDisplayItemContent, textView: TextView) {
                textView.text = item.text
            }
        }
    }

    class VerseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = this.itemView.findViewById<TextView>(R.id.text)

        init {
            initSpecific(textView)
        }

        fun bind(item: BookDisplayItem) {
            bindSpecific(item.fullContent, textView)
        }

        companion object {
            fun initSpecific(textView: TextView) {
                //textView.textSize = AppUtils.spToPx(14f)
                textView.setTextColor(Color.parseColor("#000000"))
                //textView.setTextColor(Color.parseColor("white"))
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }

            fun bindSpecific(item: BookDisplayItemContent, textView: TextView) {
                // cache html parse result
                if (item.html == null) {
                    item.html = AppUtils.parseHtml(item.text)
                    item.text = "" // reduce memory load
                }
                textView.text = item.html
                textView.textAlignment = TextView.TEXT_ALIGNMENT_INHERIT
                textView.setPadding(0)
                if (item.blockQuoteKind != null) {
                    when (item.blockQuoteKind) {
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
        }
    }

    class SplitTitleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)
        private val textView2 = itemView.findViewById<TextView>(R.id.text2)

        init {
            TitleViewHolder.initSpecific(textView)
            TitleViewHolder.initSpecific(textView2)
        }

        fun bind(item: BookDisplayItem) {
            TitleViewHolder.bindSpecific(item.firstPartialContent!![0], textView)
            TitleViewHolder.bindSpecific(item.secondPartialContent!![0], textView2)
        }
    }

    class SplitVerseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val firstSideVerse = itemView.findViewById<ViewGroup>(R.id.firstSideVerse)
        private val secondSideVerse = itemView.findViewById<ViewGroup>(R.id.secondSideVerse)

        init {
            (0 until firstSideVerse.childCount).forEach {
                VerseViewHolder.initSpecific(firstSideVerse.getChildAt(it) as TextView)
            }
            (0 until secondSideVerse.childCount).forEach {
                VerseViewHolder.initSpecific(secondSideVerse.getChildAt(it) as TextView)
            }
        }

        fun bind(item: BookDisplayItem) {
            bindSpecific(item.firstPartialContent!!, firstSideVerse)
            bindSpecific(item.secondPartialContent!!, secondSideVerse)
        }

        companion object {

            private fun bindSpecific(items: List<BookDisplayItemContent>, textViewGroup: ViewGroup) {
                // ensure enough text views.
                while (textViewGroup.childCount < items.size) {
                    val tv = TextView(textViewGroup.context)

                    // Create a LayoutParams for TextView
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    tv.layoutParams = lp

                    VerseViewHolder.initSpecific(tv)
                    textViewGroup.addView(tv)
                }

                // reset all views
                (0 until textViewGroup.childCount).forEach {
                    textViewGroup.getChildAt(it).visibility = View.GONE
                }

                items.forEachIndexed { i, item ->
                    val textView = textViewGroup.getChildAt(i) as TextView
                    textView.visibility = View.VISIBLE
                    VerseViewHolder.bindSpecific(item, textView)
                }
            }
        }
    }
}