package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookHighlighter(private val context: Context,
                      private val bookNumber: Int,
                      private val bibleVersion: String) {

    // use shades of yellow in both day and night modes
    private val highlightColor = AppUtils.colorResToString(R.color.highlightColor, context)
    private lateinit var highlightRanges: List<HighlightRange>

    suspend fun load() {
        val db = AppDatabase.getDatabase(context)
        highlightRanges = db.highlightRangeDao().getHighlightRanges(bibleVersion, bookNumber)
        if (highlightRanges.isEmpty()) {
            highlightRanges = listOf(
                HighlightRange(0, "", 0,
                1, 1, 0, 10, 20)
            )
        }
    }

    fun processBlockText(chapterNumber: Int, verseNumber: Int, verseBlockIndex: Int,
                         source: VerseHighlighter): String {
        source.beginProcessing()
        val blockHighlights = highlightRanges.filter {
            it.chapterNumber == chapterNumber && it.verseNumber == verseNumber &&
                    it.verseBlockIndex == verseBlockIndex
        }
        for (highlightRange in blockHighlights) {
            source.updateMarkup(highlightRange.startIndex,
                "<span style='background-color: $highlightColor'>", false)
            source.updateMarkup(highlightRange.endIndex,
                "</span>", true)
        }
        source.finalizeProcessing()
        return source.rawText.toString()
    }
}