package com.aaronicsubstances.niv1984.ui.view_adapters

interface BookReadingEventListener {
    fun onUrlClick(bibleVersionIndex: Int, url: String)
    fun onVerseLongClick(bibleVersionIndex: Int, chapterNumber: Int, verseNumber: Int)
}