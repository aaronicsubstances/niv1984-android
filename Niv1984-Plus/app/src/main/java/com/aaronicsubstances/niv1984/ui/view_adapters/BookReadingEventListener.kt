package com.aaronicsubstances.niv1984.ui.view_adapters

interface BookReadingEventListener {
    fun onFootNoteClick(bibleVersionIndex: Int, description: String)
    fun onVerseLongClick(bibleVersionIndex: Int, chapterNumber: Int, verseNumber: Int)
}