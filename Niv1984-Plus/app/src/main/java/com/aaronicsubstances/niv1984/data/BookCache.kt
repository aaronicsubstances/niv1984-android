package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.models.BookCacheEntry
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.BookParser

class BookCache(private val context: Context,
                private val bookNumber: Int) {
    companion object {
        private const val CACHE_VERSION = "1"
    }

    private fun generateGroupId(bibleVersion: String, isNightMode: Boolean): String {
        return "$bibleVersion-$bookNumber-${isNightMode}"
    }

    suspend fun load(bibleVersion: String, isNightMode: Boolean,
             chapterIndices: MutableList<Pair<Int, Int>>): List<BookDisplayItem> {
        val db = SearchDatabase.getDatabase(context)
        val groupId = generateGroupId(bibleVersion, isNightMode)
        val entries = db.bookCacheEntryDao().getEntries(groupId, CACHE_VERSION)
        val items = mutableListOf<BookDisplayItem>()
        var runningChapterNumber = 0
        for (cached in entries) {
            if (cached.chapterNumber != runningChapterNumber) {
                runningChapterNumber = cached.chapterNumber
                chapterIndices.add(Pair(runningChapterNumber, items.size))
            }
            val removableMarkups = cached.serializedHighlightModeRemovableMarkups?.let {
                AppBinarySerializer.deserializeMarkups(it)
            }
            val viewType = enumValueOf<BookDisplayItemViewType>(cached.serializedViewType)
            val blockQuoteKind = cached.serializedBlockQuoteKind?.let {enumValueOf<BookParser.BlockQuoteKind>(it) }
            val fullContent = BookDisplayItemContent(0, cached.text,
                    blockQuoteKind, null, cached.isFirstDivider, cached.footNoteId, removableMarkups)
            val item = BookDisplayItem(viewType, cached.chapterNumber, cached.verseNumber,
                    fullContent, null, null, cached.isFirstVerseContent)
            items.add(item)
        }
        return items
    }

    suspend fun save(bibleVersion: String, isNightMode: Boolean,
             items: List<BookDisplayItem>) {
        val db = SearchDatabase.getDatabase(context)
        val groupId = generateGroupId(bibleVersion, isNightMode)
        val entries = mutableListOf<BookCacheEntry>()
        for (item in items) {
            var serializedRemovableMarkups = item.fullContent.highlightModeRemovableMarkups?.let {
                AppBinarySerializer.serializeMarkups(it)
            }
            val cached = BookCacheEntry(0, groupId, CACHE_VERSION, entries.size,
                    item.viewType.toString(),
                    item.chapterNumber, item.verseNumber, item.isFirstVerseContent,
                    item.fullContent.text, item.fullContent.blockQuoteKind?.toString(),
                    item.fullContent.isFirstDivider, item.fullContent.footNoteId, serializedRemovableMarkups)
            entries.add(cached)
        }
        db.bookCacheEntryDao().setEntries(groupId, entries)
    }

    suspend fun purge(bibleVersion: String, chapterNumber: Int) {
        val db = SearchDatabase.getDatabase(context)
        db.bookCacheEntryDao().purgeEntries(listOf(true, false).map { isNightMode ->
            generateGroupId(bibleVersion, isNightMode)
        }, chapterNumber)
    }
}