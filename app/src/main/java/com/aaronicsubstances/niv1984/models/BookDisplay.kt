package com.aaronicsubstances.niv1984.models

import com.aaronicsubstances.niv1984.utils.BookParser

data class BookDisplay(
    val bookNumber: Int,
    val bibleVersions: List<String>,
    val displayItems: List<BookDisplayItem>,
    val chapterIndices: List<Int>,
    val displayMultipleSideBySide: Boolean,
    val isNightMode: Boolean
)


data class BookDisplayItem(val viewType: BookDisplayItemViewType,
                           val chapterNumber: Int,
                           val verseNumber: Int,
                           val fullContent: BookDisplayItemContent,
                           var firstPartialContent: List<BookDisplayItemContent>? = null,
                           var secondPartialContent: List<BookDisplayItemContent>? = null)

data class BookDisplayItemContent(val bibleVersionIndex: Int,
                                  var text: String,
                                  val blockQuoteKind: BookParser.BlockQuoteKind? = null,
                                  var html: CharSequence? = null,
                                  val isFirstDivider: Boolean = false)

enum class BookDisplayItemViewType {
    TITLE, VERSE, HEADER, FOOTNOTE, DIVIDER, CHAPTER_FRAGMENT, CROSS_REFERENCES
}