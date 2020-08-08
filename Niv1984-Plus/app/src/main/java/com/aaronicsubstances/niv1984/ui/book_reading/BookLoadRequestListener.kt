package com.aaronicsubstances.niv1984.ui.book_reading

interface BookLoadRequestListener {
    fun onBookLoadRequest(bookNumber: Int, chapterNumber: Int, verseNumber: Int)
}