package com.aaronicsubstances.niv1984.models

data class BookDisplay(val bookNumber: Int,
                       val bibleVersions: List<String>,
                       val displayItems: List<BookDisplayItem>,
                       val chapterIndices: List<Int>)

data class BookDisplayItem(val bibleVersion: String,
                           val chapterNumber: Int,
                           var indexInChapter: Int,
                           val viewType: BookDisplayItemViewType,
                           val verseNumber: Int,
                           val text: CharSequence)

enum class BookDisplayItemViewType {
    TITLE, VERSE, HEADER, FOOTNOTE, DIVIDER, CHAPTER_FRAGMENT, CROSS_REFERENCES
}