package com.aaronicsubstances.niv1984.view_adapters

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.FiniteListAdapter
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.parsing.BookParser
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookLoadAdapter: FiniteListAdapter<BookDisplayItem, RecyclerView.ViewHolder>(null) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            BookDisplayItemViewType.DIVIDER.ordinal -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_divider, parent, false)
                return DividerViewHolder(itemView)
            }
            BookDisplayItemViewType.TITLE.ordinal -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.book_load_item_title, parent, false)
                return TitleViewHolder(itemView)
            }
            BookDisplayItemViewType.VERSE.ordinal -> {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.book_load_item_verse, parent, false)
                return VerseViewHolder(itemView)
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
            BookDisplayItemViewType.DIVIDER -> {
                // do nothing
            }
            BookDisplayItemViewType.TITLE -> {
                (holder as TitleViewHolder).bind(item)
            }
            BookDisplayItemViewType.VERSE -> {
                (holder as VerseViewHolder).bind(item)
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
            if (item.html == null) {
                item.html = AppUtils.parseHtml(item.text)
                item.text = "" // reduce memory load
            }
            textView.text = item.html
        }
    }

    class DividerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    class TitleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text)

        init {
            initSpecific(textView)
        }

        fun bind(item: BookDisplayItem) {
            bindSpecific(item, textView)
        }

        companion object {
            private fun initSpecific(textView: TextView) {
                //textView.textSize = AppUtils.spToPx(24f)
                textView.setTextColor(Color.parseColor("#000000"))
                //textView.setTextColor(Color.parseColor("white"))
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            }

            private fun bindSpecific(item: BookDisplayItem, textView: TextView) {
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
            bindSpecific(item, textView)
        }

        companion object {
            private fun initSpecific(textView: TextView) {
                //textView.textSize = AppUtils.spToPx(14f)
                textView.setTextColor(Color.parseColor("#000000"))
                //textView.setTextColor(Color.parseColor("white"))
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }

            private fun bindSpecific(item: BookDisplayItem, textView: TextView) {
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
}