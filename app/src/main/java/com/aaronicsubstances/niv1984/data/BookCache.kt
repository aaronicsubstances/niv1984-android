package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.BookParser
import com.google.gson.Gson
import java.io.*

class BookCache(private val context: Context,
                private val bookNumber: Int) {

    private val CACHE_TAG = "0"

    private fun generateCacheEntryName(bibleVersion: String, isNightMode: Boolean): String {
        return "$bibleVersion-$CACHE_TAG-$bookNumber-${isNightMode}.ser"
    }

    fun load(bibleVersion: String, isNightMode: Boolean,
             chapterIndices: MutableList<Int>): List<BookDisplayItem> {
        val fullPath = File(context.cacheDir,
                generateCacheEntryName(bibleVersion, isNightMode))
        val items = InputStreamReader(FileInputStream(fullPath), AppUtils.DEFAULT_CHARSET).use {
            val cached = Gson().fromJson(it, CachedBookDisplay::class.java)
            cached.convert(chapterIndices)
        }
        return items
    }

    fun save(bibleVersion: String, isNightMode: Boolean,
             items: List<BookDisplayItem>, chapterIndices: List<Int>) {
        val fullPath = File(context.cacheDir,
                generateCacheEntryName(bibleVersion, isNightMode))
        OutputStreamWriter(FileOutputStream(fullPath), AppUtils.DEFAULT_CHARSET).use {
            val cached = CachedBookDisplay.toCache(items, chapterIndices)
            Gson().toJson(cached, it)
        }
    }

    fun purge(bibleVersion: String) {
        listOf(true, false).forEach {
            val fullPath = File(context.cacheDir,
                    generateCacheEntryName(bibleVersion, it))
            fullPath.delete() // ignore deletion failure
        }
    }
}


class CachedBookDisplay {
    var displayItems: Array<CachedBookDisplayItem>? = null
    var chapterIndices: Array<Int>? = null

    companion object {

        private fun fromCacheItemContent(cached: CachedBookDisplayItemContent): BookDisplayItemContent {
            return BookDisplayItemContent(cached.bibleVersionIndex,
                    cached.text, cached.blockQuoteKind, null,
                    cached.isFirstDivider,
                    cached.highlightModeRemovableMarkups?.map {
                        VerseHighlighter.Markup(it.tag, it.pos, it.placeholder,
                                it.id, it.addedDuringUpdate, it.removeDuringHighlighting)
                    }
            )
        }

        private fun toCacheItemContent(itemContent: BookDisplayItemContent): CachedBookDisplayItemContent {
            return CachedBookDisplayItemContent().apply {
                this.bibleVersionIndex = itemContent.bibleVersionIndex
                this.text = itemContent.text
                this.blockQuoteKind = itemContent.blockQuoteKind
                this.isFirstDivider = itemContent.isFirstDivider
                this.highlightModeRemovableMarkups = itemContent.highlightModeRemovableMarkups?.map {
                    CachedMarkup().apply {
                        this.tag = it.tag
                        this.pos = it.pos
                        this.placeholder = it.placeholder
                        this.id = it.id
                        this.addedDuringUpdate = it.addedDuringUpdate
                        this.removeDuringHighlighting = it.removeDuringHighlighting
                    }
                }?.toTypedArray()
            }
        }

        fun toCache(items: List<BookDisplayItem>, chapterIndices: List<Int>): CachedBookDisplay {
            val cachedItems = items.map {
                CachedBookDisplayItem().apply {
                    this.viewType = it.viewType
                    this.chapterNumber = it.chapterNumber
                    this.verseNumber = it.verseNumber
                    this.fullContent = toCacheItemContent(it.fullContent)
                    this.isFirstVerseContent = it.isFirstVerseContent
                }
            }
            return CachedBookDisplay().apply {
                this.chapterIndices = chapterIndices.toTypedArray()
                this.displayItems = cachedItems.toTypedArray()
            }
        }
    }

    fun convert(chapterIndices: MutableList<Int>): List<BookDisplayItem> {
        chapterIndices.addAll(this.chapterIndices!!)
        return displayItems!!.map {
            BookDisplayItem(it.viewType!!, it.chapterNumber, it.verseNumber,
                    fromCacheItemContent(it.fullContent!!),
                    null, null, it.isFirstVerseContent)
        }
    }

    class CachedBookDisplayItem {
        var viewType: BookDisplayItemViewType? = null
        var chapterNumber: Int = 0
        var verseNumber: Int = 0
        var fullContent: CachedBookDisplayItemContent? = null
        var isFirstVerseContent: Boolean = false
    }

    class CachedBookDisplayItemContent {
        var bibleVersionIndex: Int = 0
        var text: String = ""
        var blockQuoteKind: BookParser.BlockQuoteKind? = null
        var isFirstDivider: Boolean = false
        var highlightModeRemovableMarkups: Array<CachedMarkup>? = null
    }

    class CachedMarkup {
        var tag: String = ""
        var pos: Int = 0
        var placeholder: String? = null
        var id: String? = null
        var addedDuringUpdate: Boolean = false
        var removeDuringHighlighting: Boolean = false
    }
}