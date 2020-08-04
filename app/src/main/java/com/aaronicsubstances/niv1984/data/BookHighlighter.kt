package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.HighlightRange
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

    suspend fun load() {
        highlightData = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.userHighlightDataDao().getHighlightData(bibleVersion, bookNumber)
        }
    }

    fun processBlockText(chapterNumber: Int, verseNumber: Int, verseBlockIndex: Int,
                         source: VerseHighlighter): String {
        source.beginProcessing()
        val blockHighlightData = highlightData.firstOrNull {
            it.chapterNumber == chapterNumber && it.verseNumber == verseNumber &&
                    it.verseBlockIndex == verseBlockIndex
        }
        val blockHighlights = if (blockHighlightData == null) {
            arrayOf()
        }
        else {
            AppUtils.deserializeFromJson(blockHighlightData.data, Array<HighlightRange>::class.java)
        }
        try {
            for (highlightRange in blockHighlights) {
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
        catch (ex: Exception) {
            val exLoc = "$chapterNumber.${verseNumber}[$verseBlockIndex]"
            AppUtils.showLongToast(context, "Unable to apply highlights to $exLoc: ${ex.message}")
            var i = source.markupList.size -1
            while (i >= 0) {
                if (source.markupList[i].addedDuringUpdate) {
                    source.markupList.removeAt(i)
                }
                i--
            }
        }
        source.finalizeProcessing()
        return source.rawText.toString()
    }

    fun getHighlightModeRemovableMarkups(source: VerseHighlighter): List<VerseHighlighter.Markup>? {
        val removableMarkups = source.markupList.filter { it.id != null }
        return if (removableMarkups.isEmpty()) null else removableMarkups
    }
}