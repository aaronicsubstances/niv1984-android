package com.aaronicsubstances.niv1984.ui.book_reading

import com.aaronicsubstances.niv1984.models.UserBookmark

interface BookLoadRequestListener {
    fun onBookLoadRequest(bookNumber: Int, chapterNumber: Int, verseNumber: Int)
    fun onBookLoadRequest(entity: UserBookmark)
}