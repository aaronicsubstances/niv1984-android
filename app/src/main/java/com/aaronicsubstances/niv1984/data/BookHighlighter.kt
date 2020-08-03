package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.UserHighlightData
import com.aaronicsubstances.niv1984.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookHighlighter(private val context: Context,
                      private val bookNumber: Int,
                      private val bibleVersion: String) {

    // use shades of yellow in both day and night modes
    private val highlightColor = AppUtils.colorResToString(R.color.highlightColor, context)
    private lateinit var highlightData: List<UserHighlightData>
    private var latestChapterData =  UserHighlightData.ChapterData(0, arrayOf())

    suspend fun load() {
        highlightData = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.userHighlightDataDao().getHighlightData(bibleVersion, bookNumber)
        }
        loadChapterData(1)
    }

    private fun loadChapterData(chapterNumber: Int) {
        if (latestChapterData.chapterNumber == chapterNumber) {
            return
        }
        var chapterHighlightDataItem = highlightData.firstOrNull { it.chapterNumber == chapterNumber }
        if (chapterHighlightDataItem != null) {
            latestChapterData = AppUtils.deserializeFromJson(chapterHighlightDataItem.data,
                UserHighlightData.ChapterData::class.java)
            latestChapterData.chapterNumber = chapterNumber
        }
        else {
            latestChapterData = UserHighlightData.ChapterData(chapterNumber, arrayOf())
        }
    }

    fun processBlockText(chapterNumber: Int, verseNumber: Int, verseBlockIndex: Int,
                         source: VerseHighlighter): String {
        source.beginProcessing()
        loadChapterData(chapterNumber)
        val blockHighlights = latestChapterData.verseBlocks.firstOrNull {
            it.verseNumber == verseNumber && it.blockIndex == verseBlockIndex
        }
        if (blockHighlights != null) {
            for (highlightRange in blockHighlights.ranges) {
                source.updateMarkup(
                    highlightRange.startIndex,
                    "<span style='background-color: $highlightColor'>", false
                )
                source.updateMarkup(
                    highlightRange.endIndex,
                    "</span>", true
                )
            }
        }
        source.finalizeProcessing()
        return source.rawText.toString()
    }
}